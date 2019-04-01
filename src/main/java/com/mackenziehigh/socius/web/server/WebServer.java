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
import com.mackenziehigh.socius.web.server.filters.RequestFilter;
import com.mackenziehigh.socius.web.server.filters.RequestFilters;
import com.mackenziehigh.socius.web.server.loggers.DefaultWebLogger;
import com.mackenziehigh.socius.web.server.loggers.SafeWebLogger;
import com.mackenziehigh.socius.web.server.loggers.WebLogger;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.AdaptiveRecvByteBufAllocator;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
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
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.ssl.SslHandler;
import java.io.Closeable;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import javax.net.ssl.SSLEngine;

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

    private final Duration maxPauseTime;

    private final int maxRequestSize;

    private final int maxInitialLineSize;

    private final int maxHeaderSize;

    private final int compressionLevel;

    private final int compressionWindowBits;

    private final int compressionMemoryLevel;

    private final int compressionThreshold;

    private final Duration uplinkTimeout;

    private final Duration downlinkTimeout;

    private final Duration responseTimeout;

    private final boolean hasShutdownHook;

    private final RequestFilter prechecks;

    private final EventLoopGroup bossGroup = new NioEventLoopGroup(BOSS_THREAD_COUNT);

    private final EventLoopGroup workerGroup = new NioEventLoopGroup(WORKER_THREAD_COUNT);

    private final Stage stage = Cascade.newExecutorStage(workerGroup).addErrorHandler(ex -> serverLogger().onException(ex));

    private final AtomicInteger connectionCount = new AtomicInteger();

    private final AdaptiveRecvByteBufAllocator recvBufferAllocator;

    private final boolean secureSocketsEnabled;

    private final Supplier<SSLEngine> secureSocketsEngine;

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
        this.maxPauseTime = builder.builderMaxPauseTime;
        this.maxRequestSize = builder.builderMaxRequestSize;
        this.maxInitialLineSize = builder.builderMaxInitialLineSize;
        this.maxHeaderSize = builder.builderMaxHeaderSize;
        this.compressionLevel = builder.builderCompressionLevel;
        this.compressionWindowBits = builder.builderCompressionWindowBits;
        this.compressionMemoryLevel = builder.builderCompressionMemoryLevel;
        this.compressionThreshold = builder.builderCompressionThreshold;
        this.downlinkTimeout = builder.builderDownlinkTimeout;
        this.uplinkTimeout = builder.builderUplinkTimeout;
        this.responseTimeout = builder.builderResponseTimeout;
        this.hasShutdownHook = builder.builderShutdownHook;
        this.prechecks = RequestFilters.chain(builder.builderPrechecks, RequestFilters.deny());

        this.recvBufferAllocator = new AdaptiveRecvByteBufAllocator(recvAllocatorMin, recvAllocatorInitial, recvAllocatorMax);
        this.recvBufferAllocator.maxMessagesPerRead(maxMessagesPerRead);

        this.secureSocketsEnabled = builder.builderSecureSocketsEnabled;
        this.secureSocketsEngine = builder.builderSecureSocketsEngine;

        this.translator = new Translator(serverName, serverId, replyTo);
        this.router = new Correlator(stage);
        this.initializer = new Initializer();
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
    public Duration getUplinkTimeout ()
    {
        return uplinkTimeout;
    }

    /**
     * Get the maximum amount of time the server will wait
     * for a response to be written to the socket.
     *
     * @return the read timeout.
     */
    public Duration getDownlinkTimeout ()
    {
        return downlinkTimeout;
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
     * Determine whether SSL/TLS will be used for each connection.
     *
     * @return true, if SSL/TLS is enabled.
     */
    public boolean isSecure ()
    {
        return secureSocketsEnabled;
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
            serverLogger.onStarted(this);
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
            bossGroup.shutdownGracefully().syncUninterruptibly();
            workerGroup.shutdownGracefully().syncUninterruptibly();
            stage.close();
            // Delayed tasks (timeout tasks) can delay shutdown.
            // Therefore, we must use shutdownNow().
            workerGroup.shutdownNow();
            serverLogger.onStopped();
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
        @Override
        protected void initChannel (final SocketChannel channel)
        {
            final Connection connection = new Connection(channel);
            connection.init();
        }
    }

    /**
     * A connection between a single client and the server,
     * which will handle exactly one request/response pair.
     */
    private final class Connection
    {
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
        private final int maxChunkSize = maxRequestSize;

        /**
         * If the client attempts to upload a chunked request
         * that is larger than we are willing to accept,
         * then automatically close the connection with
         * an HTTP (417) (Expectation Failed) response.
         */
        private final boolean closeOnExpectationFailed = true;

        private final boolean strictDecompression = false;

        private final boolean validateHeaders = true;

        /**
         * This logger object is unique to this connection.
         */
        private final WebLogger connectionLogger = serverLogger.extend();

        /**
         * This string uniquely identifies the request/response pair.
         * Since only a single request is performed per connection,
         * this string also uniquely identifies the connection.
         */
        private final String correlationId = UUID.randomUUID().toString();

        /**
         * Netty-based representation of the connection.
         */
        private final SocketChannel channel;

        /**
         * This object converts incoming bytes to Netty-based multi-object HTTP requests.
         */
        private final HttpRequestDecoder httpDecoder = new HttpRequestDecoder(maxInitialLineSize,
                                                                              maxHeaderSize,
                                                                              maxChunkSize,
                                                                              validateHeaders);

        /**
         * This object converts outgoing Netty-based multi-object HTTP responses to bytes.
         */
        private final HttpResponseEncoder httpEncoder = new HttpResponseEncoder();

        /**
         * This object decompresses incoming HTTP requests.
         */
        private final HttpContentDecompressor decompressor = new HttpContentDecompressor(strictDecompression);

        /**
         * This object compresses outgoing HTTP responses.
         */
        private final HttpContentCompressor compressor = new HttpContentCompressor(compressionLevel, compressionWindowBits, compressionMemoryLevel, compressionThreshold);

        /**
         * This object combines multi-object HTTP requests into single object full HTTP requests.
         */
        private final HttpObjectAggregator aggregator = new HttpObjectAggregator(maxRequestSize, closeOnExpectationFailed);

        public Connection (final SocketChannel channel)
        {
            this.channel = channel;
        }

        /**
         * This object enforces the configured timeouts for the connection.
         *
         * <p>
         * These timeouts are critical from a security and robustness perspective.
         * Malicious clients may attempt to use slow writes or slow reads
         * in order to exhaust the limited resources of the server.
         * Likewise, if no response is generated in a timely manner,
         * then the client will be sent a default response.
         * </p>
         */
        private final TimeoutMachine timeoutTimer = new TimeoutMachine(workerGroup, uplinkTimeout, responseTimeout, uplinkTimeout)
        {
            @Override
            public void onReadTimeout ()
            {
                connectionLogger.onUplinkTimeout();
                closeWithNoResponse();
            }

            @Override
            public void onResponseTimeout ()
            {
                connectionLogger.onResponseTimeout();
                sendErrorAndClose(HttpResponseStatus.REQUEST_TIMEOUT.code());
            }

            @Override
            public void onWriteTimeout ()
            {
                connectionLogger.onDownlinkTimeout();
                closeWithNoResponse();
            }
        };

        /**
         * This object detects when the HTTP request has been fully read-in.
         */
        private final MessageToMessageDecoder<FullHttpRequest> requestDetector = new MessageToMessageDecoder<FullHttpRequest>()
        {
            @Override
            protected void decode (final ChannelHandlerContext ctx,
                                   final FullHttpRequest msg,
                                   final List<Object> out)
            {
                /**
                 * Prevent the client from maliciously sending more bytes
                 * after the end of the HTTP request, which could be exploited
                 * to waste CPU and bandwidth resources on the server.
                 */
                channel.shutdownInput(); // TODO: Blocks further reads???

                /**
                 * The Response Timeout needs to be triggered to start.
                 */
                timeoutTimer.reactTo(TimeoutMachine.Events.READ_COMPLETE);

                /**
                 * Send the message through the rest of the pipeline.
                 */
                msg.retain();
                out.add(msg);
            }
        };

        /**
         * This object detects when the final HTTP response has begun being written-out.
         *
         * <p>
         * This object ensures that only one *final* response is sent to the client.
         * Note, however, that several 100-Continue responses may be sent,
         * as part of the reception of the request.
         * </p>
         */
        private final MessageToMessageEncoder<FullHttpResponse> responseDetector = new MessageToMessageEncoder<FullHttpResponse>()
        {
            private final AtomicBoolean replied = new AtomicBoolean();

            @Override
            protected void encode (final ChannelHandlerContext ctx,
                                   final FullHttpResponse msg,
                                   final List<Object> out)
            {
                if (replied.compareAndSet(false, true))
                {
                    timeoutTimer.reactTo(TimeoutMachine.Events.RESPONSE_COMPLETE);
                    out.add(msg);
                }
            }
        };

        /**
         * This object applies the request-filters to the incoming HTTP request.
         */
        private final MessageToMessageDecoder<HttpRequest> prechecker = new MessageToMessageDecoder<HttpRequest>()
        {

            @Override
            protected void decode (final ChannelHandlerContext ctx,
                                   final HttpRequest msg,
                                   final List<Object> out)
            {
                /**
                 * Convert the Netty-based request to a GPB-based request.
                 * Some parts of the request will be omitted,
                 * as they are not needed at this stage.
                 */
                final ServerSideHttpRequest prefix;

                try
                {
                    prefix = translator.prefixOf(correlationId, channel.remoteAddress(), channel.localAddress(), msg);
                }
                catch (Throwable ex)
                {
                    connectionLogger.onException(ex);
                    return;
                }

                try
                {

                    final RequestFilter.Action decision = prechecks.apply(prefix);

                    if (decision.action() == RequestFilter.ActionType.ACCEPT)
                    {
                        connectionLogger.onAccepted(prefix);
                        out.add(msg);
                    }
                    else if (decision.action() == RequestFilter.ActionType.REJECT)
                    {
                        final int statusCode = decision.status().orElse(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
                        connectionLogger.onRejected(prefix, statusCode);
                        sendErrorAndClose(statusCode);
                    }
                    else // DENY, FORWARD never occurs.
                    {
                        closeWithNoResponse();
                        connectionLogger.onDenied(prefix);
                    }
                }
                catch (Throwable ex)
                {
                    connectionLogger.onException(ex);
                    connectionLogger.onDenied(prefix);
                    closeWithNoResponse();
                }
            }
        };

        /**
         * This object translates Netty-based HTTP requests to GPB-based HTTP requests.
         */
        private final MessageToMessageDecoder<FullHttpRequest> protoDecoder = new MessageToMessageDecoder<FullHttpRequest>()
        {

            @Override
            protected void decode (final ChannelHandlerContext ctx,
                                   final FullHttpRequest msg,
                                   final List<Object> out)
            {
                try
                {
                    final ServerSideHttpRequest request = translator.requestToGPB(correlationId, channel.remoteAddress(), channel.localAddress(), msg);
                    out.add(request);
                }
                catch (Throwable ex)
                {
                    connectionLogger.onException(ex);
                    sendErrorAndClose(HttpResponseStatus.BAD_REQUEST.code());
                }
            }
        };

        /**
         * This object translates GPB-based HTTP responses to Netty-based HTTP responses.
         */
        private final MessageToMessageEncoder<ServerSideHttpResponse> protoEncoder = new MessageToMessageEncoder<ServerSideHttpResponse>()
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
                    connectionLogger.onException(ex);
                    sendErrorAndClose(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
                }
            }
        };

        /**
         * This object routes incoming responses to the external actors
         * and then routes the responses from those actors.
         */
        private final SimpleChannelInboundHandler<ServerSideHttpRequest> correlator = new SimpleChannelInboundHandler<ServerSideHttpRequest>()
        {
            @Override
            protected void channelRead0 (final ChannelHandlerContext ctx,
                                         final ServerSideHttpRequest msg)
            {
                /**
                 * Log that the request will not be out for processing by the actors.
                 */
                connectionLogger.onRequest(msg);

                /**
                 * Send the request out of the server for processing.
                 */
                router.dispatch(correlationId, msg, response -> onReply(ctx, response));
            }

            private void onReply (final ChannelHandlerContext ctx,
                                  final ServerSideHttpResponse response)
            {
                /**
                 * Log that the response has been received from the external actors.
                 */
                connectionLogger.onResponse(response);

                /**
                 * Send the response to the client by passing it all the way through the pipeline.
                 * If a response was already sent to the client, then this will have no real effect,
                 * as this response will be deliberately and silently dropped.
                 */
                channel.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            }
        };

        /**
         * This object logs any unhandled exceptions that occur in the pipeline.
         */
        private final ChannelDuplexHandler exceptionHandler = new ChannelDuplexHandler()
        {
            @Override
            public void exceptionCaught (final ChannelHandlerContext ctx,
                                         final Throwable cause)
            {
                connectionLogger.onException(cause);
                sendErrorAndClose(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
            }
        };

        /**
         * Setup the Netty pipeline.
         */
        public void init ()
        {
            /**
             * The logger, etc, will need to be notified when the connection closes.
             */
            channel.closeFuture().addListener(x -> onClose());

            /**
             * Keep track of how many connections are open.
             */
            final int connectionNumber = connectionCount.incrementAndGet();

            /**
             * Configure the connection-specific logger.
             */
            connectionLogger.onConnect(channel.localAddress(), channel.remoteAddress());

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
                connectionLogger.onTooManyConnections(connectionNumber);
                closeWithNoResponse();
                return;
            }

            /**
             * Start the time that will detect slow reads, responses, and writes.
             */
            timeoutTimer.reactTo(TimeoutMachine.Events.INIT);

            /**
             * If SSL/TLS is enabled, then add an SSL Handler,
             * which will perform all of the encryption and decryption.
             */
            if (secureSocketsEnabled)
            {
                final SSLEngine engine = secureSocketsEngine.get();
                channel.pipeline().addLast(new SslHandler(engine));
            }

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
            channel.pipeline().addLast(httpDecoder);

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
            channel.pipeline().addLast(httpEncoder);

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
            channel.pipeline().addLast(decompressor);

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
            channel.pipeline().addLast(compressor);

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
            channel.pipeline().addLast(prechecker);

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
             * Downlink Output: Pass-through.
             */
            channel.pipeline().addLast(aggregator);

            /**
             * The Request Detector will detect when the request has been fully read-in.
             *
             * At that point, the client will be prevented from sending more bytes,
             * since that would be a potential attack vector (CPU usage exploit).
             *
             * The uplink-timeout will be stopped, since the read is now complete.
             * The response-timeout will be started, since the response is in-progress.
             *
             * Uplink Input: (1) Full HTTP Request.
             * Uplink Output: (1) Full HTTP Request.
             *
             * Downlink Input: Pass-through.
             * Downlink Output: Pass-through.
             */
            channel.pipeline().addLast(requestDetector);

            /**
             * The Response Detector will detect when the response has begun being written-out.
             *
             * Uplink Input: Pass-Through.
             * Uplink Output: Pass-Through.
             *
             * Downlink Input: (1) Full HTTP Response.
             * Downlink Output: (1) Full HTTP Response.
             */
            channel.pipeline().addLast(responseDetector);

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
            channel.pipeline().addLast(protoDecoder);

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
            channel.pipeline().addLast(protoEncoder);

            /**
             * The Correlator coordinates the transmission of incoming HTTP requests
             * to the connected (external) actors, and the reception of responses
             * from those connected (external) actors.
             *
             * Uplink Input: (1) Socius GPB-based Full HTTP Request.
             * Uplink Output: None.
             *
             * Downlink Input: None.
             * Downlink Output: (1) Socius GPB-based Full HTTP Response.
             */
            channel.pipeline().addLast(correlator);

            /**
             * This handler will catch any unhandled exceptions from other handlers.
             */
            channel.pipeline().addLast(exceptionHandler);

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
                // TODO: log it, right code?
                connectionLogger.onTooManyConnections(connectionNumber);
                sendErrorAndClose(HttpResponseStatus.TOO_MANY_REQUESTS.code());
                return;
            }
        }

        /**
         * Send a default error-response to the client and then close the connection.
         *
         * @param status is an HTTP status code.
         */
        private void sendErrorAndClose (final int status)
        {
            final ServerSideHttpResponse response = Translator.newErrorResponseGPB(status);
            channel.writeAndFlush(response);
            channel.close();
        }

        /**
         * Close the connection immediately without sending an HTTP response to the client.
         */
        private void closeWithNoResponse ()
        {
            channel.close();
        }

        /**
         * This method will be invoked when Netty closes the connection.
         */
        private void onClose ()
        {
            connectionCount.decrementAndGet();

            /**
             * If a response arrives late, just ignore it.
             */
            router.disregard(correlationId);

            /**
             * Stop the timeout timer, since the connection is now closed.
             */
            timeoutTimer.reactTo(TimeoutMachine.Events.WRITE_COMPLETE);

            connectionLogger.onDisconnect(channel.localAddress(), channel.remoteAddress());
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
        private WebLogger builderServerLogger = new DefaultWebLogger();

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

        private Duration builderMaxPauseTime;

        private Integer builderMaxRequestSize;

        private Integer builderMaxInitialLineSize;

        private Integer builderMaxHeaderSize;

        private Integer builderCompressionLevel;

        private Integer builderCompressionWindowBits;

        private Integer builderCompressionMemoryLevel;

        private Integer builderCompressionThreshold;

        private Duration builderUplinkTimeout;

        private Duration builderDownlinkTimeout;

        private Duration builderResponseTimeout;

        private boolean builderShutdownHook = false;

        private Boolean builderSecureSocketsEnabled;

        private Supplier<SSLEngine> builderSecureSocketsEngine;

        private RequestFilter builderPrechecks = RequestFilters.forward();

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
            this.withLogger(new DefaultWebLogger());
            this.withServerName(DEFAULT_SERVER_NAME);
            this.withReplyTo(DEFAULT_REPLY_TO);
            this.withBindAddress(DEFAULT_BIND_ADDRESS);
            this.withPort(DEFAULT_PORT);
            this.withMaxMessagesPerRead(DEFAULT_MAX_MESSAGES_PER_READ);
            this.withRecvBufferAllocator(DEFAULT_RECV_ALLOCATOR_MIN, DEFAULT_RECV_ALLOCATOR_INITIAL, DEFAULT_RECV_ALLOCATOR_MAX);
            this.withSoftConnectionLimit(DEFAULT_SOFT_CONNECTION_LIMIT);
            this.withHardConnectionLimit(DEFAULT_HARD_CONNECTION_LIMIT);
            this.withMaxPauseTime(DEFAULT_MAX_PAUSE_TIME);
            this.withMaxRequestSize(DEFAULT_MAX_REQUEST_SIZE);
            this.withMaxInitialLineSize(DEFAULT_MAX_INITIAL_LINE_SIZE);
            this.withMaxHeaderSize(DEFAULT_MAX_HEADER_SIZE);
            this.withCompressionLevel(DEFAULT_COMPRESSION_LEVEL);
            this.withCompressionWindowBits(DEFAULT_COMPRESSION_WINDOW_BITS);
            this.withCompressionMemoryLevel(DEFAULT_COMPRESSION_MEMORY_LEVEL);
            this.withCompressionThreshold(DEFAULT_COMPRESSION_THRESHOLD);
            this.withUplinkTimeout(DEFAULT_SLOW_UPLINK_TIMEOUT);
            this.withDownlinkTimeout(DEFAULT_SLOW_DOWNLINK_TIMEOUT);
            this.withResponseTimeout(DEFAULT_RESPONSE_TIMEOUT);
            this.withUnsecureSockets();

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
        public Builder withLogger (final WebLogger logger)
        {
            builderServerLogger = Objects.requireNonNull(logger, "logger");
            builderServerLogger = new SafeWebLogger(logger);
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
        public Builder withUplinkTimeout (final Duration timeout)
        {
            builderUplinkTimeout = Objects.requireNonNull(timeout, "timeout");
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
        public Builder withDownlinkTimeout (final Duration timeout)
        {
            builderDownlinkTimeout = Objects.requireNonNull(timeout, "timeout");
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
         * Unconditionally accept any HTTP requests that reaches this predicate.
         *
         * @return this.
         */
        public Builder withAcceptFilter ()
        {
            return withRequestFilter(RequestFilters.accept(x -> true));
        }

        /**
         * Apply the given predicate to all incoming HTTP requests.
         * Accept, Reject, or Deny those requests as the predicate deems appropriate.
         *
         * @param check may cause the HTTP request to be accepted, rejected, or denied.
         * @return this.
         */
        public Builder withRequestFilter (final RequestFilter check)
        {
            Objects.requireNonNull(check, "check");
            builderPrechecks = RequestFilters.chain(builderPrechecks, check);
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
         * Specify that SSL/TLS will be used for all connections.
         *
         * @param supplier will create an engine for each new socket.
         * @return this.
         */
        public Builder withSecureSockets (final Supplier<SSLEngine> supplier)
        {
            this.builderSecureSocketsEnabled = true;
            this.builderSecureSocketsEngine = Objects.requireNonNull(supplier, "supplier");
            return this;
        }

        /**
         * Specify that SSL/TLS will <b>not</b> be used for any connections.
         *
         * @return this.
         */
        public Builder withUnsecureSockets ()
        {
            this.builderSecureSocketsEnabled = false;
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
            requireSetting("Max Pause Time", builderMaxPauseTime);
            requireSetting("Max Request Size", builderMaxRequestSize);
            requireSetting("Max Initial Line Size", builderMaxInitialLineSize);
            requireSetting("Max Header Size", builderMaxHeaderSize);
            requireSetting("Compression Level", builderCompressionLevel);
            requireSetting("Compression Window Bits", builderCompressionWindowBits);
            requireSetting("Compression Memory Level", builderCompressionMemoryLevel);
            requireSetting("Compression Threshold", builderCompressionThreshold);
            requireSetting("Slow Uplink Timeout", builderUplinkTimeout);
            requireSetting("Slow Downlink Timeout", builderDownlinkTimeout);
            requireSetting("Response Timeout", builderResponseTimeout);
            requireSetting("SSL/TLS On|Off", builderSecureSocketsEnabled);

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
