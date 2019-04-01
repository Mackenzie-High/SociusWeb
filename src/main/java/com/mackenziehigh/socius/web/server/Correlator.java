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
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * An instance of this class provides the point of contact between
 * the web-server itself and the third-party actors connected thereto.
 */
final class Correlator
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
    private final Map<String, Consumer<ServerSideHttpResponse>> connections = Maps.newConcurrentMap();

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

    /**
     * Keep a count of the responses could not be routed to an open connection.
     * Expose this counter for use in the unit-tests.
     */
    final AtomicLong responseRoutingFailures = new AtomicLong();

    public Correlator (final Stage stage)
    {
        this.requestsOut = stage.newActor().withScript(this::onRequest).create();
        this.responsesIn = stage.newActor().withScript(this::onResponse).create();
    }

    /**
     * Disregard any response(s) that subsequently arrive with the given Correlation-ID.
     *
     * @param correlationId identifies the request and the response.
     */
    public void disregard (final String correlationId)
    {
        connections.remove(correlationId);
    }

    /**
     * Begin managing a request-response exchange.
     *
     * @param cid identifies the request and the response.
     * @param request will be sent to the external actors, who will generate a response.
     * @param callback will be used to transmit the response to the client.
     */
    public void dispatch (final String correlationId,
                          final ServerSideHttpRequest request,
                          final Consumer<ServerSideHttpResponse> callback)
    {
        /**
         * Do not allow a connection to be opened with
         * the same UUID as an existing connection.
         */
        if (connections.containsKey(correlationId))
        {
            return;
        }

        /**
         * Create the response handler that will be used to route
         * the corresponding HTTP Response, if and when it occurs.
         */
        connections.putIfAbsent(correlationId, callback);

        /**
         * Send the HTTP Request to the external actors,
         * so they can form an HTTP Response.
         */
        requestsOut.input().send(request);
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
            responseRoutingFailures.incrementAndGet();
            return;
        }

        /**
         * Find the connection that correlates to the given response.
         */
        final Consumer<ServerSideHttpResponse> connection = connections.get(correlationId);

        if (connection == null)
        {
            /**
             * If a response was already transmitted, then the connection was already closed.
             * Alternatively, the external handlers may be broadcasting responses to multiple
             * servers in a publish-subscribe like manner. Consequently, we may have been
             * given a response that does not correlate to one of our connections.
             * In either case, just silently drop the response.
             */
            responseRoutingFailures.incrementAndGet();
        }
        else
        {
            /**
             * Send the HTTP Response to the client and cause the connection to be closed thereafter.
             */
            connection.accept(response);
        }
    }
}
