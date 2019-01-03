package com.mackenziehigh.socius.web;

import com.google.common.base.Preconditions;
import com.mackenziehigh.cascade.Cascade;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import static io.netty.handler.codec.http.HttpMethod.GET;
import io.netty.handler.codec.http.HttpObjectAggregator;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpUtil;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.CharsetUtil;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class WebSocketServer
{
    private static final Logger logger = LogManager.getLogger(WebSocketServer.class);

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
//    private final Map<String, WebServer.Connection> connections = Maps.newConcurrentMap();
    private WebSocketServer (final Builder builder)
    {
        this.host = builder.host;
        this.port = builder.port;
        this.aggregationCapacity = builder.aggregationCapacity;
        this.serverName = builder.serverName;
        this.replyTo = builder.replyTo;
        this.responseTimeout = builder.responseTimeout;
    }

    public static Builder newWebSocketServer ()
    {
        return new Builder();
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
    public WebSocketServer start ()
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
    public WebSocketServer stop ()
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

            final ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new WebSocketServerInitializer());

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
//        try
//        {
//            stop();
//        }
//        catch (InterruptedException ex)
//        {
//            logger.error(ex);
//        }
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
        public WebSocketServer build ()
        {
            final WebSocketServer server = new WebSocketServer(this);
            return server;
        }
    }

    /**
     */
    private static class WebSocketServerInitializer
            extends ChannelInitializer<SocketChannel>
    {

        private static final String WEBSOCKET_PATH = "/websocket";

        @Override
        public void initChannel (SocketChannel ch)
                throws Exception
        {
            ChannelPipeline pipeline = ch.pipeline();
            pipeline.addLast(new HttpServerCodec());
            pipeline.addLast(new HttpObjectAggregator(65536));
            pipeline.addLast(new WebSocketServerCompressionHandler());
            pipeline.addLast(new WebSocketServerProtocolHandler(WEBSOCKET_PATH, null, true));
            pipeline.addLast(new WebSocketIndexPageHandler());
            pipeline.addLast(new WebSocketFrameHandler());
        }
    }

    /**
     * Outputs index page content.
     */
    public static class WebSocketIndexPageHandler
            extends SimpleChannelInboundHandler<FullHttpRequest>
    {

        @Override
        protected void channelRead0 (ChannelHandlerContext ctx,
                                     FullHttpRequest req)
                throws Exception
        {
            // Handle a bad request.
            if (!req.decoderResult().isSuccess())
            {
                sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST));
                return;
            }

            // Allow only GET methods.
            if (req.method() != GET)
            {
                sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, FORBIDDEN));
                return;
            }

            // Send the index page
            if ("/".equals(req.uri()) || "/index.html".equals(req.uri()))
            {
                ByteBuf content = Unpooled.copiedBuffer("HelloWorld = " + ctx.hashCode(), StandardCharsets.US_ASCII);
                FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, OK, content);

                res.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
                HttpUtil.setContentLength(res, content.readableBytes());

                sendHttpResponse(ctx, req, res);
            }
            else
            {
                sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND));
            }
        }

        @Override
        public void exceptionCaught (ChannelHandlerContext ctx,
                                     Throwable cause)
        {
            cause.printStackTrace();
            ctx.close();
        }

        private static void sendHttpResponse (ChannelHandlerContext ctx,
                                              FullHttpRequest req,
                                              FullHttpResponse res)
        {
            // Generate an error page if response getStatus code is not OK (200).
            if (res.status().code() != 200)
            {
                ByteBuf buf = Unpooled.copiedBuffer(res.status().toString(), CharsetUtil.UTF_8);
                res.content().writeBytes(buf);
                buf.release();
                HttpUtil.setContentLength(res, res.content().readableBytes());
            }

            // Send the response and close the connection if necessary.
            ChannelFuture f = ctx.channel().writeAndFlush(res);
            if (!HttpUtil.isKeepAlive(req) || res.status().code() != 200)
            {
                f.addListener(ChannelFutureListener.CLOSE);
            }
        }
    }

    /**
     * Echoes uppercase content of text frames.
     */
    private static class WebSocketFrameHandler
            extends SimpleChannelInboundHandler<WebSocketFrame>
    {
        @Override
        protected void channelRead0 (ChannelHandlerContext ctx,
                                     WebSocketFrame frame)
                throws Exception
        {
            // ping and pong frames already handled

            if (frame instanceof TextWebSocketFrame)
            {
                // Send the uppercase string back.
                String request = ((TextWebSocketFrame) frame).text();

                ctx.channel().writeAndFlush(new TextWebSocketFrame("ERIN4 = " + ctx.hashCode()));
            }
            else
            {
                String message = "unsupported frame type: " + frame.getClass().getName();
                System.out.println(message);
                throw new UnsupportedOperationException(message);
            }
        }
    }

    public static void main (String[] args)
    {
        final WebSocketServer server = WebSocketServer
                .newWebSocketServer()
                .withPort(3000)
                .build()
                .start();
    }
}
