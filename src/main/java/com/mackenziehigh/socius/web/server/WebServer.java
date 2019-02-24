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
import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

/**
 * A non-blocking HTTP server based on the Netty framework,
 * for creating RESTful APIs using the Cascade framework.
 */
public final class WebServer
        implements Closeable
{
    private static final int BOSS_THREAD_COUNT = 1;

    private static final int WORKER_THREAD_COUNT = 1;

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

    private final int maxRequestSize;

    private final int maxInitialLineSize;

    private final int maxHeaderSize;

    private final Duration slowReadTimeout;

    private final Duration responseTimeout;

    private final Duration connectionTimeout;

    private final ImmutableList<Precheck> prechecks;

    private final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();

    private final Stage stage = Cascade.newExecutorStage(service);

    private final AdaptiveRecvByteBufAllocator recvBufferAllocator;

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
        this.maxRequestSize = builder.builderMaxRequestSize;
        this.maxInitialLineSize = builder.builderMaxInitialLineSize;
        this.maxHeaderSize = builder.builderMaxHeaderSize;
        this.slowReadTimeout = builder.builderSlowReadTimeout;
        this.responseTimeout = builder.builderResponseTimeout;
        this.connectionTimeout = builder.builderConnectionTimeout;
        this.prechecks = ImmutableList.copyOf(new CopyOnWriteArrayList<>(builder.builderPrechecks));

        this.recvBufferAllocator = new AdaptiveRecvByteBufAllocator(recvAllocatorMin, recvAllocatorInitial, recvAllocatorMax);
        this.recvBufferAllocator.maxMessagesPerRead(maxMessagesPerRead);

        this.translator = new Translator(serverName, serverId, replyTo);
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
//        return connections.size();
        return 0; // TODO
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
    public Duration getSlowReadTimeout ()
    {
        return slowReadTimeout;
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
            if (shutdownHook != null)
            {
                shutdownHook.channel().close().sync();
            }
        }
        return this;
    }

    @Override
    public void close ()
            throws IOException
    {
        // stop();
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
            Runtime.getRuntime().addShutdownHook(new Thread(this::onShutdown));

            final ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup);
            bootstrap.channel(NioServerSocketChannel.class);
            bootstrap.childHandler(new Initializer());
            bootstrap.option(ChannelOption.RCVBUF_ALLOCATOR, recvBufferAllocator);

            shutdownHook = bootstrap.bind(bindAddress, port).sync();
            shutdownHook.channel().closeFuture().sync();
        }
        catch (InterruptedException ex)
        {
            // TODO:
            ex.printStackTrace();
            // logger.catching(ex);
        }
        finally
        {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            service.shutdown();
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
            // TODO:
            ex.printStackTrace();
            // logger.catching(ex);
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

            channel.pipeline().addLast(new HttpResponseEncoder());
            channel.pipeline().addLast(new HttpRequestDecoder(maxInitialLineSize,
                                                              maxHeaderSize,
                                                              maxChunkSize,
                                                              validateHeaders));
            channel.pipeline().addLast(new Prechecker(translator, prechecks));
            channel.pipeline().addLast(new HttpObjectAggregator(maxRequestSize, closeOnExpectationFailed));
            channel.pipeline().addLast(SlowReadTimeout.newHandler(channel, service, slowReadTimeout));
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
    }

    private void closeStaleConnections ()
    {
        try
        {
            router.closeStaleConnections(Instant.MIN);
        }
        catch (Throwable ex)
        {
            // TODO
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

        private Integer builderMaxRequestSize;

        private Integer builderMaxInitialLineSize;

        private Integer builderMaxHeaderSize;

        private Duration builderSlowReadTimeout;

        private Duration builderResponseTimeout;

        private Duration builderConnectionTimeout;

        private final List<Precheck> builderPrechecks = new LinkedList<>();

        private Builder ()
        {
            // Pass.
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
        public Builder withSlowReadTimeout (final Duration timeout)
        {
            if (timeout == null)
            {
                throw new NullPointerException("SlowReadTimeout");
            }
            else
            {
                builderSlowReadTimeout = timeout;
                return this;
            }
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
            if (timeout == null)
            {
                throw new NullPointerException("ResponseTimeout");
            }
            else
            {
                builderResponseTimeout = timeout;
                return this;
            }
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
            if (timeout == null)
            {
                throw new NullPointerException("ConnectionTimeout");
            }
            else
            {
                builderConnectionTimeout = timeout;
                return this;
            }
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
            if (condition == null)
            {
                throw new NullPointerException("condition");
            }
            else
            {
                final Precheck check = msg -> condition.test(msg) ? Precheck.Result.ACCEPT : Precheck.Result.FORWARD;
                builderPrechecks.add(check);
                return this;
            }
        }

        /**
         * Unconditionally reject any HTTP requests that reach this predicate.
         *
         * <p>
         * Equivalent: <code>withPredicateReject(x -> true)</code>
         * </p>
         *
         * @return this.
         */
        public Builder withPredicateReject ()
        {
            return withPredicateReject(x -> true);
        }

        /**
         * Conditionally reject HTTP requests that reach this predicate,
         * forwarding any non-matching requests to the next predicate in the rule chain.
         *
         * <p>
         * If this limit is exceeded, then an HTTP (403) (Forbidden)
         * response will be sent to the client automatically.
         * </p>
         *
         * @param condition may cause the HTTP request to be rejected.
         * @return this.
         */
        public Builder withPredicateReject (final Predicate<web_m.HttpRequest> condition)
        {
            if (condition == null)
            {
                throw new NullPointerException("condition");
            }
            else
            {
                final Precheck check = msg -> condition.test(msg) ? Precheck.Result.REJECT : Precheck.Result.FORWARD;
                builderPrechecks.add(check);
                return this;
            }
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
         * Construct the web-server and start it up.
         *
         * @return the new web-server.
         */
        public WebServer build ()
        {
            if (builderBindAddress == null)
            {
                throw new IllegalStateException("Required: Bind Address");
            }
            else if (builderPort == null)
            {
                throw new IllegalStateException("Required: Port");
            }
            else if (builderMaxMessagesPerRead == null)
            {
                throw new IllegalStateException("Required: Max Messages Per Second");
            }
            else if (builderRecvAllocatorInitial == null)
            {
                throw new IllegalStateException("Required: Recv Allocator");
            }
            else if (builderSoftConnectionLimit == null)
            {
                throw new IllegalStateException("Required: Soft Connection Limit");
            }
            else if (builderHardConnectionLimit == null)
            {
                throw new IllegalStateException("Required: Hard Connection Limit");
            }
            else if (builderMaxRequestSize == null)
            {
                throw new IllegalStateException("Required: Max Request Size");
            }
            else if (builderMaxInitialLineSize == null)
            {
                throw new IllegalStateException("Required: Max Initial Line Size");
            }
            else if (builderMaxHeaderSize == null)
            {
                throw new IllegalStateException("Required: Max Header Size");
            }
            else if (builderSlowReadTimeout == null)
            {
                throw new IllegalStateException("Required: Slow Read Timeout");
            }
            else if (builderResponseTimeout == null)
            {
                throw new IllegalStateException("Required: Response Timeout");
            }
            else if (builderConnectionTimeout == null)
            {
                throw new IllegalStateException("Required: Connection Timeout");
            }
            else
            {
                final WebServer server = new WebServer(this);
                return server;
            }
        }
    }
}
