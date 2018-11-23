package com.mackenziehigh.socius.web;

import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import com.google.common.collect.Maps;
import com.google.common.net.MediaType;
import com.google.protobuf.ByteString;
import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import com.mackenziehigh.socius.web.http_m.HttpHeader;
import com.mackenziehigh.socius.web.http_m.HttpProtocol;
import com.mackenziehigh.socius.web.http_m.HttpQueryParameter;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders.Names;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.HttpResponseStatus;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A experimental non-blocking HTTP server based on the Netty framework.
 */
public final class WebServer
{
    private static final Logger logger = LogManager.getLogger(WebServer.class);

    /**
     * This counter is used to assign sequence-numbers to requests.
     */
    private final AtomicLong seqnum = new AtomicLong();

    /**
     * This is the human-readable name of this server to embed in requests.
     */
    private final String serverName;

    /**
     * This is the UUID of this server lifetime to embed in requests.
     */
    private final String serverId = UUID.randomUUID().toString();

    /**
     * This user-defined value will be embedded in requests.
     */
    private final String replyTo;

    /**
     * This is the host that the server will listen on.
     */
    private final String host;

    /**
     * This is the port that the server will listen on.
     */
    private final int port;

    /**
     * Connections will be closed, if a response is not received within this timeout.
     */
    private final Duration responseTimeout;

    /**
     * HTTP Chunk Aggregation Limit.
     */
    private final int aggregationCapacity;

    /**
     * This flag will be set to true, when start() is called.
     */
    private final AtomicBoolean started = new AtomicBoolean();

    /**
     * This flag will be set to true, when stop() is called.
     */
    private final AtomicBoolean stopped = new AtomicBoolean();

    /**
     * A reference to this object is needed in order to be able to stop the server.
     */
    private volatile ChannelFuture shutdownHook;

    /**
     * This stage is used to create private actors herein.
     *
     * <p>
     * Since a server is an I/O end-point, is expected to have high throughput,
     * and needs to have low latency, the server will have its own dedicated stage.
     * </p>
     */
    private final Stage stage = Cascade.newStage();

    /**
     * This map maps correlation UUIDs of requests to consumer functions
     * that will be used to process the corresponding responses.
     *
     * <p>
     * Entries are added to this map whenever an HTTP request is processed.
     * Entries are lazily removed from this map, if they have been there too long.
     * </p>
     */
    private final Map<String, Connection> connections = Maps.newConcurrentMap();

    /**
     * This queue is used to remove connections from the map of connections.
     */
    private final Queue<Connection> responseTimeoutQueue = new PriorityQueue<>();

    /**
     * This processor will be used to send HTTP requests out of the server,
     * so that external handler actors can process the requests.
     */
    private final Actor<http_m.HttpRequest, http_m.HttpRequest> requestsOut;

    /**
     * This processor will receive the HTTP responses from the external actors
     * and then will route those responses to the originating connection.
     */
    private final Actor<http_m.HttpResponse, http_m.HttpResponse> responsesIn;

    /**
     * Sole Constructor.
     *
     * @param builder contains the initial server settings.
     */
    private WebServer (final Builder builder)
    {
        this.host = builder.host;
        this.port = builder.port;
        this.aggregationCapacity = builder.aggregationCapacity;
        this.serverName = builder.serverName;
        this.replyTo = builder.replyTo;
        this.responseTimeout = builder.responseTimeout;
        this.requestsOut = stage.newActor().withScript(this::onRequest).create();
        this.responsesIn = stage.newActor().withScript(this::onResponse).create();
    }

    /**
     * Get the current state of the sequence-number generator.
     *
     * @return the current sequence-number.
     */
    public long getSeqnum ()
    {
        return seqnum.get();
    }

    /**
     * Get the human-readable name of this server.
     *
     * @return the server name.
     */
    public String getServerName ()
    {
        return serverName;
    }

    /**
     * Get the universally-unique-identifier of this server instance.
     *
     * @return the server identifier.
     */
    public String getServerId ()
    {
        return serverId;
    }

    /**
     * Get the (Reply-To) property embedded in each outgoing request.
     *
     * @return the reply-to address.
     */
    public String getReplyTo ()
    {
        return replyTo;
    }

    /**
     * Get the name of the host that the server is listening on.
     *
     * @return the server host.
     */
    public String getHost ()
    {
        return host;
    }

    /**
     * Get the port that the server is listening on.
     *
     * @return the server port.
     */
    public int getPort ()
    {
        return port;
    }

    /**
     * Get the maximum amount of time the server will wait for a response,
     * before the connection is closed without sending a response.
     *
     * @return the connection timeout.
     */
    public Duration getResponseTimeout ()
    {
        return responseTimeout;
    }

    /**
     * Get the size of the buffer used to join chunked messages into one.
     *
     * @return the approximate maximum size of a request.
     */
    public int getAggregationCapacity ()
    {
        return aggregationCapacity;
    }

    /**
     * Get the number of open connections at this time.
     *
     * @return the number of pending requests.
     */
    public int getConnectionCount ()
    {
        return connections.size();
    }

    /**
     * Use this connection to receive HTTP Requests from this HTTP server.
     *
     * @return the connection.
     */
    public Output<http_m.HttpRequest> requestsOut ()
    {
        return requestsOut.output();
    }

    /**
     * Use this connection to send HTTP Responses to this HTTP server.
     *
     * <p>
     * If the HTTP Response correlates to an existing live HTTP Request,
     * then the response will be forwarded to the associated client.
     * </p>
     *
     * <p>
     * If the HTTP Response does not correlate to an existing live HTTP Request,
     * then the response will be silently dropped.
     * </p>
     *
     * @return the connection.
     */
    public Input<http_m.HttpResponse> responsesIn ()
    {
        return responsesIn.input();
    }

    /**
     * Use this method to start the server.
     *
     * <p>
     * This method has no effect, if the server was already started.
     * </p>
     *
     * @return this.
     */
    public WebServer start ()
    {
        if (started.compareAndSet(false, true))
        {
            logger.info("Starting Server");
            final Thread thread = new Thread(this::run);
            thread.start();
        }
        return this;
    }

    /**
     * Use this method to shutdown the server and release its threads.
     *
     * <p>
     * This method has no effect, if the server already begun to stop.
     * </p>
     *
     * @return this.
     * @throws java.lang.InterruptedException
     */
    public WebServer stop ()
            throws InterruptedException
    {
        if (stopped.compareAndSet(false, true))
        {
            logger.info("Stopping Server");

            if (shutdownHook != null)
            {
                shutdownHook.channel().close().sync();
            }
        }
        return this;
    }

    /**
     * Server Main.
     */
    private void run ()
    {
        final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        final EventLoopGroup workerGroup = new NioEventLoopGroup();
        try
        {
            Runtime.getRuntime().addShutdownHook(new Thread(this::onShutdown));

            final Thread thread = new Thread(this::prunePendingResponses);
            thread.setDaemon(true);
            thread.start();

            final ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new Initializer());

            shutdownHook = b.bind(host, port).sync();
            shutdownHook.channel().closeFuture().sync();
        }
        catch (InterruptedException ex)
        {
            logger.catching(ex);
        }
        finally
        {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    private void onShutdown ()
    {
        try
        {
            stop();
        }
        catch (InterruptedException ex)
        {
            logger.error(ex);
        }
    }

    private http_m.HttpRequest onRequest (final http_m.HttpRequest request)
    {
        return request;
    }

    private void onResponse (final http_m.HttpResponse response)
    {
        routeResponse(response);
    }

    /**
     * This actor receives the HTTP Responses from the external handlers connected to this server,
     * routes them to the appropriate client-server connection, and then transmits them via Netty.
     *
     * @param response needs to be send to a client.
     */
    private void routeResponse (final http_m.HttpResponse response)
    {
        /**
         * Get the Correlation-ID that allows us to map responses to requests.
         * Whoever created the response should have included the Correlation-ID.
         * If they did not, they may have implicitly included it, by including the request itself.
         */
        final String correlationId;

        if (response.hasCorrelationId())
        {
            correlationId = response.getCorrelationId();
        }
        else if (response.hasRequest() && response.getRequest().hasCorrelationId())
        {
            correlationId = response.getRequest().getCorrelationId();
        }
        else
        {
            /**
             * No Correlation-ID is present.
             * Therefore, we will not be able to find the relevant client-server connection.
             * Drop the response silently.
             */
            return;
        }

        /**
         * This consumer will take the response and send it to the client.
         * This consumer is a one-shot operation.
         */
        final Connection connection = connections.get(correlationId);

        if (connection == null)
        {
            return;
        }

        /**
         * Send the HTTP Response to the client, if they are still connected.
         */
        connection.send(response);
    }

    private void prunePendingResponses ()
    {
        while (responseTimeoutQueue.isEmpty() == false)
        {
            final Connection conn = responseTimeoutQueue.peek();

            if (conn.timeout.isBefore(Instant.now().minus(responseTimeout)))
            {
                conn.close();
            }
            else
            {
                break;
            }
        }
    }

    /**
     * Logic to setup the Netty pipeline to handle a <b>single</b> connection.
     *
     * <p>
     * The logic herein is executed whenever a new connection is established!
     * </p>
     */
    private final class Initializer
            extends ChannelInitializer<SocketChannel>
    {
        @Override
        protected void initChannel (final SocketChannel channel)
                throws Exception
        {
            channel.pipeline().addLast(new HttpResponseEncoder());
            channel.pipeline().addLast(new HttpRequestDecoder()); // TODO: Args?
            channel.pipeline().addLast(new HttpObjectAggregator(aggregationCapacity, true));
            channel.pipeline().addLast(new HttpHandler());
        }

    }

    /**
     * An instance of this class will be used to translate
     * an incoming HTTP Request into a Protocol Buffer
     * and then send the Protocol Buffer to external handlers.
     *
     * <p>
     * A new instance of this class will be created per connection!
     * </p>
     */
    private final class HttpHandler
            extends SimpleChannelInboundHandler<Object>
    {
        @Override
        public void channelReadComplete (final ChannelHandlerContext ctx)
                throws Exception
        {
            ctx.flush();
        }

        @Override
        protected void channelRead0 (final ChannelHandlerContext ctx,
                                     final Object msg)
                throws Exception
        {
            if (msg instanceof FullHttpRequest)
            {
                final http_m.HttpRequest encodedRequest = encode((FullHttpRequest) msg);
                final String correlationId = encodedRequest.getCorrelationId();

                /**
                 * Create the response handler that will be used to route
                 * the corresponding HTTP Response, if and when it occurs.
                 */
                Verify.verify(connections.containsKey(correlationId) == false);
                final Connection connection = new Connection(correlationId, Instant.now(), ctx);
                responseTimeoutQueue.add(connection);
                connections.put(correlationId, connection);

                /**
                 * Send the HTTP Request to the external actors,
                 * so they can form an HTTP Response.
                 */
                requestsOut.input().send(encodedRequest);
            }
        }

        @Override
        public void exceptionCaught (final ChannelHandlerContext ctx,
                                     final Throwable cause)
                throws Exception
        {
            /**
             * Log the exception.
             */
            logger.warn(cause);

            /**
             * Notify the client of the error, but do not tell them why (for security).
             */
            final FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, INTERNAL_SERVER_ERROR);

            /**
             * Send the response to the client.
             */
            ctx.writeAndFlush(response);

            /**
             * Close the connection.
             */
            ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }

        private http_m.HttpRequest encode (final FullHttpRequest request)
                throws URISyntaxException
        {
            final String correlationId = UUID.randomUUID().toString();

            final http_m.HttpRequest.Builder builder = http_m.HttpRequest.newBuilder();

            builder.setServerName(serverName);
            builder.setServerId(serverId);
            builder.setSequenceNumber(seqnum.getAndIncrement());
            builder.setTimestamp(System.currentTimeMillis());
            builder.setCorrelationId(correlationId);
            builder.setReplyTo(replyTo);
            builder.setProtocol(HttpProtocol.newBuilder()
                    .setText(request.protocolVersion().text())
                    .setName(request.protocolVersion().protocolName())
                    .setMajorVersion(request.protocolVersion().majorVersion())
                    .setMinorVersion(request.protocolVersion().minorVersion()));
            builder.setMethod(request.method().name());

            /**
             * Encode the URL.
             */
            final QueryStringDecoder qsDecoder = new QueryStringDecoder(request.uri());
            builder.setUri(qsDecoder.uri());
            builder.setPath(qsDecoder.path());
            builder.setRawPath(qsDecoder.rawPath());
            builder.setRawQuery(qsDecoder.rawQuery());

            /**
             * Encode Query Parameters.
             */
            for (Entry<String, List<String>> params : qsDecoder.parameters().entrySet())
            {
                final HttpQueryParameter.Builder param = HttpQueryParameter.newBuilder();
                param.setKey(params.getKey());
                param.addAllValues(params.getValue());

                builder.putParameters(param.getKey(), param.build());
            }

            /**
             * Encode Host.
             */
            if (request.headers().contains(Names.HOST))
            {
                builder.setHost(request.headers().get(Names.HOST));
            }

            /**
             * Encode Content Type.
             */
            if (request.headers().contains(Names.CONTENT_TYPE))
            {
                builder.setContentType(request.headers().get(Names.CONTENT_TYPE));
            }

            /**
             * Encode the headers.
             */
            for (String name : request.headers().names())
            {
                builder.putHeaders(name,
                                   HttpHeader.newBuilder()
                                           .setKey(name)
                                           .addAllValues(request.headers().getAll(name))
                                           .build());
            }

            /**
             * Encode the cookies.
             */
            for (String header : request.headers().getAll(Names.COOKIE))
            {
                for (Cookie cookie : ServerCookieDecoder.STRICT.decode(header))
                {
                    final http_m.HttpCookie.Builder cookieBuilder = http_m.HttpCookie.newBuilder();

                    cookieBuilder.setDomain(cookie.domain());
                    cookieBuilder.setHttpOnly(cookie.isHttpOnly());
                    cookieBuilder.setSecure(cookie.isSecure());
                    cookieBuilder.setPath(cookie.path());
                    cookieBuilder.setMaxAge(cookie.maxAge());

                    builder.addCookies(cookieBuilder);
                }
            }

            /**
             * Encode the body.
             */
            if (request.content().isReadable())
            {
                // TODO: Optimize? Use shared temp buffer?
                final byte[] bytes = new byte[request.content().readableBytes()];
                request.content().readBytes(bytes);
                final ByteString byteString = ByteString.copyFrom(bytes);
                builder.setContentLength(byteString.size());
                builder.setBody(byteString);
            }
            else
            {
                builder.setContentLength(0);
                builder.setBody(ByteString.EMPTY);
            }

            return builder.build();
        }

    }

    /**
     * Representation of client-server connection.
     */
    private final class Connection
            implements Comparable<Connection>
    {
        public final Instant timeout;

        public final ChannelHandlerContext ctx;

        public final String correlationId;

        private final AtomicBoolean sent = new AtomicBoolean();

        private final AtomicBoolean closed = new AtomicBoolean();

        public Connection (final String correlationId,
                           final Instant timeout,
                           final ChannelHandlerContext context)
        {

            this.correlationId = correlationId;
            this.timeout = timeout;
            this.ctx = context;
        }

        @Override
        public int compareTo (final Connection other)
        {
            return timeout.compareTo(other.timeout);
        }

        public void send (final http_m.HttpResponse encodedResponse)
        {
            /**
             * Sending a response is a one-shot operation.
             * Do not allow duplicate responses.
             */
            if (sent.compareAndSet(false, true) == false)
            {
                return;
            }

            /**
             * Decode the body.
             */
            final ByteBuf body = Unpooled.copiedBuffer(encodedResponse.getBody().asReadOnlyByteBuffer());

            /**
             * Decode the HTTP status code.
             */
            final HttpResponseStatus status = encodedResponse.hasStatus()
                    ? HttpResponseStatus.valueOf(encodedResponse.getStatus())
                    : HttpResponseStatus.OK;

            /**
             * Create the response.
             */
            final FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, status, body);

            /**
             * Decode the headers.
             */
            for (Entry<String, HttpHeader> header : encodedResponse.getHeadersMap().entrySet())
            {
                response.headers().add(header.getKey(), header.getValue().getValuesList());
            }

            /**
             * Decode the content-type.
             */
            if (encodedResponse.hasContentType())
            {
                response.headers().add(Names.CONTENT_TYPE, encodedResponse.getContentType());
            }
            else
            {
                response.headers().add(Names.CONTENT_TYPE, MediaType.OCTET_STREAM.toString());
            }

            /**
             * Decode the content-length.
             */
            response.headers().add(Names.CONTENT_LENGTH, encodedResponse.getBody().size());

            /**
             * Send the response to the client.
             */
            ctx.writeAndFlush(response);

            /**
             * Close the connection.
             */
            close();
        }

        public void close ()
        {
            /**
             * Release this connection.
             */
            connections.remove(correlationId);

            /**
             * Formally close the connection.
             */
            if (closed.compareAndSet(false, true))
            {
                ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
            }
        }
    }

    /**
     * Use this method to begin creating a new web-server instance.
     *
     * @return a builder that can build a web-server.
     */
    public static Builder newWebServer ()
    {
        return new Builder();
    }

    /**
     * Builder of Web Servers.
     */
    public static final class Builder
    {
        private String serverName = "";

        private String replyTo = "";

        private String host = "localhost";

        private int port = 8080;

        private int aggregationCapacity = 65536;

        private Duration responseTimeout = Duration.ofSeconds(60);

        private Builder ()
        {
            // Pass.
        }

        /**
         * Set the reply-to field to embed inside of all requests.
         *
         * @param value will be embedded in all requests.
         * @return this.
         */
        public Builder withReplyTo (final String value)
        {
            this.replyTo = value;
            return this;
        }

        /**
         * Set the human-readable name of the new web-server.
         *
         * @param value will be embedded in all requests.
         * @return this.
         */
        public Builder withServerName (final String value)
        {
            this.serverName = value;
            return this;
        }

        /**
         * Set the maximum capacity of HTTP Chunk Aggregation.
         *
         * @param capacity will limit the size of incoming messages.
         * @return this.
         */
        public Builder withAggregationCapacity (final int capacity)
        {
            Preconditions.checkArgument(capacity >= 0, "capacity < 0");
            this.aggregationCapacity = capacity;
            return this;
        }

        /**
         * Connections will be closed automatically, if a response exceeds this timeout.
         *
         * @param timeout is the maximum amount of time allowed for a response.
         * @return this.
         */
        public Builder withResponseTimeout (final Duration timeout)
        {
            responseTimeout = Objects.requireNonNull(timeout, "timeout");
            return this;
        }

        /**
         * Specify the host that the server will listen on.
         *
         * @param host is a host-name or IP address.
         * @return this.
         */
        public Builder withHost (final String host)
        {
            this.host = Objects.requireNonNull(host, "host");
            return this;
        }

        /**
         * Set the port that the server will listen on.
         *
         * @param value is the port to use.
         * @return this.
         */
        public Builder withPort (final int value)
        {
            this.port = value;
            return this;
        }

        /**
         * Construct the web-server and start it up.
         *
         * @return the new web-server.
         */
        public WebServer build ()
        {
            final WebServer server = new WebServer(this);
            return server;
        }
    }
}
