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
import com.mackenziehigh.socius.web.server.loggers.WebLogger;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * An instance of this class provides the point of contact between
 * the web-server itself and the third-party actors connected thereto.
 */
final class Correlator
{
    private final ScheduledExecutorService service;

    /**
     * If a response is not received within this time-limit,
     * then a default response will be sent to the client.
     */
    private final Duration responseTimeout;

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
    private final Map<String, OpenConnection> connections = Maps.newConcurrentMap();

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

    public Correlator (final Stage stage,
                       final ScheduledExecutorService service,
                       final Duration responseTimeout)
    {
        this.requestsOut = stage.newActor().withScript(this::onRequest).create();
        this.responsesIn = stage.newActor().withScript(this::onResponse).create();
        this.responseTimeout = responseTimeout;
        this.service = service;
    }

    /**
     * Begin managing a request-response exchange.
     *
     * @param logger is the connection-specific logger.
     * @param request will be sent to the external actors, who will generate a response.
     * @param callback will be used to transmit the response to the client.
     */
    public void dispatch (final WebLogger logger,
                          final ServerSideHttpRequest request,
                          final Consumer<ServerSideHttpResponse> callback)
    {
        logger.onRequest(request);

        final String correlationId = request.getCorrelationId();

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
        final OpenConnection connection = new OpenConnection(logger, correlationId, callback);
        connections.putIfAbsent(correlationId, connection);

        /**
         * If a response is not received before the response-timeout expires,
         * then we will need to generate a default error-response.
         */
        service.schedule(() -> connection.closeStaleConnection(), responseTimeout.toMillis(), TimeUnit.MILLISECONDS);

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
        final OpenConnection connection = connections.get(correlationId);

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
            connection.respondWith(response);
        }
    }

    private static final class OpenConnection
    {
        private final WebLogger logger;

        public final Consumer<ServerSideHttpResponse> output;

        public final String correlationId;

        private final AtomicBoolean sent = new AtomicBoolean();

        public OpenConnection (final WebLogger logger,
                               final String correlationId,
                               final Consumer<ServerSideHttpResponse> onResponse)
        {
            this.logger = logger;
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
                logger.onResponse(response);
                output.accept(response);
            }
        }

        public void closeStaleConnection ()
        {
            /**
             * Sending a response is a one-shot operation.
             * Do not allow duplicate responses.
             */
            if (sent.compareAndSet(false, true))
            {
                final ServerSideHttpResponse response = Translator.newErrorResponseGPB(HttpResponseStatus.REQUEST_TIMEOUT.code());
                logger.onResponseTimeout();
                logger.onResponse(response);
                output.accept(response);
            }
        }
    }
}
