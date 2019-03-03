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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import com.mackenziehigh.socius.web.messages.web_m;
import com.mackenziehigh.socius.web.messages.web_m.HttpRequest;
import com.mackenziehigh.socius.web.messages.web_m.HttpResponse;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.AdaptiveRecvByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.traffic.GlobalChannelTrafficShapingHandler;
import java.io.Closeable;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

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

    private final ServerLogger serverLogger;

    private final ConnectionLoggerFactory connectionLogger;

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

    private final ImmutableList<Precheck> prechecks;

    private final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();

    private final Stage stage = Cascade.newExecutorStage(service).addErrorHandler(ex -> serverLogger().onException(ex));

    private final AtomicLong connectionCount = new AtomicLong();

    private final AdaptiveRecvByteBufAllocator recvBufferAllocator;

    private final GlobalChannelTrafficShapingHandler trafficShapingHandler;

    /**
     * TODO: Make the sequence-numbers increment monotonically.
     */
    private final Translator translator;

    private final Router router = new Router(stage);

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
     * Sole Constructor.
     *
     * @param builder contains the initial server settings.
     */
    private WebServer (final Builder builder)
    {
        this.serverLogger = builder.builderServerLogger;
        this.connectionLogger = builder.builderConnectionLogger;
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
        this.prechecks = ImmutableList.copyOf(new CopyOnWriteArrayList<>(builder.builderPrechecks));

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
    }

    private ServerLogger serverLogger ()
    {
        return serverLogger;
    }

    /**
     * Get the current state of the sequence-number generator.
     *
     * @return the current sequence-number.
     */
    public long getSequenceCount ()
    {
        // TODO
        return 0;
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
    public Output<HttpRequest> requestsOut ()
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
    public Input<HttpResponse> responsesIn ()
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
            bootstrap.childHandler(new Initializer());
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
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            service.shutdown();
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
     * The logic herein is executed whenever a new connection is established!
     * </p>
     */
    private final class Initializer
            extends ChannelInitializer<SocketChannel>
    {
        // TODO: What if the factory throws an exception?
        private final ConnectionLogger channelLogger = ConnectionLoggers.newSafeLogger(connectionLogger.newConnectionLogger());

        @Override
        protected void initChannel (final SocketChannel channel)
        {
            channelLogger.onConnect();

            channel.closeFuture().addListener(x -> onClose());

            if (connectionCount.incrementAndGet() >= hardConnectionLimit)
            {
                channel.close();
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

            HardTimeout.enforce(service, channel, connectionTimeout);

            channel.pipeline().addLast(trafficShapingHandler);
            channel.pipeline().addLast(new HttpResponseEncoder());
            channel.pipeline().addLast(new HttpRequestDecoder(maxInitialLineSize,
                                                              maxHeaderSize,
                                                              maxChunkSize,
                                                              validateHeaders));
            channel.pipeline().addLast(new Prechecker(translator, prechecks));
            channel.pipeline().addLast(new HttpObjectAggregator(maxRequestSize, closeOnExpectationFailed));
//            channel.pipeline().addLast(SlowUplinkTimeout.newHandler(channel, service, SlowUplinkTimeout));
            channel.pipeline().addLast(new TranslationEncoder(translator));
            channel.pipeline().addLast(new TranslationDecoder(translator));
            channel.pipeline().addLast(router.newHandler());

            /**
             * If a response is not sent back to the client within the given time limit,
             * then close the connection upon sending an appropriate default response.
             * Technically, this timer task will always execute; however,
             * the task is harmless, if the response was already sent.
             */
            service.schedule(WebServer.this::closeStaleConnections, responseTimeout.toMillis(), TimeUnit.MILLISECONDS);
        }

        private void onClose ()
        {
            connectionCount.decrementAndGet();
            channelLogger.onDisconnect();
        }
    }

    private void closeStaleConnections ()
    {
        try
        {
            router.closeStaleConnections(Instant.MIN);
        }
        catch (Throwable ex)
        {
            serverLogger.onException(ex);
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
        private ServerLogger builderServerLogger = ServerLoggers.newNullLogger();

        private ConnectionLoggerFactory builderConnectionLogger = () -> ConnectionLoggers.newNullLogger();

        private String builderServerName = "";

        private String builderReplyTo = "";

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

        private final List<Precheck> builderPrechecks = new LinkedList<>();

        private final Map<Integer, HttpResponse> defaultResponses = Maps.newTreeMap();

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
            this.withServerLogger(ServerLoggers.newNullLogger());
            this.withConnectionLogger(() -> ConnectionLoggers.newNullLogger());
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
         * Specify a default response to use for error messages with a given status code.
         *
         * <p>
         * The response specified here will not override responses generated upstream.
         * </p>
         *
         * @param statusCode is the status code of an error message.
         * @param response will be used as the response for the given status code.
         * @return this.
         */
        public Builder withDefaultResponse (final int statusCode,
                                            final HttpResponse response)
        {
            Objects.requireNonNull(response, "response");
            defaultResponses.put(statusCode, response);
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
         * Specify the connection-specific logger to use.
         *
         * @param logger will create a logger for each connection.
         * @return this.
         */
        public Builder withServerLogger (final ServerLogger logger)
        {
            builderServerLogger = Objects.requireNonNull(logger, "logger");
            return this;
        }

        /**
         * Specify the server-wide logger to use.
         *
         * @param logger will be used by the web-server.
         * @return this.
         */
        public Builder withConnectionLogger (final ConnectionLoggerFactory logger)
        {
            builderConnectionLogger = Objects.requireNonNull(logger, "logger");
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
            builderSlowDownlinkTimeout = Objects.requireNonNull(timeout, "timeout");;
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
         * Unconditionally accept any HTTP requests that reach this predicate.
         *
         * <p>
         * Equivalent: <code>withPredicateAccept(x -> true)</code>
         * </p>
         *
         * @return this.
         */
        public Builder withPredicateAccept ()
        {
            return withPredicateAccept(x -> true);
        }

        /**
         * Conditionally accept HTTP requests that reach this predicate,
         * forwarding any non-matching requests to the next predicate in the rule chain.
         *
         * @param condition may cause the HTTP request to be accepted.
         * @return this.
         */
        public Builder withPredicateAccept (final Predicate<web_m.HttpRequest> condition)
        {
            Objects.requireNonNull(condition, "condition");
            final Precheck check = msg -> condition.test(msg) ? Precheck.Result.ACCEPT : Precheck.Result.FORWARD;
            builderPrechecks.add(check);
            return this;

        }

        /**
         * Unconditionally reject any HTTP requests that reach this predicate.
         *
         * <p>
         * Equivalent: <code>withPredicateReject(status, x -> true)</code>
         * </p>
         *
         * @param status is the HTTP status code to use upon rejection.
         * @return this.
         */
        public Builder withPredicateReject (final int status)
        {
            return withPredicateReject(status, x -> true);
        }

        /**
         * Conditionally reject HTTP requests that reach this predicate,
         * forwarding any non-matching requests to the next predicate in the rule chain.
         *
         * <p>
         * If this predicate matches, then an HTTP response with the given
         * status code will be sent to the client automatically.
         * </p>
         *
         * @param status is the HTTP status code to use upon rejection.
         * @param condition may cause the HTTP request to be rejected.
         * @return this.
         */
        public Builder withPredicateReject (final int status,
                                            final Predicate<web_m.HttpRequest> condition)
        {
            Objects.requireNonNull(condition, "condition");
            final Precheck check = msg -> condition.test(msg) ? Precheck.Result.REJECT : Precheck.Result.FORWARD;
            builderPrechecks.add(check);
            return this;

        }

        /**
         * Unconditionally reject any HTTP requests that reach this predicate.
         *
         * <p>
         * Equivalent: <code>withPredicateDeny(x -> true)</code>
         * </p>
         *
         * @return this.
         */
        public Builder withPredicateDeny ()
        {
            return withPredicateDeny(x -> true);
        }

        /**
         * Conditionally reject HTTP requests that reach this predicate,
         * forwarding any non-matching requests to the next predicate in the rule chain.
         *
         * <p>
         * If this predicate matches, then the connection will be closed
         * without sending any HTTP response to the client.
         * </p>
         *
         * @param condition may cause the HTTP request to be rejected.
         * @return this.
         */
        public Builder withPredicateDeny (final Predicate<web_m.HttpRequest> condition)
        {
            Objects.requireNonNull(condition, "condition");
            final Precheck check = msg -> condition.test(msg) ? Precheck.Result.DENY : Precheck.Result.FORWARD;
            builderPrechecks.add(check);
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
            requireSetting("Connection Logger", builderConnectionLogger);
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
