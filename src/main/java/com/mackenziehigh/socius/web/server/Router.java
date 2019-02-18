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

import com.google.common.base.Verify;
import com.google.common.collect.Maps;
import com.mackenziehigh.cascade.Cascade.Stage.Actor;
import com.mackenziehigh.socius.web.messages.web_m.HttpRequest;
import com.mackenziehigh.socius.web.messages.web_m.HttpResponse;
import java.net.URISyntaxException;
import java.time.Instant;
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
     * Entries are added to this map whenever an HTTP request is processed.
     * Entries are lazily removed from this map, if they have been there too long.
     * </p>
     */
    private final Map<String, Conversation> connections = Maps.newConcurrentMap();

    /**
     * This queue is used to remove connections from the map of connections.
     */
    private final Queue<Conversation> responseTimeoutQueue = new PriorityQueue<>();

    /**
     * This processor will be used to send HTTP requests out of the server,
     * so that external handler actors can process the requests.
     */
    public final Actor<HttpRequest, HttpRequest> requestsOut;

    /**
     * This processor will receive the HTTP responses from the external actors
     * and then will route those responses to the originating connection.
     */
    public final Actor<HttpResponse, HttpResponse> responsesIn;

    public Router (final SharedState shared)
    {
//        this.shared = shared;
        this.requestsOut = shared.stage.newActor().withScript(this::onRequest).create();
        this.responsesIn = shared.stage.newActor().withScript(this::onResponse).create();
    }

    public void open (final HttpRequest request,
                      final Consumer<HttpResponse> callback)
            throws URISyntaxException
    {
        final String correlationId = request.getCorrelationId();

        /**
         * Create the response handler that will be used to route
         * the corresponding HTTP Response, if and when it occurs.
         */
        Verify.verify(connections.containsKey(correlationId) == false);
        final Conversation connection = new Conversation(correlationId, callback);
        responseTimeoutQueue.add(connection);
        connections.put(correlationId, connection);

        /**
         * Send the HTTP Request to the external actors,
         * so they can form an HTTP Response.
         */
        requestsOut.input().send(request);
    }

    private HttpRequest onRequest (final HttpRequest request)
    {
        return request;
    }

    private void onResponse (final HttpResponse response)
    {
        routeResponse(response);
    }

    /**
     * This actor receives the HTTP Responses from the external handlers connected to this server,
     * routes them to the appropriate client-server connection, and then transmits them via Netty.
     *
     * @param response needs to be send to a client.
     */
    private void routeResponse (final HttpResponse response)
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

        if (connection == null)
        {
            return;
        }

        /**
         * Send the HTTP Response to the client, if they are still connected.
         */
        connection.respondWith(response);
    }

    private void prunePendingResponses ()
    {
        final Instant now = Instant.now();

//        while (responseTimeoutQueue.isEmpty() == false)
//        {
//            final Connection conn = responseTimeoutQueue.peek();
//
//            if (conn.timeout.isBefore(now.minus(responseTimeout)))
//            {
//                conn.close();
//            }
//            else
//            {
//                break;
//            }
//        }
    }

    /**
     * Representation of client-server connection.
     */
    private final class Conversation
            implements Comparable<Conversation>
    {
        public final Instant timeout = Instant.now();

        public final Consumer<HttpResponse> output;

        public final String correlationId;

        private final AtomicBoolean sent = new AtomicBoolean();

        private final AtomicBoolean closed = new AtomicBoolean();

        public Conversation (final String correlationId,
                             final Consumer<HttpResponse> onResponse)
        {

            this.correlationId = correlationId;
            this.output = onResponse;
        }

        @Override
        public int compareTo (final Conversation other)
        {
            return timeout.compareTo(other.timeout);
        }

        public void respondWith (final HttpResponse response)
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
             * Send the response to the client and close the connection.
             */
            output.accept(response);
        }
    }
}
