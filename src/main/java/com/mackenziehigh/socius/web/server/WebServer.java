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
package com.mackenziehigh.socius.web.server;

import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import com.mackenziehigh.socius.web.messages.web_m.ServerSideHttpRequest;
import com.mackenziehigh.socius.web.messages.web_m.ServerSideHttpResponse;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.AdaptiveRecvByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.traffic.GlobalChannelTrafficShapingHandler;
import java.io.Closeable;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A non-blocking HTTP server based on the Netty framework,
 * for creating RESTful APIs using the Cascade framework.
 */
public final class WebServer
        implements Closeable
{
    public static final String DEFAULT_SERVER_NAME = "";

    public static final String DEFAULT_REPLY_TO = "";

    public static final String DEFAULT_BIND_ADDRESS = "127.0.0.1";

    public static final int DEFAULT_PORT = 8080;

    public static final int DEFAULT_MAX_MESSAGES_PER_READ = 1;

    public static final int DEFAULT_MAX_REQUEST_SIZE = 64 * 1024;

    public static final int DEFAULT_MAX_INITIAL_LINE_SIZE = 1024;

    public static final int DEFAULT_MAX_HEADER_SIZE = 8 * 1024;

    public static final int DEFAULT_COMPRESSION_LEVEL = 6;

    public static final int DEFAULT_COMPRESSION_WINDOW_BITS = 15;

    public static final int DEFAULT_COMPRESSION_MEMORY_LEVEL = 8;

    public static final int DEFAULT_COMPRESSION_THRESHOLD = 0; // Always Compress.

    public static final int DEFAULT_RECV_ALLOCATOR_MIN = 64;

    public static final int DEFAULT_RECV_ALLOCATOR_MAX = 2 * DEFAULT_MAX_REQUEST_SIZE;

    public static final int DEFAULT_RECV_ALLOCATOR_INITIAL = 1024;

    public static final int DEFAULT_SOFT_CONNECTION_LIMIT = 128;

    public static final int DEFAULT_HARD_CONNECTION_LIMIT = 4 * DEFAULT_SOFT_CONNECTION_LIMIT;

    public static final long DEFAULT_SERVER_UPLINK_BANDWIDTH = DEFAULT_SOFT_CONNECTION_LIMIT * DEFAULT_MAX_REQUEST_SIZE;

    public static final long DEFAULT_SERVER_DOWNLINK_BANDWIDTH = DEFAULT_SERVER_UPLINK_BANDWIDTH;

    public static final long DEFAULT_CONNECTION_UPLINK_BANDWIDTH = DEFAULT_SERVER_UPLINK_BANDWIDTH; // In effect, disable the limit.

    public static final long DEFAULT_CONNECTION_DOWNLINK_BANDWIDTH = DEFAULT_CONNECTION_UPLINK_BANDWIDTH;

    public static final Duration DEFAULT_MAX_PAUSE_TIME = Duration.ofSeconds(1);

    public static final Duration DEFAULT_SLOW_UPLINK_TIMEOUT = Duration.ofSeconds(8);

    public static final Duration DEFAULT_SLOW_DOWNLINK_TIMEOUT = Duration.ofSeconds(8);

    public static final Duration DEFAULT_RESPONSE_TIMEOUT = Duration.ofSeconds(32);

    public static final Duration DEFAULT_CONNECTION_TIMEOUT = Duration.ofSeconds(8)
            .plus(DEFAULT_SLOW_UPLINK_TIMEOUT)
            .plus(DEFAULT_RESPONSE_TIMEOUT)
            .plus(DEFAULT_SLOW_DOWNLINK_TIMEOUT);

    public static final int BOSS_THREAD_COUNT = 1;

    public static final int WORKER_THREAD_COUNT = 1;

    private final WebLogger serverLogger;

    private final String serverId = UUID.randomUUID().toString();

    private final String serverName;

    private final String replyTo;

    private final String bindAddress;

    private final int port;

    private final int maxMessagesPerRead;

    private final int recvAllocatorMin;

    private final int recvAllocatorInitial;

    private final int recvAllocatorMax;

    private final int softConnectionLimit;

    private final int hardConnectionLimit;

    private final long maxServerUplinkBandwidth;

    private final long maxServerDownlinkBandwidth;

    private final long maxConnectionUplinkBandwidth;

    private final long maxConnectionDownlinkBandwidth;

    private final Duration maxPauseTime;

    private final int maxRequestSize;

    private final int maxInitialLineSize;

    private final int maxHeaderSize;

    private final int compressionLevel;

    private final int compressionWindowBits;

    private final int compressionMemoryLevel;

    private final int compressionThreshold;

    private final Duration SlowUplinkTimeout;

    private final Duration SlowDownlinkTimeout;

    private final Duration responseTimeout;

    private final Duration connectionTimeout;

    private final boolean hasShutdownHook;

    private final Precheck prechecks;

    private final ScheduledExecutorService service = Executors.newScheduledThreadPool(8);

    private final Stage stage = Cascade.newExecutorStage(service).addErrorHandler(ex -> serverLogger().onException(ex));

    private final AtomicLong connectionCount = new AtomicLong();

    private final AdaptiveRecvByteBufAllocator recvBufferAllocator;

    private final GlobalChannelTrafficShapingHandler trafficShapingHandler;

    /**
     * TODO: Make the sequence-numbers increment monotonically.
     */
    private final Translator translator;

    private final Correlator router;

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

    private final Initializer initializer;

    private final AtomicLong uplinkTimeoutCounter = new AtomicLong();

    private final AtomicLong downlinkTimeoutCounter = new AtomicLong();

    private final AtomicLong responseTimeoutCounter = new AtomicLong();

    private final AtomicLong connectionTimeoutCounter = new AtomicLong();

    /**
     * Sole Constructor.
     *
     * @param builder contains the initial server settings.
     */
    private WebServer (final Builder builder)
    {
        this.serverLogger = builder.builderServerLogger;
        this.serverName = builder.builderServerName;
        this.replyTo = builder.builderReplyTo;
        this.bindAddress = builder.builderBindAddress;
        this.port = builder.builderPort;
        this.maxMessagesPerRead = builder.builderMaxMessagesPerRead;
        this.recvAllocatorMin = builder.builderRecvAllocatorMin;
        this.recvAllocatorInitial = builder.builderRecvAllocatorInitial;
        this.recvAllocatorMax = builder.builderRecvAllocatorMax;
        this.softConnectionLimit = builder.builderSoftConnectionLimit;
        this.hardConnectionLimit = builder.builderHardConnectionLimit;
        this.maxServerUplinkBandwidth = builder.builderMaxServerUplinkBandwidth;
        this.maxServerDownlinkBandwidth = builder.builderMaxServerDownlinkBandwidth;
        this.maxConnectionUplinkBandwidth = builder.builderMaxConnectionUplinkBandwidth;
        this.maxConnectionDownlinkBandwidth = builder.builderMaxConnectionDownlinkBandwidth;
        this.maxPauseTime = builder.builderMaxPauseTime;
        this.maxRequestSize = builder.builderMaxRequestSize;
        this.maxInitialLineSize = builder.builderMaxInitialLineSize;
        this.maxHeaderSize = builder.builderMaxHeaderSize;
        this.compressionLevel = builder.builderCompressionLevel;
        this.compressionWindowBits = builder.builderCompressionWindowBits;
        this.compressionMemoryLevel = builder.builderCompressionMemoryLevel;
        this.compressionThreshold = builder.builderCompressionThreshold;
        this.SlowDownlinkTimeout = builder.builderSlowDownlinkTimeout;
        this.SlowUplinkTimeout = builder.builderSlowUplinkTimeout;
        this.responseTimeout = builder.builderResponseTimeout;
        this.connectionTimeout = builder.builderConnectionTimeout;
        this.hasShutdownHook = builder.builderShutdownHook;
        this.prechecks = Prechecks.chain(builder.builderPrechecks, Prechecks.deny());

        this.recvBufferAllocator = new AdaptiveRecvByteBufAllocator(recvAllocatorMin, recvAllocatorInitial, recvAllocatorMax);
        this.recvBufferAllocator.maxMessagesPerRead(maxMessagesPerRead);

        this.trafficShapingHandler = new GlobalChannelTrafficShapingHandler(service,
                                                                            maxServerDownlinkBandwidth,
                                                                            maxServerUplinkBandwidth,
                                                                            maxConnectionDownlinkBandwidth,
                                                                            maxConnectionUplinkBandwidth,
                                                                            TimeUnit.SECONDS.toMillis(1),
                                                                            maxPauseTime.toMillis());

        this.translator = new Translator(serverName, serverId, replyTo);
        this.initializer = new Initializer();

        this.router = new Correlator(service, responseTimeout);
    }

    private WebLogger serverLogger ()
    {
        return serverLogger;
    }

    /**
     * Expose the initializer to simplify unit-testing.
     *
     * @return the initializer that initializes each channel.
     */
    final ChannelInitializer<SocketChannel> initializer ()
    {
        return initializer;
    }

    public long getUplinkTimeoutCount ()
    {
        return uplinkTimeoutCounter.get();
    }

    public long getDownlinkTimeoutCount ()
    {
        return downlinkTimeoutCounter.get();
    }

    public long getResponseTimeoutCount ()
    {
        return responseTimeoutCounter.get();
    }

    public long getConnectionTimeoutCount ()
    {
        return connectionTimeoutCounter.get();
    }

    /**
     * Get the current state of the sequence-number generator.
     *
     * @return the number of translated requests, thus far.
     */
    public long getSequenceCount ()
    {
        return translator.sequenceCount();
    }

    /**
     * Get the number of open connections at this time.
     *
     * @return the number of pending requests.
     */
    public int getConnectionCount ()
    {
        return connectionCount.intValue();
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
    public String getBindAddress ()
    {
        return bindAddress;
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
     * Get the maximum amount of time the server will wait for a request
     * to be read off of the socket into a full message object.
     *
     * @return the read timeout.
     */
    public Duration getSlowUplinkTimeout ()
    {
        return SlowUplinkTimeout;
    }

    /**
     * Get the maximum amount of time the server will wait
     * for a response to be written to the socket.
     *
     * @return the read timeout.
     */
    public Duration getSlowDownlinkTimeout ()
    {
        return SlowDownlinkTimeout;
    }

    /**
     * Get the maximum amount of time the server will wait for a response,
     * before sending a default error response and closing the connection.
     *
     * @return the connection timeout.
     */
    public Duration getResponseTimeout ()
    {
        return responseTimeout;
    }

    /**
     * Get the maximum amount of time the server will wait for a response,
     * before the connection is closed without sending a response at all.
     *
     * @return the connection timeout.
     */
    public Duration getConnectionTimeout ()
    {
        return connectionTimeout;
    }

    /**
     * Get the maximum number of bytes per second that can be sent to the server, overall.
     *
     * @return the global uplink bandwidth limit.
     */
    public long getMaxServerUplinkBandwidth ()
    {
        return maxServerUplinkBandwidth;
    }

    /**
     * Get the maximum number of bytes per second that can be sent from the server, overall.
     *
     * @return the global downlink bandwidth limit.
     */
    public long getMaxServerDownlinkBandwidth ()
    {
        return maxServerDownlinkBandwidth;
    }

    /**
     * Get the maximum number of bytes per second that can be sent to the server, per connection.
     *
     * @return the connection-specific uplink bandwidth limit.
     */
    public long getMaxConnectionUplinkBandwidth ()
    {
        return maxConnectionUplinkBandwidth;
    }

    /**
     * Get the maximum number of bytes per second that can be sent from the server, per connection.
     *
     * @return the connection-specific uplink bandwidth limit.
     */
    public long getMaxConnectionDownlinkBandwidth ()
    {
        return maxConnectionDownlinkBandwidth;
    }

    /**
     * Get the maximum amount of time to pause throughput due to excessive bandwidth.
     *
     * @return the maximum pause time.
     */
    public Duration getMaxPauseTime ()
    {
        return maxPauseTime;
    }

    /**
     * Get the the maximum allowed overall size of HTTP requests.
     *
     * @return the maximum size of each request.
     */
    public int getMaxRequestSize ()
    {
        return maxRequestSize;
    }

    /**
     * Get the the maximum allowed size of the request-line in an HTTP request.
     *
     * @return the maximum size of the first line of an HTTP request.
     */
    public int getMaxInitialLineSize ()
    {
        return maxInitialLineSize;
    }

    /**
     * Get the the maximum allowed size of the headers in an HTTP request.
     *
     * @return the maximum combined size of the headers in an HTTP request.
     */
    public int getMaxHeaderSize ()
    {
        return maxHeaderSize;
    }

    /**
     * Get the maximum number of allowed concurrent connections before
     * new connections are rejected by sending an automatic response.
     *
     * @return the connection limit.
     */
    public int getSoftConnectionLimit ()
    {
        return softConnectionLimit;
    }

    /**
     * Get the maximum number of allowed concurrent connections before
     * new connections are rejected by closing the connection without
     * sending any response to the client.
     *
     * @return the connection limit.
     */
    public int getHardConnectionLimit ()
    {
        return hardConnectionLimit;
    }

    /**
     * Get the maximum number of reads that will be performed per read loop.
     *
     * @return the read loop iteration limit.
     */
    public int getMaxMessagesPerRead ()
    {
        return maxMessagesPerRead;
    }

    /**
     * Get the minimum size that will be used to allocate a new
     * byte buffer when reading from the server socket,
     * after the adaptive algorithm makes adjustments.
     *
     * @return the inclusive minimum buffer size.
     */
    public int getRecvBufferMinSize ()
    {
        return recvAllocatorMin;
    }

    /**
     * Get the maximum size that will be used to allocate a new
     * byte buffer when reading from the server socket,
     * after the adaptive algorithm makes adjustments.
     *
     * @return the inclusive maximum buffer size.
     */
    public int getRecvBufferMaxSize ()
    {
        return recvAllocatorMax;
    }

    /**
     * Get the initial size that will be used to allocate a new
     * byte buffer when reading from the server socket,
     * before the adaptive algorithm makes adjustments.
     *
     * @return the initial buffer size.
     */
    public int getRecvBufferInitialSize ()
    {
        return recvAllocatorInitial;
    }

    /**
     * Get the compression-level applied to the downlink,
     * when the Accept-Encoding header allows it.
     *
     * <p>
     * See Also: <a href="https://netty.io/4.1/api/io/netty/handler/codec/http/HttpContentCompressor.html">HttpContentCompressor</a>
     * </p>
     *
     * @return the compression-level.
     */
    public int getCompressionLevel ()
    {
        return compressionLevel;
    }

    /**
     * Get the window-size setting used during downlink compression.
     *
     * <p>
     * See Also: <a href="https://netty.io/4.1/api/io/netty/handler/codec/http/HttpContentCompressor.html">HttpContentCompressor</a>
     * </p>
     *
     * @return the window-size of the compressor.
     */
    public int getCompressionWindowBits ()
    {
        return compressionWindowBits;
    }

    /**
     * Get the memory-level setting used during downlink compression.
     *
     * <p>
     * See Also: <a href="https://netty.io/4.1/api/io/netty/handler/codec/http/HttpContentCompressor.html">HttpContentCompressor</a>
     * </p>
     *
     * @return the memory-level of the compressor.
     */
    public int getCompressionMemoryLevel ()
    {
        return compressionMemoryLevel;
    }

    /**
     * Get the compression-threshold setting used during downlink compression.
     *
     * <p>
     * See Also: <a href="https://netty.io/4.1/api/io/netty/handler/codec/http/HttpContentCompressor.html">HttpContentCompressor</a>
     * </p>
     *
     * @return the memory-level of the compressor.
     */
    public int getCompressionThreshold ()
    {
        return compressionThreshold;
    }

    /**
     * Use this connection to receive HTTP Requests from this HTTP server.
     *
     * @return the connection.
     */
    public Output<ServerSideHttpRequest> requestsOut ()
    {
        return router.requestsOut.output();
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
    public Input<ServerSideHttpResponse> responsesIn ()
    {
        return router.responsesIn.input();
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
            serverLogger.onStart();
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
     */
    public WebServer stop ()
    {
        if (stopped.compareAndSet(false, true))
        {
            if (shutdownHook != null)
            {
                try
                {
                    shutdownHook.channel().close().sync();
                }
                catch (Throwable ex)
                {
                    serverLogger.onException(ex);
                }
            }
        }
        return this;
    }

    @Override
    public void close ()
    {
        stop();
    }

    /**
     * Server Main.
     */
    private void run ()
    {
        final EventLoopGroup bossGroup = new NioEventLoopGroup(BOSS_THREAD_COUNT);
        final EventLoopGroup workerGroup = new NioEventLoopGroup(WORKER_THREAD_COUNT);
        try
        {
            if (hasShutdownHook)
            {
                Runtime.getRuntime().addShutdownHook(new Thread(this::onShutdown));
            }

            final ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup);
            bootstrap.channel(NioServerSocketChannel.class);
            bootstrap.childHandler(initializer);
            bootstrap.option(ChannelOption.RCVBUF_ALLOCATOR, recvBufferAllocator);

            shutdownHook = bootstrap.bind(bindAddress, port).sync();
            shutdownHook.channel().closeFuture().sync();
        }
        catch (Throwable ex)
        {
            serverLogger.onException(ex);
        }
        finally
        {
            trafficShapingHandler.release();
            bossGroup.shutdownGracefully().syncUninterruptibly();
            workerGroup.shutdownGracefully().syncUninterruptibly();
            stage.close();
            // Delayed tasks (timeout tasks) can delay shutdown.
            // Therefore, we must use shutdownNow().
            service.shutdownNow();
            serverLogger.onStop();
        }
    }

    private void onShutdown ()
    {
        stop();
    }

    /**
     * Logic to setup the Netty pipeline to handle a <b>single</b> connection.
     *
     * <p>
     * The logic herein is executed whenever a new connection is established.
     * However, only a single Initializer object exists.
     * </p>
     */
    private final class Initializer
            extends ChannelInitializer<SocketChannel>
    {
        /**
         * If this handler receives a message before the timeout expires,
         * then the message will be forwarded to the rest of the pipeline;
         * otherwise, the message will not be forwarded, because the connection
         * is in the process of being closed due to the timeout.
         */
        private final class SlowUplinkDetector
                extends MessageToMessageDecoder<FullHttpRequest>
        {
            private final AtomicBoolean slowUplinkTimeoutExpired = new AtomicBoolean(false);

            @Override
            protected void decode (final ChannelHandlerContext ctx,
                                   final FullHttpRequest msg,
                                   final List<Object> out)
            {
                if (slowUplinkTimeoutExpired.compareAndSet(false, true))
                {
                    out.add(msg);
                }
            }

            public void onSlowUplinkTimeout (final SocketChannel channel)
            {
                if (slowUplinkTimeoutExpired.compareAndSet(false, true))
                {
                    closeWithNoResponse(channel);
                }
            }
        }

        /**
         * This object is used to detect when a response is sent to the client.
         */
        private final class SlowDownlinkDetector
                extends MessageToMessageEncoder<HttpResponse>
        {
            @Override
            protected void encode (final ChannelHandlerContext ctx,
                                   final HttpResponse msg,
                                   final List<Object> out)
            {
                out.add(msg);

                if (msg.status() == HttpResponseStatus.CONTINUE)
                {
                    // Ignore, because this may occur multiple times,
                    // as the request is uploaded to the server.
                    // In other words, this is not a final response to the client.
                }
                else
                {
                    service.schedule(() -> onSlowDownlinkTimeout(ctx.channel()), SlowDownlinkTimeout.toMillis(), TimeUnit.MILLISECONDS);
                }
            }

            public void onSlowDownlinkTimeout (final Channel channel)
            {
                if (channel.isOpen())
                {
                    closeWithNoResponse(channel);
                }
            }
        }

        /**
         *
         */
        private final class Prechecker
                extends MessageToMessageDecoder<HttpRequest>
        {
            private final InetSocketAddress remoteAddress;

            private final InetSocketAddress localAddress;

            public Prechecker (final InetSocketAddress remoteAddress,
                               final InetSocketAddress localAddress)
            {
                this.remoteAddress = remoteAddress;
                this.localAddress = localAddress;
            }

            @Override
            protected void decode (final ChannelHandlerContext ctx,
                                   final HttpRequest msg,
                                   final List<Object> out)
            {
                try
                {
                    /**
                     * Convert the Netty-based request to a GPB-based request.
                     * Some parts of the request will be omitted,
                     * as they are not needed at this stage.
                     */
                    final ServerSideHttpRequest prefix = translator.prefixOf(remoteAddress, localAddress, msg);

                    final Precheck.Action decision = prechecks.check(prefix);

                    if (decision.action() == Precheck.ActionType.ACCEPT)
                    {
                        out.add(msg);
                    }
                    else if (decision.action() == Precheck.ActionType.REJECT)
                    {
                        final int statusCode = decision.status().orElse(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
                        sendErrorAndClose(ctx.channel(), statusCode);
                    }
                    else // DENY, FORWARD never occurs.
                    {
                        closeWithNoResponse(ctx.channel());
                    }
                }
                catch (Throwable e)
                {
                    serverLogger.onException(e); // TODO: channel logger
                    closeWithNoResponse(ctx.channel());
                }
            }
        }

        /**
         * An decoder that translates Netty-based HTTP requests to GPB-based HTTP requests.
         */
        private final class TranslationDecoder
                extends MessageToMessageDecoder<FullHttpRequest>
        {

            private final InetSocketAddress remoteAddress;

            private final InetSocketAddress localAddress;

            public TranslationDecoder (final InetSocketAddress remoteAddress,
                                       final InetSocketAddress localAddress)
            {
                this.remoteAddress = remoteAddress;
                this.localAddress = localAddress;
            }

            @Override
            protected void decode (final ChannelHandlerContext ctx,
                                   final FullHttpRequest msg,
                                   final List<Object> out)
            {
                try
                {
                    final ServerSideHttpRequest request = translator.requestToGPB(remoteAddress, localAddress, msg);
                    out.add(request);
                }
                catch (Throwable e)
                {
                    sendErrorAndClose(ctx.channel(), HttpResponseStatus.BAD_REQUEST.code());
                }
            }
        }

        /**
         * An encoder that translates GPB-based HTTP responses to Netty-based HTTP responses.
         */
        private final class TranslationEncoder
                extends MessageToMessageEncoder<ServerSideHttpResponse>
        {
            @Override
            protected void encode (final ChannelHandlerContext ctx,
                                   final ServerSideHttpResponse msg,
                                   final List<Object> out)
            {
                try
                {
                    final FullHttpResponse response = translator.responseFromGPB(msg);
                    response.retain();
                    out.add(response);
                }
                catch (Throwable ex)
                {
                    sendErrorAndClose(ctx.channel(), HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
                }
            }
        }

        @Override
        protected void initChannel (final SocketChannel channel)
        {
            final WebLogger channelLogger = WebLoggers.newSafeLogger(serverLogger.extend());

            /**
             * Configure the connection-specific logger.
             */
            channelLogger.setRemoteAddess(channel.remoteAddress().toString(), channel.remoteAddress().getPort()); // TODO
            channelLogger.onConnect();

            /**
             * The logger, etc, will need to be notified when the connection closes.
             */
            channel.closeFuture().addListener(x -> onClose(channelLogger));

            /**
             * Keep track of how many connections are open.
             */
            // TODO: HAndle more safely
            final long connectionNumber = connectionCount.incrementAndGet();

            /**
             * If there are more connections open than the hard-limit allows,
             * then go ahead and close this new connection without sending
             * any HTTP response to the client.
             *
             * Notice that this check is done before setting up the pipeline.
             * We want to avoid allocating additional unnecessary resources,
             * if we are dropping incoming requests due to resource exhaustion.
             */
            if (connectionNumber >= hardConnectionLimit)
            {
                // TODO: Log it
                closeWithNoResponse(channel);
                return;
            }

            /**
             * The maximum chunk size, which is required by the HttpRequestDecoder,
             * is always equal to the maximum request size, because the chunk
             * size does not actually limit the client.
             * Rather, if the max-chunk-size is smaller than a received chunk,
             * then the decoder will break the chunk into smaller chunks.
             * Since we aggregate all of the chunks into a single message,
             * we do not need chunks broken down into smaller chunks,
             * because it is pointless and wastes resources.
             */
            final int maxChunkSize = maxRequestSize;

            final boolean validateHeaders = true;

            /**
             * If the client attempts to upload a chunked request
             * that is larger than we are willing to accept,
             * then automatically close the connection with
             * an HTTP (417) (Expectation Failed) response.
             */
            final boolean closeOnExpectationFailed = true;

            final boolean strictDecompression = false;

            /**
             * The Traffic Shaping Handler will rate-limit the bandwidth
             * of data coming-from and going-to the client on both
             * a per-channel and per-server basis.
             *
             * Uplink Input: (0 .. M) Byte Buffers.
             * Uplink Output: (0 .. M) Byte Buffers.
             *
             * Downlink Input: (0 .. N) Byte Buffers.
             * Downlink Output: (0 .. N) Byte Buffers.
             */
            channel.pipeline().addLast(trafficShapingHandler);

            /**
             * The HTTP Request Decoder converts incoming Byte Buffers into
             * HTTP Request, HTTP Content, and Last HTTP Content objects,
             * as data is received through the pipeline from the client.
             *
             * Notice that a single request may be broken into an HTTP Request object,
             * multiple HTTP Content objects thereafter, and finally a Last HTTP Content object,
             * because of the client sending data using the HTTP 1.1 'chunked' encoding.
             * The HTTP Object Aggregator, further down the pipeline, will convert
             * the stream of multiple objects into a single Full HTTP Request object.
             *
             * Uplink Input: (0 .. M) Byte Buffers.
             * Uplink Output: (1) HTTP Request, (0 .. M) HTTP Content, (1) Last HTTP Content.
             *
             * Downlink Input: Pass-through.
             * Downlink Output: Pass-through.
             */
            channel.pipeline().addLast(new HttpRequestDecoder(maxInitialLineSize,
                                                              maxHeaderSize,
                                                              maxChunkSize,
                                                              validateHeaders));

            /**
             * The HTTP Response Encoder converts outgoing HTTP responses into Byte Buffers,
             * so that they can be written to the socket, and sent to the client.
             *
             * Uplink Input: Pass-through.
             * Uplink Output: Pass-through.
             *
             * Downlink Input: (1) HTTP Response.
             * Downlink Output: (0 .. N) Byte Buffers.
             */
            channel.pipeline().addLast(new HttpResponseEncoder());

            /**
             * The Slow Downlink Detector detects when the HTTP response begins
             * being sent to the client, so that we can force close the connection,
             * if the client reads the response to slowly.
             *
             * This is a defensive mechanism, which is intended to help defend against
             * slow HTTP attacks that maliciously read a response at a very low data-rate.
             *
             * Uplink Input: Pass-through.
             * Uplink Output: Pass-through.
             *
             * Downlink Input: (1) HTTP Response.
             * Downlink Output: (1) HTTP Response.
             */
            channel.pipeline().addLast(new SlowDownlinkDetector());

            /**
             * The HTTP Content Decompressor converts incoming HTTP Content objects,
             * which are compressed according to the Content-Encoding header from the client,
             * into non-compressed HTTP Content objects.
             *
             * Uplink Input: (1) HTTP Request, (0 .. M) HTTP Content, (1) Last HTTP Content.
             * Uplink Output: (1) HTTP Request, (0 .. M) HTTP Content, (1) Last HTTP Content.
             *
             * Downlink Input: Pass-through.
             * Downlink Output: Pass-through.
             */
            channel.pipeline().addLast(new HttpContentDecompressor(strictDecompression));

            /**
             * The HTTP Content Compressor compresses outgoing HTTP Content objects,
             * in accordance with the Accept-Encoding header that was received during the HTTP Request,
             * in order to conserve downlink bandwidth to the client.
             *
             * Uplink Input: Pass-through.
             * Uplink Output: Pass-through.
             *
             * Downlink Input: (1) HTTP Content.
             * Downlink Output: (1) HTTP Content.
             */
            channel.pipeline().addLast(new HttpContentCompressor(compressionLevel, compressionWindowBits, compressionMemoryLevel, compressionThreshold));

            /**
             * The checker applies the user-defined predicate verifications to the incoming HTTP Request object,
             * translated to a partial GPB representation for API consistency,
             * and closes the connection (sometimes without an HTTP response), if any verification fails.
             *
             * Notice that when the client sends a request using the HTTP 1.1 'chunked' encoding,
             * the HTTP Content objects, and Last HTTP Content object, are ignored by the checker,
             * since only the initial HTTP Request object is needed for the verification to occur.
             *
             * Uplink Input: (1) HTTP Request, (0 .. M) HTTP Content, (1) Last HTTP Content.
             * Uplink Output: (1) HTTP Request, (0 .. M) HTTP Content, (1) Last HTTP Content.
             *
             * Downlink Input: Pass-through.
             * Downlink Output: Pass-through.
             */
            channel.pipeline().addLast(new Prechecker(channel.remoteAddress(), channel.localAddress()));

            /**
             * The HTTP Object Aggregator will convert a stream of incoming HTTP Request, HTTP Content,
             * and Last HTTP Content objects into a single Full HTTP Request object.
             * As part of this process, the HTTP Object Aggregator may send zero
             * or more (100 Continue) responses back to the client in order to
             * signal that the client needs to send another chunk.
             *
             * Uplink Input: (1) HTTP Request, (0 .. M) HTTP Content, (1) Last HTTP Content.
             * Uplink Output: (1) Full HTTP Request.
             *
             * Downlink Input: Pass-through.
             * Downlink Output: Pass-through. TODO
             */
            channel.pipeline().addLast(new HttpObjectAggregator(maxRequestSize, closeOnExpectationFailed));

            /**
             * The Slow Uplink Detector detects when the Full HTTP Request is received.
             *
             * If the Slow Uplink Timeout expires before the detector detects that
             * the request has been fully received, then the connection will be closed
             * without sending any HTTP response to the client.
             *
             * This is a defensive mechanism, which is intended to help defend against
             * slow HTTP attacks that maliciously send a request at a very low data-rate.
             *
             * Uplink Input: (1) Netty-based Full HTTP Request.
             * Uplink Output: (1) Netty-based Full HTTP Request.
             *
             * Downlink Input: Pass-through.
             * Downlink Output: Pass-through.
             */
            final SlowUplinkDetector slowUplinkDetector = new SlowUplinkDetector();
            channel.pipeline().addLast(slowUplinkDetector);
            service.schedule(() -> slowUplinkDetector.onSlowUplinkTimeout(channel), SlowUplinkTimeout.toMillis(), TimeUnit.MILLISECONDS);

            /**
             * The Translation Decoder converts an incoming Netty-based
             * Full HTTP Request to an Socius GPB-based HTTP Request.
             *
             * Uplink Input: (1) Netty-based Full HTTP Request.
             * Uplink Output: (1) Socius GPB-based Full HTTP Request.
             *
             * Downlink Input: Pass-through.
             * Downlink Output: Pass-through.
             */
            channel.pipeline().addLast(new TranslationDecoder(channel.remoteAddress(), channel.localAddress()));

            /**
             * The Translation Encoder converts a Socius GPB-based HTTP Response object
             * to an equivalent Netty-based Full HTTP Response object.
             *
             * Uplink Input: Pass-through.
             * Uplink Output: Pass-through.
             *
             * Downlink Input: (1) Socius GPB-based Full HTTP Response.
             * Downlink Output: (1) Netty-based Full HTTP Response.
             */
            channel.pipeline().addLast(new TranslationEncoder());

            /**
             * The Router coordinates the transmission of incoming HTTP requests
             * to the connected (external) actors, and the reception of responses
             * from those connected (external) actors.
             *
             * Uplink Input: (1) Socius GPB-based Full HTTP Request.
             * Uplink Output: None.
             *
             * Downlink Input: None.
             * Downlink Output: (1) Socius GPB-based Full HTTP Response.
             */
            channel.pipeline().addLast(router.newHandler());

            /**
             * If the connection is open for too long, then go ahead and close
             * the connection without sending any HTTP response to the client.
             */
            final Runnable onConnectionTimeout = () ->
            {
                if (channel.isOpen())
                {
                    closeWithNoResponse(channel);
                }
            };

            /**
             * The Connection Timeout is a final fail-safe,
             * which will unconditionally close the connection,
             * if the given timeout is exceeded.
             */
            service.schedule(onConnectionTimeout, connectionTimeout.toMillis(), TimeUnit.MILLISECONDS);

            /**
             * If there are more connections open than the soft-limit allows,
             * then go ahead and close this new connection,
             * but send a meaningful HTTP response to the client.
             *
             * Notice that this check is done after setting up the pipeline.
             * We need the HttpEncoder, etc, to be present in the pipeline,
             * because we are sending an HTTP message that will need to be encoded.
             *
             * Notice that this check is done after the timeouts were scheduled.
             * If the client maliciously reads responses slow, we need to make
             * sure that the timeout is in-place, so that the channel is not held open.
             */
            if (connectionNumber >= softConnectionLimit)
            {
                // TODO: log it
                sendErrorAndClose(channel, HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
                return;
            }
        }

        private void sendErrorAndClose (final Channel channel,
                                        final int status)
        {
            final ServerSideHttpResponse response = Translator.newErrorResponseGPB(status);
            channel.writeAndFlush(response);
            channel.close();
        }

        private void closeWithNoResponse (final Channel channel)
        {
            channel.close();
        }

        private void onClose (final WebLogger logger)
        {
            connectionCount.decrementAndGet();
            logger.onDisconnect();
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
        private WebLogger builderServerLogger = WebLoggers.newNullLogger();

        private String builderServerName = DEFAULT_SERVER_NAME; // Deliberately, set by default.

        private String builderReplyTo = DEFAULT_REPLY_TO; // Deliberately, set by default.

        private String builderBindAddress;

        private Integer builderPort;

        private Integer builderMaxMessagesPerRead;

        private Integer builderRecvAllocatorMin;

        private Integer builderRecvAllocatorInitial;

        private Integer builderRecvAllocatorMax;

        private Integer builderSoftConnectionLimit;

        private Integer builderHardConnectionLimit;

        private Long builderMaxServerUplinkBandwidth;

        private Long builderMaxServerDownlinkBandwidth;

        private Long builderMaxConnectionUplinkBandwidth;

        private Long builderMaxConnectionDownlinkBandwidth;

        private Duration builderMaxPauseTime;

        private Integer builderMaxRequestSize;

        private Integer builderMaxInitialLineSize;

        private Integer builderMaxHeaderSize;

        private Integer builderCompressionLevel;

        private Integer builderCompressionWindowBits;

        private Integer builderCompressionMemoryLevel;

        private Integer builderCompressionThreshold;

        private Duration builderSlowUplinkTimeout;

        private Duration builderSlowDownlinkTimeout;

        private Duration builderResponseTimeout;

        private Duration builderConnectionTimeout;

        private boolean builderShutdownHook = false;

        private Precheck builderPrechecks = Prechecks.forward();

        private Builder ()
        {
            // Pass.
        }

        /**
         * Specify default values for the web-server setup.
         *
         * @return this.
         */
        public Builder withDefaultSettings ()
        {
            this.withServerLogger(WebLoggers.newNullLogger());
            this.withServerName(DEFAULT_SERVER_NAME);
            this.withReplyTo(DEFAULT_REPLY_TO);
            this.withBindAddress(DEFAULT_BIND_ADDRESS);
            this.withPort(DEFAULT_PORT);
            this.withMaxMessagesPerRead(DEFAULT_MAX_MESSAGES_PER_READ);
            this.withRecvBufferAllocator(DEFAULT_RECV_ALLOCATOR_MIN, DEFAULT_RECV_ALLOCATOR_INITIAL, DEFAULT_RECV_ALLOCATOR_MAX);
            this.withSoftConnectionLimit(DEFAULT_SOFT_CONNECTION_LIMIT);
            this.withHardConnectionLimit(DEFAULT_HARD_CONNECTION_LIMIT);
            this.withMaxServerUplinkBandwidth(DEFAULT_SERVER_UPLINK_BANDWIDTH);
            this.withMaxServerDownlinkBandwidth(DEFAULT_SERVER_DOWNLINK_BANDWIDTH);
            this.withMaxConnectionUplinkBandwidth(DEFAULT_CONNECTION_UPLINK_BANDWIDTH);
            this.withMaxConnectionDownlinkBandwidth(DEFAULT_CONNECTION_DOWNLINK_BANDWIDTH);
            this.withMaxPauseTime(DEFAULT_MAX_PAUSE_TIME);
            this.withMaxRequestSize(DEFAULT_MAX_REQUEST_SIZE);
            this.withMaxInitialLineSize(DEFAULT_MAX_INITIAL_LINE_SIZE);
            this.withMaxHeaderSize(DEFAULT_MAX_HEADER_SIZE);
            this.withCompressionLevel(DEFAULT_COMPRESSION_LEVEL);
            this.withCompressionWindowBits(DEFAULT_COMPRESSION_WINDOW_BITS);
            this.withCompressionMemoryLevel(DEFAULT_COMPRESSION_MEMORY_LEVEL);
            this.withCompressionThreshold(DEFAULT_COMPRESSION_THRESHOLD);
            this.withSlowUplinkTimeout(DEFAULT_SLOW_UPLINK_TIMEOUT);
            this.withSlowDownlinkTimeout(DEFAULT_SLOW_DOWNLINK_TIMEOUT);
            this.withResponseTimeout(DEFAULT_RESPONSE_TIMEOUT);
            this.withConnectionTimeout(DEFAULT_CONNECTION_TIMEOUT);

            return this;
        }

        /**
         * Add a shutdown hook that will automatically shutdown the server,
         * when the enclosing application shuts down normally.
         *
         * @return this.
         */
        public Builder withShutdownHook ()
        {
            builderShutdownHook = true;
            return this;
        }

        /**
         * Specify the logger to use.
         *
         * @param logger will be used by the server and extended for each connection.
         * @return this.
         */
        public Builder withServerLogger (final WebLogger logger)
        {
            builderServerLogger = Objects.requireNonNull(logger, "logger");
            return this;
        }

        /**
         * Set the server-name field to embed inside of all requests.
         *
         * <p>
         * The web-server will embed this name in all outgoing requests,
         * which can be useful, when multiple web-servers interface
         * with a set of shared backend application-servers.
         * </p>
         *
         * <p>
         * No restrictions are placed on the server-name.
         * You can set this to anything that you desire.
         * Generally though, different servers should have different names.
         * The server-name should be meaningful to a human, for debugging.
         * </p>
         *
         * <p>
         * If you do not specify a specify a server-name,
         * then default is an empty-string.
         * </p>
         *
         * @param name will be embedded in all requests.
         * @return this.
         */
        public Builder withServerName (final String name)
        {
            builderServerName = Objects.requireNonNull(name, "ServerName");
            return this;
        }

        /**
         * Set the reply-to field to embed inside of all requests.
         *
         * <p>
         * The web-server will embed this address in all outgoing requests,
         * which can be useful, when multiple web-servers interface
         * with a set of shared backend application-servers.
         * </p>
         *
         * <p>
         * No restrictions are placed on the reply-to address.
         * You can set this to anything that you desire.
         * </p>
         *
         * <p>
         * If you do not specify a specify a reply-to address,
         * then default is an empty-string.
         * </p>
         *
         * @param address will be embedded in all requests.
         * @return this.
         */
        public Builder withReplyTo (final String address)
        {
            builderReplyTo = Objects.requireNonNull(address, "ReplyTo");
            return this;
        }

        /**
         * Specify the maximum number of reads from the socket per read loop.
         *
         * @param limit is the maximum number of read() calls per poll.
         * @return this.
         * @see <a href="https://netty.io/4.1/api/io/netty/channel/MaxMessagesRecvByteBufAllocator.html">MaxMessagesRecvByteBufAllocator</a>
         */
        public Builder withMaxMessagesPerRead (final int limit)
        {
            if (limit < 1)
            {
                throw new IllegalArgumentException("MaxMessagesPerRead < 1");
            }
            else
            {
                builderMaxMessagesPerRead = limit;
                return this;
            }
        }

        /**
         * Specify how byte buffers are allocated when reading from the server socket.
         *
         * @param minimum is the inclusive minimum buffer size.
         * @param initial is the initial buffer size before adaptation.
         * @param maximum is the inclusive maximum buffer size.
         * @return this.
         * @see <a href="https://netty.io/4.1/api/io/netty/channel/AdaptiveRecvByteBufAllocator.html">AdaptiveRecvByteBufAllocator</a>
         */
        public Builder withRecvBufferAllocator (final int minimum,
                                                final int initial,
                                                final int maximum)
        {
            if (minimum < 0)
            {
                throw new IllegalArgumentException("RecvByteBufAllocator (minimum) < 0");
            }
            else if (initial < minimum)
            {
                throw new IllegalArgumentException("RecvByteBufAllocator (initial) < (minimum)");
            }
            else if (maximum < initial)
            {
                throw new IllegalArgumentException("RecvByteBufAllocator (maximum) < (initial)");
            }
            else
            {
                builderRecvAllocatorMin = minimum;
                builderRecvAllocatorInitial = initial;
                builderRecvAllocatorMax = maximum;
                return this;
            }
        }

        /**
         * Specify the maximum number of concurrent connections.
         *
         * <p>
         * Once this limit is reached, any additional new connections
         * will be sent an HTTP (503) (Service Unavailable) response,
         * unless the hard connection limit is also reached.
         * </p>
         *
         * <p>
         * This limit is intended to allow graceful degradation
         * of the server in cases where the server is overloaded.
         * </p>
         *
         * <p>
         * <b>Warning:</b> Setting this limit too low can make Denial-of-Service
         * attacks easier for the attacker to implement.
         * </p>
         *
         * @param limit is the maximum number of concurrent connections.
         * @return this.
         */
        public Builder withSoftConnectionLimit (final int limit)
        {
            if (limit < 0)
            {
                throw new IllegalArgumentException("SoftConnectionLimit < 0");
            }
            else
            {
                builderSoftConnectionLimit = limit;
                return this;
            }
        }

        /**
         * Specify the maximum number of concurrent connections.
         *
         * <p>
         * Once this limit is reached, any additional new connections
         * will simply be closed without sending any response.
         * </p>
         *
         * <p>
         * <b>Warning:</b> Setting this limit too low can make Denial-of-Service
         * attacks easier for the attacker to implement.
         * </p>
         *
         * @param limit is the maximum number of concurrent connections.
         * @return this.
         */
        public Builder withHardConnectionLimit (final int limit)
        {
            if (limit < 0)
            {
                throw new IllegalArgumentException("HardConnectionLimit < 0");
            }
            else
            {
                builderHardConnectionLimit = limit;
                return this;
            }
        }

        /**
         * Specify the maximum rate at which data can be sent to the server, overall.
         *
         * <p>
         * This method is used to configure a <code>GlobalChannelTrafficShapingHandler</code>.
         * </p>
         *
         * @param limit is the maximum data-rate in bytes per second.
         * @return this.
         */
        public Builder withMaxServerUplinkBandwidth (final long limit)
        {
            if (limit < 1)
            {
                throw new IllegalArgumentException("MaxServerUplinkBandwidth < 1");
            }
            else
            {
                builderMaxServerUplinkBandwidth = limit;
                return this;
            }
        }

        /**
         * Specify the maximum rate at which data can be sent to the server, per connection.
         *
         * <p>
         * This method is used to configure a <code>GlobalChannelTrafficShapingHandler</code>.
         * </p>
         *
         * @param limit is the maximum data-rate in bytes per second.
         * @return this.
         */
        public Builder withMaxConnectionUplinkBandwidth (final long limit)
        {
            if (limit < 1)
            {
                throw new IllegalArgumentException("MaxConnectionUplinkBandwidth < 1");
            }
            else
            {
                builderMaxConnectionUplinkBandwidth = limit;
                return this;
            }
        }

        /**
         * Specify the maximum rate at which data can be sent from the server, overall.
         *
         * <p>
         * This method is used to configure a <code>GlobalChannelTrafficShapingHandler</code>.
         * </p>
         *
         * @param limit is the maximum data-rate in bytes per second.
         * @return this.
         */
        public Builder withMaxServerDownlinkBandwidth (final long limit)
        {
            if (limit < 1)
            {
                throw new IllegalArgumentException("MaxServerDownlinkBandwidth < 1");
            }
            else
            {
                builderMaxServerDownlinkBandwidth = limit;
                return this;
            }
        }

        /**
         * Specify the maximum rate at which data can be sent from the server, per connection.
         *
         * <p>
         * This method is used to configure a <code>GlobalChannelTrafficShapingHandler</code>.
         * </p>
         *
         * @param limit is the maximum data-rate in bytes per second.
         * @return this.
         */
        public Builder withMaxConnectionDownlinkBandwidth (final long limit)
        {
            if (limit < 1)
            {
                throw new IllegalArgumentException("MaxServerDownlinkBandwidth < 1");
            }
            else
            {
                builderMaxConnectionDownlinkBandwidth = limit;
                return this;
            }
        }

        /**
         * Specify the maximum amount of time to pause excessive traffic.
         *
         * @param delay is the maximum amount of time to pause inbound data.
         * @return this.
         */
        public Builder withMaxPauseTime (final Duration delay)
        {
            builderMaxPauseTime = Objects.requireNonNull(delay, "delay");
            return this;
        }

        /**
         * Specify the maximum allowed length of any HTTP request.
         *
         * <p>
         * For HTTP 1.1 chunked messages, this is the maximum allowed
         * size of the overall combined message, rather than
         * the maximum size of the individual chunks.
         * </p>
         *
         * <p>
         * If this limit is exceeded, then an HTTP (413) (Request Entity Too Large)
         * response will be sent to the client automatically.
         * </p>
         *
         * @param limit will limit the size of incoming messages.
         * @return this.
         */
        public Builder withMaxRequestSize (final int limit)
        {
            if (limit < 0)
            {
                throw new IllegalArgumentException("MaxRequestSize < 0");
            }
            else
            {
                builderMaxRequestSize = limit;
                return this;
            }
        }

        /**
         * Specify the maximum allowed length of the request-line of an HTTP request.
         *
         * <p>
         * If this limit is exceeded, then an HTTP (400) (Bad Request)
         * response will be sent to the client automatically.
         * </p>
         *
         * @param limit will limit the size of the initial line of each HTTP request.
         * @return this.
         */
        public Builder withMaxInitialLineSize (final int limit)
        {
            if (limit < 0)
            {
                throw new IllegalArgumentException("MaxInitialLineSize < 0");
            }
            else
            {
                builderMaxInitialLineSize = limit;
                return this;
            }
        }

        /**
         * Specify the maximum allowed length of the HTTP headers of an HTTP request.
         *
         * <p>
         * If this limit is exceeded, then an HTTP (400) (Bad Request)
         * response will be sent to the client automatically.
         * </p>
         *
         * @param limit will prevent HTTP requests with excessive headers from being accepted.
         * @return this.
         */
        public Builder withMaxHeaderSize (final int limit)
        {
            if (limit < 0)
            {
                throw new IllegalArgumentException("MaxHeaderSize < 0");
            }
            else
            {
                builderMaxHeaderSize = limit;
                return this;
            }
        }

        /**
         * Specify the maximum amount of time that the web-server
         * will wait for a HTTP request to be read off the socket.
         *
         * <p>
         * This timeout is intended to help defend against slow upload attacks.
         * If the client maliciously sends an HTTP request at a slow data-rate,
         * then this timeout will cause the connection to be closed automatically.
         * No response will be sent to the client, as they are deemed malicious.
         * </p>
         *
         * @param timeout will limit how long the server spends reading a request.
         * @return this.
         */
        public Builder withSlowUplinkTimeout (final Duration timeout)
        {
            builderSlowUplinkTimeout = Objects.requireNonNull(timeout, "timeout");
            return this;
        }

        /**
         * Specify the maximum amount of time that the web-server
         * will wait for a HTTP response to be written to the socket.
         *
         * <p>
         * This timeout is intended to help defend against slow download attacks.
         * If the client maliciously reads an HTTP response at a slow data-rate,
         * then this timeout will cause the connection to be closed automatically.
         * </p>
         *
         * @param timeout will limit how long the server spends reading a request.
         * @return this.
         */
        public Builder withSlowDownlinkTimeout (final Duration timeout)
        {
            builderSlowDownlinkTimeout = Objects.requireNonNull(timeout, "timeout");
            return this;

        }

        /**
         * Connections will be closed automatically, if a response exceeds this timeout.
         *
         * <p>
         * If this limit is exceeded, then an HTTP (504) (Gateway Timeout)
         * response will be sent to the client automatically.
         * </p>
         *
         * @param timeout is the maximum amount of time allowed for a response.
         * @return this.
         */
        public Builder withResponseTimeout (final Duration timeout)
        {
            builderResponseTimeout = Objects.requireNonNull(timeout, "timeout");
            return this;
        }

        /**
         * Connections will be closed automatically, if a response exceeds this timeout.
         *
         * <p>
         * If this limit is exceeded, then the connection will
         * be closed without sending any response to the client.
         * </p>
         *
         * @param timeout is the maximum amount of time allowed for a response.
         * @return this.
         */
        public Builder withConnectionTimeout (final Duration timeout)
        {
            builderConnectionTimeout = Objects.requireNonNull(timeout, "timeout");
            return this;
        }

        /**
         * Unconditionally accept any HTTP requests that reaches this predicate.
         *
         * @return this.
         */
        public Builder withPrecheckAccept ()
        {
            return withPrecheck(Prechecks.accept(x -> true));
        }

        /**
         * Apply the given predicate to all incoming HTTP requests.
         * Accept, Reject, or Deny those requests as the predicate deems appropriate.
         *
         * @param check may cause the HTTP request to be accepted, rejected, or denied.
         * @return this.
         */
        public Builder withPrecheck (final Precheck check)
        {
            Objects.requireNonNull(check, "check");
            builderPrechecks = Prechecks.chain(builderPrechecks, check);
            return this;

        }

        /**
         * Specify the host that the server will listen on.
         *
         * @param address is a host-name or IP address.
         * @return this.
         */
        public Builder withBindAddress (final String address)
        {
            this.builderBindAddress = Objects.requireNonNull(address, "address");
            return this;
        }

        /**
         * Specify the port that the server will listen on.
         *
         * @param value is the port to use.
         * @return this.
         */
        public Builder withPort (final int value)
        {
            this.builderPort = value;
            return this;
        }

        /**
         * Specify the compression-level applied to the downlink,
         * when the Accept-Encoding header allows compression.
         *
         * <p>
         * See Also: <a href="https://netty.io/4.1/api/io/netty/handler/codec/http/HttpContentCompressor.html">HttpContentCompressor</a>
         * </p>
         *
         * @param level is a value between zero (no compression) and nine (max compression).
         * @return this.
         */
        public Builder withCompressionLevel (final int level)
        {
            this.builderCompressionLevel = level;
            return this;
        }

        /**
         * Specify the window-size setting used during downlink compression.
         *
         * <p>
         * See Also: <a href="https://netty.io/4.1/api/io/netty/handler/codec/http/HttpContentCompressor.html">HttpContentCompressor</a>
         * </p>
         *
         * @param count should be a value between nine (worse compression, less memory) and fifteen (better compression, more memory).
         * @return this.
         */
        public Builder withCompressionWindowBits (final int count)
        {
            this.builderCompressionWindowBits = count;
            return this;
        }

        /**
         * Specify the memory-level setting used during downlink compression.
         *
         * <p>
         * See Also: <a href="https://netty.io/4.1/api/io/netty/handler/codec/http/HttpContentCompressor.html">HttpContentCompressor</a>
         * </p>
         *
         * @param level should be a value between one (worse compression, less memory) and nine (better compression, more memory).
         * @return this.
         */
        public Builder withCompressionMemoryLevel (final int level)
        {
            this.builderCompressionMemoryLevel = level;
            return this;
        }

        /**
         * Specify the memory-level setting used during downlink compression.
         *
         * <p>
         * See Also: <a href="https://netty.io/4.1/api/io/netty/handler/codec/http/HttpContentCompressor.html">HttpContentCompressor</a>
         * </p>
         *
         * @param size is the minimum size a response must be in order for compression to be applied.
         * @return this.
         */
        public Builder withCompressionThreshold (final int size)
        {
            this.builderCompressionThreshold = size;
            return this;
        }

        /**
         * Construct the web-server and start it up.
         *
         * @return the new web-server.
         */
        public WebServer build ()
        {
            requireSetting("Server Logger", builderServerLogger);
            requireSetting("Server Name", builderServerName);
            requireSetting("Reply To", builderReplyTo);
            requireSetting("Bind Address", builderBindAddress);
            requireSetting("Port", builderPort);
            requireSetting("Max Messages Per Read", builderMaxMessagesPerRead);
            requireSetting("Recv Allocator Min", builderRecvAllocatorMin);
            requireSetting("Recv Allocator Max", builderRecvAllocatorMax);
            requireSetting("Recv Allocator Initial", builderRecvAllocatorInitial);
            requireSetting("Soft Connection Limit", builderSoftConnectionLimit);
            requireSetting("Hard Connection Limit", builderHardConnectionLimit);
            requireSetting("Max Server Uplink Bandwidth", builderMaxServerUplinkBandwidth);
            requireSetting("Max Server Downlink Bandwidth", builderMaxServerDownlinkBandwidth);
            requireSetting("Max Connection Uplink Bandwidth", builderMaxConnectionUplinkBandwidth);
            requireSetting("Max Connection Downlink Bandwidth", builderMaxConnectionDownlinkBandwidth);
            requireSetting("Max Pause Time", builderMaxPauseTime);
            requireSetting("Max Request Size", builderMaxRequestSize);
            requireSetting("Max Initial Line Size", builderMaxInitialLineSize);
            requireSetting("Max Header Size", builderMaxHeaderSize);
            requireSetting("Compression Level", builderCompressionLevel);
            requireSetting("Compression Window Bits", builderCompressionWindowBits);
            requireSetting("Compression Memory Level", builderCompressionMemoryLevel);
            requireSetting("Compression Threshold", builderCompressionThreshold);
            requireSetting("Slow Uplink Timeout", builderSlowUplinkTimeout);
            requireSetting("Slow Downlink Timeout", builderSlowDownlinkTimeout);
            requireSetting("Response Timeout", builderResponseTimeout);
            requireSetting("Connection Timeout", builderConnectionTimeout);

            final WebServer server = new WebServer(this);
            return server;
        }
    }

    private static void requireSetting (final String name,
                                        final Object value)
    {
        if (value == null)
        {
            throw new IllegalStateException("Required: " + name);
        }
    }
}
