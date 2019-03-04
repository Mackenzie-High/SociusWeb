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

import com.google.common.collect.Maps;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor;
import com.mackenziehigh.socius.web.messages.web_m.ServerSideHttpRequest;
import com.mackenziehigh.socius.web.messages.web_m.ServerSideHttpResponse;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

final class Router
{
    /**
     * This map maps correlation UUIDs of requests to consumer functions
     * that will be used to process the corresponding responses.
     *
     * <p>
     * Entries are added to this map whenever an HTTP request begins.
     * </p>
     *
     * <p>
     * Entries are periodically removed from this map, if they have been there too long.
     * In such cases, the an HTTP response will be sent to the client,
     * which will indicate that the connection timed-out.
     * </p>
     */
    private final Map<String, Conversation> connections = Maps.newConcurrentMap();

    /**
     * This queue is used to remove stale connections from the map of connections.
     */
    private final Queue<Conversation> responseTimeoutQueue = new PriorityQueue<>(Conversation.COMPARATOR);

    /**
     * This processor will be used to send HTTP requests out of the server,
     * so that external handler actors can process the requests.
     */
    public final Actor<ServerSideHttpRequest, ServerSideHttpRequest> requestsOut;

    /**
     * This processor will receive the HTTP responses from the external actors
     * and then will route those responses to the originating connection.
     */
    public final Actor<ServerSideHttpResponse, ServerSideHttpResponse> responsesIn;

    public Router (final Stage stage)
    {
        this.requestsOut = stage.newActor().withScript(this::onRequest).create();
        this.responsesIn = stage.newActor().withScript(this::onResponse).create();
    }

    public SimpleChannelInboundHandler<ServerSideHttpRequest> newHandler ()
    {
        final AtomicBoolean firstRequest = new AtomicBoolean();

        return new SimpleChannelInboundHandler<ServerSideHttpRequest>()
        {
            @Override
            protected void channelRead0 (final ChannelHandlerContext ctx,
                                         final ServerSideHttpRequest msg)
            {
                if (firstRequest.compareAndSet(false, true))
                {
                    open(msg, response -> reply(ctx, response));
                }
            }

            private void reply (final ChannelHandlerContext ctx,
                                final ServerSideHttpResponse response)
            {
                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            }
        };
    }

    /**
     * Begin managing a request-response exchange.
     *
     * @param request will be sent to the external actors, who will generate a response.
     * @param callback will be used to transmit the response to the client.
     */
    private void open (final ServerSideHttpRequest request,
                       final Consumer<ServerSideHttpResponse> callback)
    {
        synchronized (this)
        {
            final String correlationId = request.getCorrelationId();

            if (connections.containsKey(correlationId))
            {
                return;
            }

            /**
             * Create the response handler that will be used to route
             * the corresponding HTTP Response, if and when it occurs.
             */
            final Conversation connection = new Conversation(correlationId, callback);
            responseTimeoutQueue.add(connection);
            connections.put(correlationId, connection);

            /**
             * Send the HTTP Request to the external actors,
             * so they can form an HTTP Response.
             */
            requestsOut.input().send(request);
        }
    }

    private ServerSideHttpRequest onRequest (final ServerSideHttpRequest request)
    {
        return request;
    }

    private void onResponse (final ServerSideHttpResponse response)
    {
        routeResponse(response);
    }

    /**
     * This actor receives the HTTP Responses from the external handlers connected to this server,
     * routes them to the appropriate client-server connection, and then transmits them via Netty.
     *
     * @param response needs to be send to a client.
     */
    private void routeResponse (final ServerSideHttpResponse response)
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
        final Conversation connection = connections.get(correlationId);

        /**
         * If a response was already transmitted, then the connection was already closed.
         * Alternatively, the external handlers may be broadcasting responses to multiple
         * servers in a publish-subscribe like manner. Consequently, we may have been
         * given a response that does not correlate to one of our connections.
         * In either case, just silently drop the response.
         */
        if (connection == null)
        {
            return;
        }

        /**
         * Send the HTTP Response to the client.
         */
        connection.respondWith(response);
    }

    /**
     * Close any open connections that were created before the given limit.
     *
     * @param limit specifies the maximum age of any open connections.
     */
    public void closeStaleConnections (final Instant limit)
    {
        while (responseTimeoutQueue.isEmpty() == false)
        {
            final Conversation conn = responseTimeoutQueue.peek();

            if (conn.creationTime.isBefore(limit))
            {
                conn.closeStaleConnection();
            }
            else
            {
                break;
            }
        }
    }

    /**
     * Representation of client-server connection.
     */
    private static final class Conversation
    {
        public static final Comparator<Conversation> COMPARATOR = (x, y) -> x.creationTime.compareTo(y.creationTime);

        public final Instant creationTime = Instant.now();

        public final Consumer<ServerSideHttpResponse> output;

        public final String correlationId;

        private final AtomicBoolean sent = new AtomicBoolean();

        public Conversation (final String correlationId,
                             final Consumer<ServerSideHttpResponse> onResponse)
        {

            this.correlationId = correlationId;
            this.output = onResponse;
        }

        public void respondWith (final ServerSideHttpResponse response)
        {
            /**
             * Sending a response is a one-shot operation.
             * Do not allow duplicate responses.
             */
            if (sent.compareAndSet(false, true))
            {
                output.accept(response);
            }
        }

        public void closeStaleConnection ()
        {
            final ServerSideHttpResponse response = Translator.newErrorResponseGPB(HttpResponseStatus.REQUEST_TIMEOUT.code());
            respondWith(response);
        }
    }
}
