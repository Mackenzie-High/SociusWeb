/*
 * Copyright 2019 Michael Mackenzie High
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mackenziehigh.socius.web;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.protobuf.ByteString;
import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage.Actor;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import com.mackenziehigh.socius.web.web_m.HttpRequest;
import com.mackenziehigh.socius.web.web_m.WebSocketRequest;
import com.mackenziehigh.socius.web.web_m.WebSocketResponse;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A experimental non-blocking HTTP server based on the Netty framework.
 */
public final class SocketServer
{
    private static final Logger logger = LogManager.getLogger(SocketServer.class);

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
    private final Cascade.Stage stage = Cascade.newStage();

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
     * This processor will be used to send HTTP requests out of the server,
     * so that external handler actors can process the requests.
     */
    private final Actor<WebSocketRequest, WebSocketRequest> requestsOut;

    /**
     * This processor will receive the HTTP responses from the external actors
     * and then will route those responses to the originating connection.
     */
    private final Actor<WebSocketResponse, WebSocketResponse> responsesIn;

    /**
     * Sole Constructor.
     *
     * @param builder contains the initial server settings.
     */
    private SocketServer (final Builder builder)
    {
        this.host = builder.host;
        this.port = builder.port;
        this.aggregationCapacity = builder.aggregationCapacity;
        this.serverName = builder.serverName;
        this.replyTo = builder.replyTo;
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
    public Output<WebSocketRequest> requestsOut ()
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
    public Input<WebSocketResponse> responsesIn ()
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
    public SocketServer start ()
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
    public SocketServer stop ()
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

    private void run ()
    {
        // Configure the server.
        final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        final EventLoopGroup workerGroup = new NioEventLoopGroup();
        try
        {
            ServerBootstrap b = new ServerBootstrap();
            b.option(ChannelOption.SO_BACKLOG, 1024);
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new HTTPInitializer());

            Channel ch = b.bind(host, port).sync().channel();

            ch.closeFuture().sync();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        finally
        {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    private WebSocketRequest onRequest (final WebSocketRequest message)
    {
        return message;
    }

    private void onResponse (final WebSocketResponse message)
    {
        /**
         * The message is supposed to contain the Correlation-ID of the connection.
         * Use the Correlation-ID in order to lookup the corresponding collection.
         * If no Correlation-ID is present, then simply ignore the message,
         * because there is nothing that we can really do in this situation.
         * If no connection is present with the given Correlation-ID,
         * then ignore the message, because the connection has already closed.
         */
        final Connection connection = message.hasCorrelationId() ? connections.get(message.getCorrelationId()) : null;

        if (connection == null)
        {
            return;
        }

        /**
         * If the message contains binary data to send to the client,
         * then send that data to the client immediately.
         */
        if (message.hasData() && message.getData().hasBlob())
        {
            connection.onBlobFromServer(message);
        }

        /**
         * If the message contains textual data to send to the client,
         * then send that data to the client immediately.
         */
        if (message.hasData() && message.getData().hasText())
        {
            connection.onTextFromServer(message);
        }

        /**
         * If the server-side wants the web-socket to be closed now,
         * then go ahead and close the web-socket immediately.
         */
        if (message.hasClose())
        {
            connection.onCloseFromServer();
        }
    }

    private final class HTTPInitializer
            extends ChannelInitializer<SocketChannel>
    {

        @Override
        protected void initChannel (final SocketChannel channel)
        {
            channel.pipeline().addLast(new HttpResponseEncoder());
            channel.pipeline().addLast(new HttpRequestDecoder()); // TODO: Args?
            channel.pipeline().addLast(new HttpObjectAggregator(aggregationCapacity, true));
            channel.pipeline().addLast("httpHandler", new HttpServerHandler());
        }
    }

    private final class HttpServerHandler
            extends ChannelInboundHandlerAdapter
    {

//        WebSocketServerHandshaker handshaker;
        @Override
        public void channelRead (final ChannelHandlerContext ctx,
                                 final Object msg)
                throws URISyntaxException
        {

            if (msg instanceof FullHttpRequest)
            {
                final FullHttpRequest httpRequest = (FullHttpRequest) msg;

                final HttpHeaders headers = httpRequest.headers();
                System.out.println("Connection : " + headers.get("Connection"));
                System.out.println("Upgrade : " + headers.get("Upgrade"));

                if ("Upgrade".equalsIgnoreCase(headers.get(HttpHeaderNames.CONNECTION))
                    && "WebSocket".equalsIgnoreCase(headers.get(HttpHeaderNames.UPGRADE)))
                {
                    final Connection connection = new Connection(ctx, httpRequest);
                    final WebSocketHandler handler = new WebSocketHandler(connection);
                    ctx.pipeline().replace(this, "websocketHandler", handler);
                    handleHandshake(ctx, httpRequest);
                    connection.open();
                }
            }
            else
            {
                System.out.println("Incoming request is unknown");
            }
        }

        /* Do the handshaking for WebSocket request */
        protected void handleHandshake (ChannelHandlerContext ctx,
                                        FullHttpRequest req)
        {
            // TODO: Args
            final WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(getWebSocketURL(req), null, true);
            final WebSocketServerHandshaker handshaker = wsFactory.newHandshaker(req);

            if (handshaker == null)
            {
                WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
            }
            else
            {
                handshaker.handshake(ctx.channel(), req);
            }
        }

        protected String getWebSocketURL (FullHttpRequest req)
        {
            System.out.println("Req URI : " + req.getUri());
            String url = "ws://" + req.headers().get("Host") + req.getUri();
            System.out.println("Constructed URL : " + url);
            return url;
        }
    }

    private final class WebSocketHandler
            extends ChannelInboundHandlerAdapter
    {

        private final Connection connection;

        public WebSocketHandler (final Connection connection)
        {
            this.connection = connection;
        }

        @Override
        public void channelRead (final ChannelHandlerContext ctx,
                                 final Object msg)
        {
            if (msg instanceof BinaryWebSocketFrame)
            {
                connection.onBlobFromClient((BinaryWebSocketFrame) msg);
            }
            else if (msg instanceof TextWebSocketFrame)
            {
                connection.onTextFromClient((TextWebSocketFrame) msg);
            }
            else if (msg instanceof PingWebSocketFrame)
            {
                connection.onPing((PingWebSocketFrame) msg);
            }
            else if (msg instanceof PongWebSocketFrame)
            {
                connection.onPong((PongWebSocketFrame) msg);
            }
            else if (msg instanceof CloseWebSocketFrame)
            {
                connection.onCloseFromClient((CloseWebSocketFrame) msg);
            }
            else
            {
                System.out.println("Unsupported WebSocketFrame");
            }
        }
    }

    private final class Connection
    {
        public final UUID correlationId = UUID.randomUUID();

        public final String correlationIdAsString = correlationId.toString();

        private final ChannelHandlerContext context;

        private final HttpRequest httpRequest;

        public Connection (final ChannelHandlerContext context,
                           final FullHttpRequest fullHttpRequest)
                throws URISyntaxException
        {
            this.context = context;
            final long sequenceNumber = seqnum.incrementAndGet();
            this.httpRequest = HttpRequestEncoder.encode(fullHttpRequest, serverName, serverId, replyTo, sequenceNumber);
        }

        public void open ()
        {
            connections.put(correlationIdAsString, this);
        }

        public void onTextFromServer (final WebSocketResponse message)
        {
            final TextWebSocketFrame frame = new TextWebSocketFrame(message.getData().getText());
            context.writeAndFlush(frame);
        }

        public void onBlobFromServer (final WebSocketResponse message)
        {
            final ByteBuf blob = Unpooled.copiedBuffer(message.getData().getBlob().toByteArray());
            final BinaryWebSocketFrame frame = new BinaryWebSocketFrame(blob);
            context.writeAndFlush(frame);
        }

        public void onTextFromClient (final TextWebSocketFrame message)
        {
            final WebSocketRequest.Builder builder = WebSocketRequest.newBuilder();
            builder.setCorrelationId(correlationIdAsString);
            builder.setSequenceNumber(seqnum.incrementAndGet());
            builder.setServerId(serverId);
            builder.setServerName(serverName);
            builder.setReplyTo(replyTo);
            builder.setTimestamp(System.currentTimeMillis());
            builder.setRequest(httpRequest);
            builder.setData(web_m.DataFrame.newBuilder().setText(message.text()));
            requestsOut.accept(builder.build());
        }

        public void onBlobFromClient (final BinaryWebSocketFrame message)
        {
            final WebSocketRequest.Builder builder = WebSocketRequest.newBuilder();
            builder.setCorrelationId(correlationIdAsString);
            builder.setSequenceNumber(seqnum.incrementAndGet());
            builder.setServerId(serverId);
            builder.setServerName(serverName);
            builder.setReplyTo(replyTo);
            builder.setTimestamp(System.currentTimeMillis());
            builder.setRequest(httpRequest);
            final ByteString data = ByteString.copyFrom(message.content().nioBuffer());
            builder.setData(web_m.DataFrame.newBuilder().setBlob(data));
            requestsOut.accept(builder.build());
        }

        public void onPing (final PingWebSocketFrame message)
        {
        }

        public void onPong (final PongWebSocketFrame message)
        {
        }

        public void onCloseFromClient (final CloseWebSocketFrame message)
        {
            // reason and status code
            connections.remove(correlationId);
        }

        public void onCloseFromServer ()
        {
            connections.remove(correlationId);
        }
    }

    /**
     * Use this method to begin creating a new web-server instance.
     *
     * @return a builder that can build a web-server.
     */
    public static Builder newSocketServer ()
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
        public SocketServer build ()
        {
            final SocketServer server = new SocketServer(this);
            return server;
        }
    }

    public static void main (String[] args)
    {
        final SocketServer server = SocketServer.newSocketServer().withPort(9000).build().start();

        final Actor<WebSocketRequest, WebSocketResponse> actor = server.stage.newActor().withScript(SocketServer::actor).create();

        server.requestsOut().connect(actor.input());
        server.responsesIn().connect(actor.output());
    }

    private static WebSocketResponse actor (final WebSocketRequest msg)
    {
        final WebSocketResponse.Builder resp = WebSocketResponse.newBuilder();
        resp.setCorrelationId(msg.getCorrelationId());
        resp.setData(web_m.DataFrame.newBuilder().setText(msg.toString()));
        resp.setClose(false);
        return resp.build();
    }
}
