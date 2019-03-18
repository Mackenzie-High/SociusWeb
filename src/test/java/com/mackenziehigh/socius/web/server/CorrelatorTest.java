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
import com.mackenziehigh.cascade.Cascade.Stage.Actor;
import com.mackenziehigh.socius.web.messages.web_m;
import com.mackenziehigh.socius.web.messages.web_m.ServerSideHttpRequest;
import com.mackenziehigh.socius.web.messages.web_m.ServerSideHttpResponse;
import com.mackenziehigh.socius.web.server.loggers.CountingWebLogger;
import com.mackenziehigh.socius.web.server.loggers.WebLogger;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class CorrelatorTest
{
    private final web_m.HttpProtocol PROTOCOL = web_m.HttpProtocol.newBuilder()
            .setMajorVersion(1)
            .setMinorVersion(0)
            .setName("HTTP")
            .setText("HTTP/1.0")
            .build();

    private final web_m.ServerSideHttpRequest TIMEOUT_REQUEST = web_m.ServerSideHttpRequest
            .newBuilder()
            .setCorrelationId(UUID.randomUUID().toString())
            .setTimestamp(System.currentTimeMillis())
            .setMethod("GET")
            .setUri("/page0.html")
            .setProtocol(PROTOCOL)
            .build();

    private final web_m.ServerSideHttpRequest REQUEST_1 = web_m.ServerSideHttpRequest
            .newBuilder()
            .setCorrelationId(UUID.randomUUID().toString())
            .setTimestamp(System.currentTimeMillis())
            .setMethod("GET")
            .setUri("/page1.html")
            .setProtocol(PROTOCOL)
            .build();

    private final web_m.ServerSideHttpRequest REQUEST_2 = web_m.ServerSideHttpRequest
            .newBuilder()
            .setCorrelationId(UUID.randomUUID().toString())
            .setTimestamp(System.currentTimeMillis())
            .setMethod("GET")
            .setUri("/page2.html")
            .setProtocol(PROTOCOL)
            .build();

    private final ServerSideHttpResponse MALFORMED_RESPONSE = ServerSideHttpResponse
            .newBuilder()
            .setStatus(200)
            .build();

    private final ServerSideHttpResponse RESPONSE_1 = ServerSideHttpResponse
            .newBuilder()
            .setRequest(REQUEST_1)
            .setStatus(200)
            .build();

    private final ServerSideHttpResponse RESPONSE_2 = ServerSideHttpResponse
            .newBuilder()
            .setCorrelationId(REQUEST_2.getCorrelationId())
            .setStatus(200)
            .build();

    private final Runnable WAITER = () ->
    {
        // Pass.
    };

    private final WebLogger logger = CountingWebLogger.create();

    private final ScheduledExecutorService service = Executors.newScheduledThreadPool(1);

    private final Stage stage = Cascade.newExecutorStage(service);

    private final Correlator hub = new Correlator(stage, service, Duration.ofSeconds(1));

    private final Actor<ServerSideHttpRequest, ServerSideHttpResponse> webapp = Cascade
            .newExecutorStage(service)
            .newActor()
            .withScript((ServerSideHttpRequest req) -> req == REQUEST_1 ? RESPONSE_1 : RESPONSE_2)
            .create();


    {
        hub.requestsOut.output().connect(webapp.input());
        hub.responsesIn.input().connect(webapp.output());
    }

    @After
    public void destroy ()
    {
        service.shutdown();
    }

    /**
     * Test: 20190317180706768599
     *
     * <p>
     * Case: Response Received, Embedded Correlation-ID.
     * </p>
     *
     * @throws java.lang.InterruptedException
     */
    @Test
    public void test20190317180706768599 ()
            throws InterruptedException
    {
        assertTrue(REQUEST_1.hasCorrelationId());
        assertFalse(RESPONSE_1.hasCorrelationId());
        assertEquals(REQUEST_1.getCorrelationId(), RESPONSE_1.getRequest().getCorrelationId());

        final BlockingQueue<ServerSideHttpResponse> queue = new LinkedBlockingQueue<>();
        final Consumer<ServerSideHttpResponse> callback = resp -> queue.add(resp);

        /**
         * This request will cause RESPONSE_1 to be generated.
         * The response does *not* directly have a Correlation-ID.
         * Rather, the Correlation-ID is contained inside the embedded request.
         * Although the only Correlation-ID is embedded request,
         * the response must still be routed correctly.
         */
        hub.dispatch(logger, REQUEST_1, callback);

        final ServerSideHttpResponse response = queue.take();

        assertSame(RESPONSE_1, response);
    }

    /**
     * Test: 20190317184136832666
     *
     * <p>
     * Case: Response Received, Explicit Correlation-ID.
     * </p>
     *
     * @throws java.lang.InterruptedException
     */
    @Test
    public void test20190317184136832666 ()
            throws InterruptedException
    {
        assertTrue(REQUEST_2.hasCorrelationId());
        assertTrue(RESPONSE_2.hasCorrelationId());
        assertEquals(REQUEST_2.getCorrelationId(), RESPONSE_2.getCorrelationId());

        final BlockingQueue<ServerSideHttpResponse> queue = new LinkedBlockingQueue<>();
        final Consumer<ServerSideHttpResponse> callback = resp -> queue.add(resp);

        /**
         * This request will cause RESPONSE_2 to be generated.
         * The response has a Correlation-ID contained directly therein.
         */
        hub.dispatch(logger, REQUEST_2, callback);

        final ServerSideHttpResponse response = queue.take();

        assertSame(RESPONSE_2, response);
    }

    /**
     * Test: 20190317190903146030
     *
     * <p>
     * Case: Response Received, No Correlation-ID.
     * </p>
     *
     * @throws java.lang.InterruptedException
     * @throws java.util.concurrent.ExecutionException
     */
    @Test
    public void test20190317190903146030 ()
            throws InterruptedException,
                   ExecutionException
    {
        assertEquals(0, hub.responseRoutingFailures.get());

        /**
         * The response does not contain a Correlation-ID.
         * Thus, the response cannot be routed anywhere.
         * Hopefully, the response will be silently dropped.
         */
        hub.responsesIn.accept(MALFORMED_RESPONSE);
        assertFalse(MALFORMED_RESPONSE.hasCorrelationId());

        /**
         * This is a very dirty little trick.
         * 1. The actors run on the service executor.
         * 2. The service executor is single-threaded.
         * 3. The (important) submissions to the service
         * happened on the same thread executing this method.
         * 4. The WAITER task will execute *after* the actor-tasks.
         * Therefore, we can use to to wait for the actors to complete.
         */
        service.submit(WAITER).get();

        assertEquals(1, hub.responseRoutingFailures.get());
    }

    /**
     * Test: 20190317193433748899
     *
     * <p>
     * Case: Response Received, No Such Connection.
     * </p>
     *
     * @throws java.lang.InterruptedException
     * @throws java.util.concurrent.ExecutionException
     */
    @Test
    public void test20190317193433748899 ()
            throws InterruptedException,
                   ExecutionException
    {
        assertEquals(0, hub.responseRoutingFailures.get());

        /**
         * The response contains a Correlation-ID.
         * However, no corresponding request ever occurred.
         * Thus, the response cannot be routed anywhere.
         * Hopefully, the response will be silently dropped.
         */
        hub.responsesIn.accept(RESPONSE_2);
        assertTrue(RESPONSE_2.hasCorrelationId());

        /**
         * This is a very dirty little trick.
         * 1. The actors run on the service executor.
         * 2. The service executor is single-threaded.
         * 3. The (important) submissions to the service
         * happened on the same thread executing this method.
         * 4. The WAITER task will execute *after* the actor-tasks.
         * Therefore, we can use to to wait for the actors to complete.
         */
        service.submit(WAITER).get();

        assertEquals(1, hub.responseRoutingFailures.get());
    }

    /**
     * Test: 20190317194602573620
     *
     * <p>
     * Case: Response Timeout.
     * </p>
     *
     * @throws java.lang.InterruptedException
     */
    @Test
    public void test20190317194602573620 ()
            throws InterruptedException
    {
        final BlockingQueue<ServerSideHttpResponse> queue = new LinkedBlockingQueue<>();
        final Consumer<ServerSideHttpResponse> callback = resp -> queue.add(resp);

        /**
         * The actor will not generate a response of the TIMEOUT_REQUEST.
         * Therefore, the connection will be closed by the response-timeout.
         * An automatic response will be sent (408 - Request Timeout).
         */
        hub.dispatch(logger, TIMEOUT_REQUEST, callback);

        final ServerSideHttpResponse response = queue.take();

        assertEquals(408, response.getStatus());
        assertEquals("<head> <meta http-equiv=\"refresh\" content=\"5; URL=\"/408.html\" /> </head>\r\n",
                     response.getBody().toStringUtf8());
    }

}
