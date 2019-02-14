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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.ByteString;
import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Input;
import com.mackenziehigh.cascade.Cascade.Stage.Actor.Output;
import com.mackenziehigh.socius.web.messages.web_m;
import com.mackenziehigh.socius.web.messages.web_m.HttpPrefix;
import com.mackenziehigh.socius.web.messages.web_m.HttpRequest;
import com.mackenziehigh.socius.web.messages.web_m.HttpResponse;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A experimental non-blocking HTTP server based on the Netty framework.
 */
public final class WebServer
{
    private static final Logger logger = LogManager.getLogger(WebServer.class);

    private final SharedState shared;

    private final ImmutableList<Precheck> prechecks;

    /**
     * TODO: Make the sequence-numbers increment.
     */
    private final Translator translator;

    private final Router router;

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
        this.shared = new SharedState(Executors.newScheduledThreadPool(1),
                                      builder.serverName,
                                      builder.replyTo,
                                      builder.host,
                                      builder.port,
                                      builder.responseTimeout,
                                      builder.aggregationCapacity);
        this.prechecks = ImmutableList.copyOf(builder.checks);

        this.translator = new Translator(shared.serverName, shared.serverId, shared.replyTo);
        this.router = new Router(shared);
    }

    /**
     * Get the current state of the sequence-number generator.
     *
     * @return the current sequence-number.
     */
    public long getSequenceCount ()
    {
        return shared.sequenceNumber.get();
    }

    /**
     * Get the human-readable name of this server.
     *
     * @return the server name.
     */
    public String getServerName ()
    {
        return shared.serverName;
    }

    /**
     * Get the universally-unique-identifier of this server instance.
     *
     * @return the server identifier.
     */
    public String getServerId ()
    {
        return shared.serverId;
    }

    /**
     * Get the (Reply-To) property embedded in each outgoing request.
     *
     * @return the reply-to address.
     */
    public String getReplyTo ()
    {
        return shared.replyTo;
    }

    /**
     * Get the name of the host that the server is listening on.
     *
     * @return the server host.
     */
    public String getHost ()
    {
        return shared.host;
    }

    /**
     * Get the port that the server is listening on.
     *
     * @return the server port.
     */
    public int getPort ()
    {
        return shared.port;
    }

    /**
     * Get the maximum amount of time the server will wait for a response,
     * before the connection is closed without sending a response.
     *
     * @return the connection timeout.
     */
    public Duration getResponseTimeout ()
    {
        return shared.responseTimeout;
    }

    /**
     * Get the size of the buffer used to join chunked messages into one.
     *
     * @return the approximate maximum size of a request.
     */
    public int getAggregationCapacity ()
    {
        return shared.aggregationCapacity;
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
        final EventLoopGroup bossGroup = new NioEventLoopGroup(2);
        final EventLoopGroup workerGroup = new NioEventLoopGroup(2);
        try
        {
            Runtime.getRuntime().addShutdownHook(new Thread(this::onShutdown));

            final ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new Initializer());

            shutdownHook = b.bind(shared.host, shared.port).sync();
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
            HardTimeout.enforce(shared.service, channel, Duration.ofSeconds(5));

            channel.pipeline().addLast(new HttpResponseEncoder());
            channel.pipeline().addLast(new HttpRequestDecoder(shared.maxInitialLineLength,
                                                              shared.maxHeaderSize,
                                                              shared.maxChunkSize,
                                                              shared.validateHeaders));
            channel.pipeline().addLast(new PrecheckHandler(translator, prechecks));
            channel.pipeline().addLast(new HttpObjectAggregator(shared.aggregationCapacity, true));
            channel.pipeline().addLast(new TranslationEncoder(translator));
            channel.pipeline().addLast(new TranslationDecoder(translator));
            channel.pipeline().addLast(new RoutingHandler(router));
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

        private final List<Precheck> checks = new LinkedList<>();

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
         * Set the maximum allowed size of any HTTP message.
         *
         * <p>
         * For chunked messages, this is the maximum allowed
         * size of the overall combined message, rather than
         * the individual chunks.
         * </p>
         *
         * @param limit will limit the size of incoming messages.
         * @return this.
         */
        public Builder withMaxHttpMessageSize (final int limit)
        {
            Preconditions.checkArgument(limit >= 0, "limit < 0");
            this.aggregationCapacity = limit;
            return this;
        }

        public Builder withMaxConnectionCount (final int soft,
                                               final int hard)
        {
            return this;
        }

        public Builder withMaxConnectionRate (final int hertz)
        {
            return this;
        }

        public Builder withPrecheckAccept ()
        {
            return withPrecheckAccept(x -> true);
        }

        public Builder withPrecheckAccept (final Predicate<HttpPrefix> condition)
        {
            final Precheck check = msg ->
            {
                return condition.test(msg) ? Precheck.Result.ACCEPT : Precheck.Result.FORWARD;
            };

            checks.add(check);

            return this;
        }

        public Builder withPrecheckDeny ()
        {
            return withPrecheckDeny(x -> true);
        }

        public Builder withPrecheckDeny (final Predicate<HttpPrefix> condition)
        {
            final Precheck check = msg ->
            {
                return condition.test(msg) ? Precheck.Result.DENY : Precheck.Result.FORWARD;
            };

            checks.add(check);

            return this;
        }

        public Builder withHttpReadTimeout (final Duration timeout)
        {
            return this;
        }

        /**
         * Connections will be closed automatically, if a response exceeds this timeout.
         *
         * @param timeout is the maximum amount of time allowed for a response.
         * @return this.
         */
        public Builder withHttpResponseTimeout (final Duration timeout)
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

    public static void main (String[] args)
            throws InterruptedException
    {
        final Cascade.Stage stage = Cascade.newStage();

        final WebServer server = WebServer
                .newWebServer()
                .withHttpResponseTimeout(Duration.ofSeconds(1))
                .withHost("127.0.0.1")
                .withPort(8089)
                .withReplyTo("Mars")
                .withServerName("Alien")
                .withMaxHttpMessageSize(1 * 1024 * 1024)
                .withPrecheckDeny(x -> x.getMethod().equalsIgnoreCase("GET"))
                .withPrecheckAccept()
                .build();

        final Cascade.Stage.Actor<web_m.HttpRequest, web_m.HttpResponse> website = stage.newActor().withScript(WebServer::onRequest).create();
        server.requestsOut().connect(website.input());
        server.responsesIn().connect(website.output());

        server.start();

        Thread.sleep(2000);
    }

    private static HttpResponse onRequest (final web_m.HttpRequest request)
    {
        final byte[] bytes = request.toString().getBytes();

        if (request.getPath().contains("/sleep"))
        {
            try
            {
                Thread.sleep(10_000);
            }
            catch (InterruptedException ex)
            {
                // Pass
            }
        }

        final web_m.HttpResponse response = web_m.HttpResponse
                .newBuilder()
                .setRequest(request)
                .setContentType("text/martian")
                .setStatus(200)
                .setBody(ByteString.copyFrom(bytes))
                .build();

        return response;
    }
}
