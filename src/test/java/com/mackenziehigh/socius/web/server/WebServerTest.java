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

import com.google.common.base.Strings;
import com.mackenziehigh.socius.web.server.filters.RequestFilters;
import com.mackenziehigh.socius.web.server.loggers.ChainWebLogger;
import com.mackenziehigh.socius.web.server.loggers.CountingWebLogger;
import com.mackenziehigh.socius.web.server.loggers.DefaultWebLogger;
import java.net.SocketException;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class WebServerTest
{
    private static final AtomicInteger addressAllocator = new AtomicInteger(1);

    /**
     * Allocate a unique local-host address for the server to listen on.
     * This avoid problems due to TIME_WAIT, etc, since these unit-tests
     * will be starting and stopping many server instances rapidly.
     */
    private final String address = String.format("127.0.0.%d", addressAllocator.incrementAndGet());

    /**
     * All of the test servers will listen on this port, unless stated otherwise.
     */
    private final int port = 8080;

    /**
     * This logger provides counters that allow us to determine
     * which parts of the server code executed during a unit-test.
     */
    private final CountingWebLogger counter = CountingWebLogger.create();

    /**
     * This logger prints log-messages to standard-output.
     */
    private final DefaultWebLogger slogger = new DefaultWebLogger();

    /**
     * This logger combines the aforesaid loggers into one.
     */
    private final ChainWebLogger logger = new ChainWebLogger(counter, slogger);

    private void awaitServerShutdown (final WebServer server)
            throws InterruptedException
    {
        assertTrue(server.isRunning());
        server.stop().awaitShutdown(Duration.ofDays(1));
        assertFalse(server.isRunning());
    }

    /**
     * Test: 20190303101911066030
     *
     * <p>
     * Method: <code>withDefaultSettings</code>
     * </p>
     *
     * <p>
     * Case: Verify that the correct defaults are applied.
     * </p>
     */
    @Test
    public void test20190303101911066030 ()
    {
        final WebServer server = WebServer.newWebServer().withDefaultSettings().build();

        assertEquals(WebServer.DEFAULT_SERVER_NAME, server.getServerName());
        assertEquals(WebServer.DEFAULT_REPLY_TO, server.getReplyTo());
        assertEquals(WebServer.DEFAULT_BIND_ADDRESS, server.getBindAddress());
        assertEquals(WebServer.DEFAULT_PORT, server.getPort());
        assertEquals(WebServer.DEFAULT_MAX_MESSAGES_PER_READ, server.getMaxMessagesPerRead());
        assertEquals(WebServer.DEFAULT_RECV_ALLOCATOR_INITIAL, server.getRecvBufferInitialSize());
        assertEquals(WebServer.DEFAULT_RECV_ALLOCATOR_MAX, server.getRecvBufferMaxSize());
        assertEquals(WebServer.DEFAULT_RECV_ALLOCATOR_MIN, server.getRecvBufferMinSize());
        assertEquals(WebServer.DEFAULT_SOFT_CONNECTION_LIMIT, server.getSoftConnectionLimit());
        assertEquals(WebServer.DEFAULT_HARD_CONNECTION_LIMIT, server.getHardConnectionLimit());
        assertEquals(WebServer.DEFAULT_MAX_PAUSE_TIME, server.getMaxPauseTime());
        assertEquals(WebServer.DEFAULT_MAX_REQUEST_SIZE, server.getMaxRequestSize());
        assertEquals(WebServer.DEFAULT_MAX_INITIAL_LINE_SIZE, server.getMaxInitialLineSize());
        assertEquals(WebServer.DEFAULT_MAX_HEADER_SIZE, server.getMaxHeaderSize());
        assertEquals(WebServer.DEFAULT_COMPRESSION_LEVEL, server.getCompressionLevel());
        assertEquals(WebServer.DEFAULT_COMPRESSION_WINDOW_BITS, server.getCompressionWindowBits());
        assertEquals(WebServer.DEFAULT_COMPRESSION_MEMORY_LEVEL, server.getCompressionMemoryLevel());
        assertEquals(WebServer.DEFAULT_COMPRESSION_THRESHOLD, server.getCompressionThreshold());
        assertEquals(WebServer.DEFAULT_SLOW_UPLINK_TIMEOUT, server.getUplinkTimeout());
        assertEquals(WebServer.DEFAULT_SLOW_DOWNLINK_TIMEOUT, server.getDownlinkTimeout());
        assertEquals(WebServer.DEFAULT_RESPONSE_TIMEOUT, server.getResponseTimeout());
    }

    /**
     * Test: 20190303102051831715
     *
     * <p>
     * Case: Verify that the builder sets all settings correctly.
     * </p>
     */
    @Test
    public void test20190303102051831715 ()
    {
        final WebServer server = WebServer
                .newWebServer()
                .withServerName("Voyager")
                .withReplyTo("Earth")
                .withBindAddress("127.0.0.17")
                .withPort(8008)
                .withMaxMessagesPerRead(23)
                .withRecvBufferAllocator(31, 33, 37)
                .withSoftConnectionLimit(41)
                .withHardConnectionLimit(43)
                .withMaxPauseTime(Duration.ofSeconds(57))
                .withMaxRequestSize(61)
                .withMaxInitialLineSize(63)
                .withMaxHeaderSize(67)
                .withCompressionLevel(WebServer.DEFAULT_COMPRESSION_LEVEL - 2)
                .withCompressionWindowBits(WebServer.DEFAULT_COMPRESSION_WINDOW_BITS - 2)
                .withCompressionMemoryLevel(WebServer.DEFAULT_COMPRESSION_MEMORY_LEVEL - 2)
                .withCompressionThreshold(71)
                .withUplinkTimeout(Duration.ofSeconds(73))
                .withDownlinkTimeout(Duration.ofSeconds(79))
                .withResponseTimeout(Duration.ofSeconds(83))
                .withUnsecureSockets()
                .build();

        assertEquals("Voyager", server.getServerName());
        assertEquals("Earth", server.getReplyTo());
        assertEquals("127.0.0.17", server.getBindAddress());
        assertEquals(8008, server.getPort());
        assertEquals(23, server.getMaxMessagesPerRead());
        assertEquals(31, server.getRecvBufferMinSize());
        assertEquals(33, server.getRecvBufferInitialSize());
        assertEquals(37, server.getRecvBufferMaxSize());
        assertEquals(41, server.getSoftConnectionLimit());
        assertEquals(43, server.getHardConnectionLimit());
        assertEquals(Duration.ofSeconds(57), server.getMaxPauseTime());
        assertEquals(61, server.getMaxRequestSize());
        assertEquals(63, server.getMaxInitialLineSize());
        assertEquals(67, server.getMaxHeaderSize());
        assertEquals(4, server.getCompressionLevel());
        assertEquals(13, server.getCompressionWindowBits());
        assertEquals(6, server.getCompressionMemoryLevel());
        assertEquals(71, server.getCompressionThreshold());
        assertEquals(Duration.ofSeconds(73), server.getUplinkTimeout());
        assertEquals(Duration.ofSeconds(79), server.getDownlinkTimeout());
        assertEquals(Duration.ofSeconds(83), server.getResponseTimeout());
        assertFalse(server.isSecure());
    }

    /**
     * Test: 20190308002223203440
     *
     * <p>
     * Type: End-To-End Throughput Test.
     * </p>
     *
     * <p>
     * Case: Basic Throughput.
     * </p>
     *
     * @throws java.lang.InterruptedException
     */
    @Test
    public void test20190308002223203440 ()
            throws Throwable
    {
        /**
         * This is the server under test.
         */
        final WebServer engine = WebServer
                .newWebServer()
                .withDefaultSettings()
                .withLogger(logger)
                .withBindAddress(address)
                .withPort(port)
                .withRequestFilter(RequestFilters.accept())
                .build();

        /**
         * Create a web-application based on the server.
         */
        final TestApplication app = new TestApplication(engine);
        app.start();

        /**
         * Send a request to the server and receive a response.
         */
        final SocketTester tester = new SocketTester(address, port);
        tester.sendln("GET /sleep?period=1 HTTP/1.1");
        tester.sendln();
        tester.recvln("HTTP/1.1 200 OK");
        tester.recvln("Connection: close");
        tester.recvln("Content-Type: text/plain");
        tester.recvln("Content-Length: 14");
        tester.recvln();
        tester.recv("SleepComplete\n");
        tester.closed();

        /**
         * Avoid a race-condition with the asserts below,
         * by waiting for the server to fully shutdown.
         */
        awaitServerShutdown(engine);

        assertEquals(1, counter.countOnAccepted());
        assertEquals(1, counter.countOnConnect());
        assertEquals(0, counter.countOnDenied());
        assertEquals(1, counter.countOnDisconnect());
        assertEquals(0, counter.countOnDownlinkTimeout());
        assertEquals(0, counter.countOnException());
        assertEquals(1, counter.countExtend());
        assertEquals(0, counter.countOnRejected());
        assertEquals(1, counter.countOnRequest());
        assertEquals(1, counter.countOnResponse());
        assertEquals(0, counter.countOnResponseTimeout());
        assertEquals(0, counter.countOnTooManyConnections());
        assertEquals(0, counter.countOnUplinkTimeout());
    }

    /**
     * Test: 20190308002223203558
     *
     * <p>
     * Type: End-To-End Throughput Test.
     * </p>
     *
     * <p>
     * Case: Uplink Timeout.
     * </p>
     *
     * @throws java.lang.Throwable
     */
    @Test
    public void test20190308002223203558 ()
            throws Throwable
    {
        /**
         * This is the server under test.
         *
         * Notice that the uplink-timeout is only one millisecond.
         */
        final WebServer engine = WebServer
                .newWebServer()
                .withDefaultSettings()
                .withLogger(logger)
                .withBindAddress(address)
                .withPort(port)
                .withRequestFilter(RequestFilters.accept())
                .withUplinkTimeout(Duration.ofMillis(1))
                .withResponseTimeout(Duration.ofSeconds(60))
                .build();

        /**
         * Create a web-application based on the server.
         */
        final TestApplication app = new TestApplication(engine);
        app.start();

        /**
         * Begin sending a request to the server.
         * Notice that this is not a full request.
         * Wait for the uplink timeout to expire.
         * The connection will be closed by the server.
         */
        final SocketTester tester = new SocketTester(address, port);
        tester.send("GET");
        tester.sleep(100);
        tester.closed();

        /**
         * Shutdown the server.
         */
        awaitServerShutdown(engine);

        assertEquals(0, counter.countOnAccepted());
        assertEquals(1, counter.countOnConnect());
        assertEquals(0, counter.countOnDenied());
        assertEquals(1, counter.countOnDisconnect());
        assertEquals(0, counter.countOnDownlinkTimeout());
        assertEquals(0, counter.countOnException());
        assertEquals(1, counter.countExtend());
        assertEquals(0, counter.countOnRejected());
        assertEquals(0, counter.countOnRequest());
        assertEquals(0, counter.countOnResponse());
        assertEquals(0, counter.countOnResponseTimeout());
        assertEquals(0, counter.countOnTooManyConnections());
        assertEquals(1, counter.countOnUplinkTimeout());
    }

    /**
     * Test: 20190308002223203594
     *
     * <p>
     * Type: End-To-End Throughput Test.
     * </p>
     *
     * <p>
     * Case: Downlink Timeout.
     * </p>
     *
     * @throws java.lang.Throwable
     */
    @Test
    public void test20190308002223203594 ()
            throws Throwable
    {
        fail();

        final WebServer engine = WebServer
                .newWebServer()
                .withDefaultSettings()
                .withBindAddress(address)
                .withPort(port)
                .withRequestFilter(RequestFilters.accept())
                .withDownlinkTimeout(Duration.ofMillis(1))
                .withResponseTimeout(Duration.ofSeconds(60))
                .build();

        final TestApplication app = new TestApplication(engine);
        app.start();

        final SocketTester tester = new SocketTester(address, port);
        tester.sendln("GET /zero?count=3200100 HTTP/1.1");
        tester.sendln();
        tester.recvln("HTTP/1.1 200 OK");
        tester.sleep(100);
        final long discarded = tester.exhaust();
        assertTrue(discarded < 3200100);
        tester.closed();

        awaitServerShutdown(engine);
    }

    /**
     * Test: 20190308002223203627
     *
     * <p>
     * Type: End-To-End Throughput Test.
     * </p>
     *
     * <p>
     * Case: Response Timeout.
     * </p>
     *
     * @throws java.lang.Throwable
     */
    @Test
    public void test20190308002223203627 ()
            throws Throwable
    {
        /**
         * This is the server under test.
         *
         * Notice that the response timeout is only one millisecond.
         */
        final WebServer engine = WebServer
                .newWebServer()
                .withDefaultSettings()
                .withLogger(logger)
                .withBindAddress(address)
                .withPort(port)
                .withAcceptFilter()
                .withResponseTimeout(Duration.ofMillis(1))
                .build();

        /**
         * Create a web-application based on the server.
         */
        final TestApplication app = new TestApplication(engine);
        app.start();

        /**
         * Send a request to the server.
         * The web-application will take one second to generate a response.
         * However, the response timeout is only one millisecond.
         * Thus, the server will send a default response,
         * before the web-application generates a response.
         */
        final SocketTester tester = new SocketTester(address, port);
        tester.sendln("GET /sleep?period=1000 HTTP/1.1");
        tester.sendln();
        tester.recvln("HTTP/1.1 408 Request Timeout");
        tester.recvln("Connection: close");
        tester.recvln("Content-Type: text/html");
        tester.recvln("Content-Length: 74");
        tester.recvln();
        tester.recvln("<head> <meta http-equiv=\"refresh\" content=\"5; URL=\"/408.html\" /> </head>");
        tester.closed();

        /**
         * Shutdown the server.
         */
        awaitServerShutdown(engine);

        assertEquals(1, counter.countOnAccepted());
        assertEquals(1, counter.countOnConnect());
        assertEquals(0, counter.countOnDenied());
        assertEquals(1, counter.countOnDisconnect());
        assertEquals(0, counter.countOnDownlinkTimeout());
        assertEquals(0, counter.countOnException());
        assertEquals(1, counter.countExtend());
        assertEquals(0, counter.countOnRejected());
        assertEquals(1, counter.countOnRequest());
        assertEquals(1, counter.countOnResponse());
        assertEquals(1, counter.countOnResponseTimeout());
        assertEquals(0, counter.countOnTooManyConnections());
        assertEquals(0, counter.countOnUplinkTimeout());
    }

    /**
     * Test: 20190308002223203722
     *
     * <p>
     * Type: End-To-End Throughput Test.
     * </p>
     *
     * <p>
     * Case: Request denied due to the default pre-check rule.
     * </p>
     *
     * @throws java.lang.Throwable
     */
    @Test
    public void test20190308002223203722 ()
            throws Throwable
    {
        /**
         * This is the server under test.
         *
         * Notice that no request-filter was specified.
         * Therefore, the default DENY ALL filter is in-effect.
         */
        final WebServer engine = WebServer
                .newWebServer()
                .withDefaultSettings()
                .withLogger(logger)
                .withBindAddress(address)
                .withPort(port)
                .build();

        /**
         * Create a web-application based on the server.
         */
        final TestApplication app = new TestApplication(engine);
        app.start();

        /**
         * Send a request to the server.
         * The server will deny the request.
         * Thus, the connection will be closed by the server.
         * No response will be sent to the client.
         */
        final SocketTester tester = new SocketTester(address, port);
        tester.sendln("GET /zero?count=1 HTTP/1.1");
        tester.sendln();
        tester.closed();

        /**
         * Shutdown the server.
         */
        awaitServerShutdown(engine);

        assertEquals(0, counter.countOnAccepted());
        assertEquals(1, counter.countOnConnect());
        assertEquals(1, counter.countOnDenied());
        assertEquals(1, counter.countOnDisconnect());
        assertEquals(0, counter.countOnDownlinkTimeout());
        assertEquals(0, counter.countOnException());
        assertEquals(1, counter.countExtend());
        assertEquals(0, counter.countOnRejected());
        assertEquals(0, counter.countOnRequest());
        assertEquals(0, counter.countOnResponse());
        assertEquals(0, counter.countOnResponseTimeout());
        assertEquals(0, counter.countOnTooManyConnections());
        assertEquals(0, counter.countOnUplinkTimeout());
    }

    /**
     * Test: 20190308002223203750
     *
     * <p>
     * Type: End-To-End Throughput Test.
     * </p>
     *
     * <p>
     * Case: Request denied due to custom pre-check rule.
     * </p>
     *
     * @throws java.lang.Throwable
     */
    @Test
    public void test20190308002223203750 ()
            throws Throwable
    {
        /**
         * This is the server under test.
         *
         * Notice that a custom request filter was added.
         * Any request that contains the word "zero" in the URI will be denied.
         * All other requests will be accepted.
         */
        final WebServer engine = WebServer
                .newWebServer()
                .withDefaultSettings()
                .withLogger(logger)
                .withBindAddress(address)
                .withPort(port)
                .withRequestFilter(RequestFilters.deny(x -> x.getPath().contains("zero")))
                .withAcceptFilter()
                .build();

        final TestApplication app = new TestApplication(engine);
        app.start();

        /**
         * Send a request containing the word "zero" to the server.
         * The server will deny the request and close the connection.
         */
        final SocketTester tester1 = new SocketTester(address, port);
        tester1.sendln("GET /zero?count=17 HTTP/1.1");
        tester1.sendln();
        tester1.closed();

        assertEquals(1, counter.countOnConnect());
        assertEquals(1, counter.countOnDenied());

        /**
         * Send a request to the server that does *not* contain the word "zero".
         * The server will accept the request and generate a response.
         */
        final SocketTester tester2 = new SocketTester(address, port);
        tester2.sendln("GET /sleep?period=1 HTTP/1.1");
        tester2.sendln();
        tester2.recvln("HTTP/1.1 200 OK");
        tester2.recvln("Connection: close");
        tester2.recvln("Content-Type: text/plain");
        tester2.recvln("Content-Length: 14");
        tester2.recvln();
        tester2.recv("SleepComplete\n");
        tester2.closed();

        /**
         * Shutdown the server.
         */
        awaitServerShutdown(engine);

        assertEquals(1, counter.countOnAccepted());
        assertEquals(2, counter.countOnConnect());
        assertEquals(1, counter.countOnDenied());
        assertEquals(2, counter.countOnDisconnect());
        assertEquals(0, counter.countOnDownlinkTimeout());
        assertEquals(0, counter.countOnException());
        assertEquals(2, counter.countExtend());
        assertEquals(0, counter.countOnRejected());
        assertEquals(1, counter.countOnRequest());
        assertEquals(1, counter.countOnResponse());
        assertEquals(0, counter.countOnResponseTimeout());
        assertEquals(0, counter.countOnTooManyConnections());
        assertEquals(0, counter.countOnUplinkTimeout());
    }

    /**
     * Test: 20190308003051428365
     *
     * <p>
     * Type: End-To-End Throughput Test.
     * </p>
     *
     * <p>
     * Case: Request denied due to an exception thrown
     * inside of a custom pre-check rule.
     * </p>
     *
     * @throws java.lang.Throwable
     */
    @Test
    public void test20190308003051428365 ()
            throws Throwable
    {
        /**
         * This is the server under test.
         *
         * Notice that a custom request-filter is specified.
         * The request-filter always throws an ArithmeticException.
         */
        final WebServer engine = WebServer
                .newWebServer()
                .withDefaultSettings()
                .withLogger(logger)
                .withBindAddress(address)
                .withPort(port)
                .withRequestFilter(RequestFilters.accept(x -> (2 / (1 - 1)) == 0)) // div by zero
                .withAcceptFilter()
                .build();

        /**
         * Create a web-application based on the server.
         */
        final TestApplication app = new TestApplication(engine);
        app.start();

        /**
         * Send a request to the server.
         * The request filter will throw an exception.
         * Therefore, the server will deny the request and close the connection.
         */
        final SocketTester tester = new SocketTester(address, port);
        tester.sendln("GET /zero?count=17 HTTP/1.1");
        tester.sendln();
        tester.closed();

        /**
         * Shutdown the server.
         */
        awaitServerShutdown(engine);

        assertEquals(0, counter.countOnAccepted());
        assertEquals(1, counter.countOnConnect());
        assertEquals(1, counter.countOnDenied());
        assertEquals(1, counter.countOnDisconnect());
        assertEquals(0, counter.countOnDownlinkTimeout());
        assertEquals(1, counter.countOnException());
        assertEquals(1, counter.countExtend());
        assertEquals(0, counter.countOnRejected());
        assertEquals(0, counter.countOnRequest());
        assertEquals(0, counter.countOnResponse());
        assertEquals(0, counter.countOnResponseTimeout());
        assertEquals(0, counter.countOnTooManyConnections());
        assertEquals(0, counter.countOnUplinkTimeout());
    }

    /**
     * Test: 20190308002223203777
     *
     * <p>
     * Type: End-To-End Throughput Test.
     * </p>
     *
     * <p>
     * Case: Request rejected due to custom pre-check rule.
     * </p>
     *
     * @throws java.lang.Throwable
     */
    @Test
    public void test20190308002223203777 ()
            throws Throwable
    {
        /**
         * This is the server under test.
         *
         * Notice that a custom request filter is specified.
         * If the URI of a request contains the word "zero",
         * then a default (403) response will be sent back.
         */
        final WebServer engine = WebServer
                .newWebServer()
                .withDefaultSettings()
                .withLogger(logger)
                .withBindAddress(address)
                .withPort(port)
                .withRequestFilter(RequestFilters.reject(403, x -> x.getPath().contains("zero")))
                .withAcceptFilter()
                .build();

        /**
         * Create a web-application based on the server.
         */
        final TestApplication app = new TestApplication(engine);
        app.start();

        /**
         * Send a request to the server, which contains the word "zero".
         * The server will reject the request and send back a default response.
         */
        final SocketTester tester1 = new SocketTester(address, port);
        tester1.sendln("GET /zero?count=1 HTTP/1.1");
        tester1.sendln();
        tester1.recvln("HTTP/1.1 403 Forbidden");
        tester1.recvln("Connection: close");
        tester1.recvln("Content-Type: text/html");
        tester1.recvln("Content-Length: 74");
        tester1.recvln();
        tester1.recvln("<head> <meta http-equiv=\"refresh\" content=\"5; URL=\"/403.html\" /> </head>");
        tester1.closed();

        /**
         * Shutdown the server.
         */
        awaitServerShutdown(engine);

        assertEquals(0, counter.countOnAccepted());
        assertEquals(1, counter.countOnConnect());
        assertEquals(0, counter.countOnDenied());
        assertEquals(1, counter.countOnDisconnect());
        assertEquals(0, counter.countOnDownlinkTimeout());
        assertEquals(0, counter.countOnException());
        assertEquals(1, counter.countExtend());
        assertEquals(1, counter.countOnRejected());
        assertEquals(0, counter.countOnRequest());
        assertEquals(1, counter.countOnResponse());
        assertEquals(0, counter.countOnResponseTimeout());
        assertEquals(0, counter.countOnTooManyConnections());
        assertEquals(0, counter.countOnUplinkTimeout());
    }

    /**
     * Test: 20190308002223203806
     *
     * <p>
     * Type: End-To-End Throughput Test.
     * </p>
     *
     * <p>
     * Case: Sequential Requests.
     * </p>
     *
     * @throws java.lang.Throwable
     */
    @Test
    public void test20190308002223203806 ()
            throws Throwable
    {
        /**
         * This is the server under test.
         */
        final WebServer engine = WebServer
                .newWebServer()
                .withDefaultSettings()
                .withLogger(logger)
                .withBindAddress(address)
                .withPort(port)
                .withAcceptFilter()
                .build();

        /**
         * Create a web-application based on the server.
         */
        final TestApplication app = new TestApplication(engine);
        app.start();

        /**
         * Sequentially, send requests to the server and receive the responses.
         */
        final int requestCount = 100;
        for (int i = 0; i < requestCount; i++)
        {
            final SocketTester tester = new SocketTester(address, port);
            tester.sendln("GET /zero?count=3 HTTP/1.1");
            tester.sendln();
            tester.recvln("HTTP/1.1 200 OK");
            tester.recvln("Connection: close");
            tester.recvln("Content-Type: text/plain");
            tester.recvln("Content-Length: 3");
            tester.recvln();
            tester.recv("000"); // /zero?count=3
            tester.closed();
        }

        /**
         * Shutdown the server.
         */
        awaitServerShutdown(engine);

        assertEquals(requestCount, counter.countOnAccepted());
        assertEquals(requestCount, counter.countOnConnect());
        assertEquals(0, counter.countOnDenied());
        assertEquals(requestCount, counter.countOnDisconnect());
        assertEquals(0, counter.countOnDownlinkTimeout());
        assertEquals(0, counter.countOnException());
        assertEquals(requestCount, counter.countExtend());
        assertEquals(0, counter.countOnRejected());
        assertEquals(requestCount, counter.countOnRequest());
        assertEquals(requestCount, counter.countOnResponse());
        assertEquals(0, counter.countOnResponseTimeout());
        assertEquals(0, counter.countOnTooManyConnections());
        assertEquals(0, counter.countOnUplinkTimeout());
    }

    /**
     * Test: 20190308002223203832
     *
     * <p>
     * Type: End-To-End Throughput Test.
     * </p>
     *
     * <p>
     * Case: Parallel Requests.
     * </p>
     *
     * @throws java.lang.Throwable
     */
    @Test
    public void test20190308002223203832 ()
            throws Throwable
    {
        final ExecutorService service = Executors.newFixedThreadPool(16);

        /**
         * This is the server under test.
         */
        final WebServer engine = WebServer
                .newWebServer()
                .withDefaultSettings()
                .withLogger(logger)
                .withBindAddress(address)
                .withPort(port)
                .withAcceptFilter()
                .build();

        /**
         * Create a web-application based on the server.
         */
        final TestApplication app = new TestApplication(engine);
        app.start();

        /**
         * This latch will be used to wait for the requests to complete.
         */
        final int requestCount = 1280;
        final CountDownLatch latch = new CountDownLatch(requestCount);

        /**
         * Generic HTTP Request.
         */
        final Runnable client = () ->
        {
            try
            {
                final SocketTester tester = new SocketTester(address, port);
                tester.sendln("GET /zero?count=3 HTTP/1.1");
                tester.sendln();
                tester.recvln("HTTP/1.1 200 OK");
                tester.recvln("Connection: close");
                tester.recvln("Content-Type: text/plain");
                tester.recvln("Content-Length: 3");
                tester.recvln();
                tester.recv("000");
                tester.closed();
            }
            catch (Throwable ex)
            {
                fail(ex.getMessage());
            }
            finally
            {
                latch.countDown();
            }
        };

        /**
         * Submit all of the requests to the executor.
         * Some of the requests will be processed concurrently,
         * because the executor is multi-threaded .
         */
        for (int i = 0; i < requestCount; i++)
        {
            service.submit(client);
        }

        /**
         * Wait for all the request and responses to be processed.
         */
        latch.await();

        /**
         * Shutdown the server.
         */
        service.shutdown();
        awaitServerShutdown(engine);

        assertEquals(requestCount, counter.countOnAccepted());
        assertEquals(requestCount, counter.countOnConnect());
        assertEquals(0, counter.countOnDenied());
        assertEquals(requestCount, counter.countOnDisconnect());
        assertEquals(0, counter.countOnDownlinkTimeout());
        assertEquals(0, counter.countOnException());
        assertEquals(requestCount, counter.countExtend());
        assertEquals(0, counter.countOnRejected());
        assertEquals(requestCount, counter.countOnRequest());
        assertEquals(requestCount, counter.countOnResponse());
        assertEquals(0, counter.countOnResponseTimeout());
        assertEquals(0, counter.countOnTooManyConnections());
        assertEquals(0, counter.countOnUplinkTimeout());
    }

    /**
     * Test: 20190308003457842738
     *
     * <p>
     * Type: End-To-End Throughput Test.
     * </p>
     *
     * <p>
     * Case: Initial Line Too Large.
     * </p>
     *
     * @throws java.lang.InterruptedException
     */
    @Test
    public void test20190308003457842738 ()
            throws Throwable
    {

        /**
         * This is the server under test.
         */
        final int maxInitialLineSize = 1024;
        final WebServer engine = WebServer
                .newWebServer()
                .withDefaultSettings()
                .withLogger(logger)
                .withBindAddress(address)
                .withPort(port)
                .withAcceptFilter()
                .withMaxInitialLineSize(maxInitialLineSize)
                .build();

        /**
         * Create a web-application based on the server.
         */
        final TestApplication app = new TestApplication(engine);
        app.start();

        /**
         * Send a request to the server, where the initial line is too long.
         */
        final String tooLong = Strings.repeat("1", maxInitialLineSize);
        final SocketTester tester = new SocketTester(address, port);
        tester.sendln(String.format("GET /zero?count=%s HTTP/1.1", tooLong));
        tester.sendln();
        tester.recvln("HTTP/1.1 400 Bad Request");
        tester.recvln("Connection: close");
        tester.recvln("Content-Type: text/html");
        tester.recvln("Content-Length: 74");
        tester.recvln();
        tester.recvln("<head> <meta http-equiv=\"refresh\" content=\"5; URL=\"/400.html\" /> </head>");
        tester.closed();

        /**
         * Shutdown the server.
         */
        awaitServerShutdown(engine);

        assertEquals(0, counter.countOnAccepted());
        assertEquals(1, counter.countOnConnect());
        assertEquals(0, counter.countOnDenied());
        assertEquals(1, counter.countOnDisconnect());
        assertEquals(0, counter.countOnDownlinkTimeout());
        assertEquals(0, counter.countOnException());
        assertEquals(1, counter.countExtend());
        assertEquals(0, counter.countOnRejected());
        assertEquals(0, counter.countOnRequest());
        assertEquals(1, counter.countOnResponse());
        assertEquals(0, counter.countOnResponseTimeout());
        assertEquals(0, counter.countOnTooManyConnections());
        assertEquals(0, counter.countOnUplinkTimeout());
    }

    /**
     * Test: 20190308003457842710
     *
     * <p>
     * Type: End-To-End Throughput Test.
     * </p>
     *
     * <p>
     * Case: Request Too Large.
     * </p>
     *
     * @throws java.lang.Throwable
     */
    @Test
    public void test20190308003457842710 ()
            throws Throwable
    {
        fail();

        /**
         * This is the server under test.
         */
        final int maxRequestSize = 1024;
        final WebServer engine = WebServer
                .newWebServer()
                .withDefaultSettings()
                .withLogger(logger)
                .withBindAddress(address)
                .withPort(port)
                .withAcceptFilter()
                .withMaxRequestSize(maxRequestSize)
                .build();

        /**
         * Create a web-application based on the server.
         */
        final TestApplication app = new TestApplication(engine);
        app.start();

        /**
         * Send a request to the server that is too large.
         * The server will respond with a default response.
         */
        final String tooLong = Strings.repeat("1", 2048);
        final SocketTester tester = new SocketTester(address, port);
        tester.sendln(String.format("PUT /print HTTP/1.1"));
        tester.sendln("Content-Length: 2048");
        tester.sendln();
        tester.sendln(tooLong);
        tester.recvln("HTTP/1.1 413 Request Entity Too Large");
        tester.recvln("content-length: 0");
        tester.exhaust();
        tester.closed();

        /**
         * Shutdown the server.
         */
        awaitServerShutdown(engine);

        assertEquals(0, counter.countOnAccepted());
        assertEquals(1, counter.countOnConnect());
        assertEquals(0, counter.countOnDenied());
        assertEquals(1, counter.countOnDisconnect());
        assertEquals(0, counter.countOnDownlinkTimeout());
        assertEquals(0, counter.countOnException());
        assertEquals(1, counter.countExtend());
        assertEquals(0, counter.countOnRejected());
        assertEquals(0, counter.countOnRequest());
        assertEquals(1, counter.countOnResponse());
        assertEquals(0, counter.countOnResponseTimeout());
        assertEquals(0, counter.countOnTooManyConnections());
        assertEquals(0, counter.countOnUplinkTimeout());
    }

    /**
     * Test: 20190308003654044008
     *
     * <p>
     * Type: End-To-End Throughput Test.
     * </p>
     *
     * <p>
     * Case: Headers Too Large.
     * </p>
     *
     * @throws java.lang.Throwable
     */
    @Test
    public void test20190308003654044008 ()
            throws Throwable
    {
        fail();

        final WebServer engine = WebServer
                .newWebServer()
                .withDefaultSettings()
                .withBindAddress(address)
                .withPort(port)
                .withAcceptFilter()
                .withMaxHeaderSize(1024)
                .build();

        final TestApplication app = new TestApplication(engine);
        app.start();

        final String tooLong = Strings.repeat("1", 2048);

        final SocketTester tester = new SocketTester(address, port);
        tester.sendln("GET /zero?count=3 HTTP/1.1");
        tester.sendln("X: " + tooLong);
        tester.sendln("Y: " + tooLong);
        tester.sendln();
        tester.readAndPrint();
        tester.recvln("HTTP/1.1 400 Bad Request");
        tester.recvln("Connection: close");
        tester.recvln("Content-Type: text/html");
        tester.recvln("Content-Length: 74");
        tester.recvln();
        tester.recvln("<head> <meta http-equiv=\"refresh\" content=\"5; URL=\"/400.html\" /> </head>");
        tester.closed();
    }

    /**
     * Test: 20190308004116128009
     *
     * <p>
     * Type: End-To-End Throughput Test.
     * </p>
     *
     * <p>
     * Case: Soft Connection Limit Exceeded.
     * </p>
     *
     * @throws java.lang.Throwable
     */
    @Test
    public void test20190308004116128009 ()
            throws Throwable
    {
        /**
         * This is the server under test.
         *
         * Notice that the soft-connection-limit is zero;
         * therefore, no connections will be accepted.
         */
        final WebServer engine = WebServer
                .newWebServer()
                .withDefaultSettings()
                .withLogger(logger)
                .withBindAddress(address)
                .withPort(port)
                .withAcceptFilter()
                .withSoftConnectionLimit(0)
                .withHardConnectionLimit(100)
                .build();

        /**
         * Create a web-application based on the server.
         */
        final TestApplication app = new TestApplication(engine);
        app.start();

        /**
         * Send a request to the server.
         * The server will respond with a default response,
         * because one connection is greater than the zero allowed.
         */
        final SocketTester tester = new SocketTester(address, port);
        tester.sendln("GET /zero?count=3 HTTP/1.1");
        tester.sendln();
        tester.recvln("HTTP/1.1 429 Too Many Requests");
        tester.recvln("Connection: close");
        tester.recvln("Content-Type: text/html");
        tester.recvln("Content-Length: 74");
        tester.recvln();
        tester.recvln("<head> <meta http-equiv=\"refresh\" content=\"5; URL=\"/429.html\" /> </head>");
        tester.closed();

        /**
         * Shutdown the server.
         */
        awaitServerShutdown(engine);

        assertEquals(0, counter.countOnAccepted());
        assertEquals(1, counter.countOnConnect());
        assertEquals(0, counter.countOnDenied());
        assertEquals(1, counter.countOnDisconnect());
        assertEquals(0, counter.countOnDownlinkTimeout());
        assertEquals(0, counter.countOnException());
        assertEquals(1, counter.countExtend());
        assertEquals(0, counter.countOnRejected());
        assertEquals(0, counter.countOnRequest());
        assertEquals(1, counter.countOnResponse());
        assertEquals(0, counter.countOnResponseTimeout());
        assertEquals(1, counter.countOnTooManyConnections());
        assertEquals(0, counter.countOnUplinkTimeout());
    }

    /**
     * Test: 20190308004116128086
     *
     * <p>
     * Type: End-To-End Throughput Test.
     * </p>
     *
     * <p>
     * Case: Hard Connection Limit Exceeded.
     * </p>
     *
     * @throws java.lang.Throwable
     */
    @Test
    public void test20190308004116128086 ()
            throws Throwable
    {
        /**
         * This is the server under test.
         *
         * Notice that that hard-connection-limit is zero;
         * therefore, no connections will be allowed.
         */
        final WebServer engine = WebServer
                .newWebServer()
                .withDefaultSettings()
                .withLogger(logger)
                .withBindAddress(address)
                .withPort(port)
                .withAcceptFilter()
                .withSoftConnectionLimit(100)
                .withHardConnectionLimit(0)
                .build();

        /**
         * Create a web-application based on the server.
         */
        final TestApplication app = new TestApplication(engine);
        app.start();

        /**
         * Send a request to the server.
         * The server will not accept the connection, at all,
         * because one connection is greater than the zero allowed.
         */
        final SocketTester tester = new SocketTester(address, port);
        tester.sendln("GET /zero?count=3 HTTP/1.1");
        tester.sendln();

        try
        {
            tester.closed();
            fail();
        }
        catch (SocketException ex)
        {
            /**
             * Since the connection was never accepted,
             * then attempting to detect a closed socket
             * will result in a connection-reset exception.
             */
            assertTrue(ex.getMessage().toLowerCase().contains("connection reset"));
        }

        /**
         * Shutdown the server.
         */
        awaitServerShutdown(engine);

        assertEquals(0, counter.countOnAccepted());
        assertEquals(0, counter.countOnConnect());
        assertEquals(0, counter.countOnDenied());
        assertEquals(0, counter.countOnDisconnect());
        assertEquals(0, counter.countOnDownlinkTimeout());
        assertEquals(0, counter.countOnException());
        assertEquals(1, counter.countExtend());
        assertEquals(0, counter.countOnRejected());
        assertEquals(0, counter.countOnRequest());
        assertEquals(0, counter.countOnResponse());
        assertEquals(0, counter.countOnResponseTimeout());
        assertEquals(1, counter.countOnTooManyConnections());
        assertEquals(0, counter.countOnUplinkTimeout());
    }

    /**
     * Test: 20190308003457842596
     *
     * <p>
     * Type: End-To-End Throughput Test.
     * </p>
     *
     * <p>
     * Case: Compressed Request.
     * </p>
     *
     * @throws java.lang.Throwable
     */
    @Test
    public void test20190308003457842596 ()
            throws Throwable
    {
        /**
         * This is the server under test.
         */
        final WebServer engine = WebServer
                .newWebServer()
                .withDefaultSettings()
                .withLogger(logger)
                .withBindAddress(address)
                .withPort(port)
                .withAcceptFilter()
                .build();

        /**
         * Create a web-application based on the server.
         */
        final TestApplication app = new TestApplication(engine);
        app.start();

        /**
         * Send a GZIP compressed request to the server,
         * where the compressed data is the string "HelloWorld!".
         */
        final SocketTester tester = new SocketTester(address, port);
        tester.sendln("POST /print HTTP/1.1");
        tester.sendln("Content-Encoding: gzip");
        tester.sendln("Content-Length: 30");
        tester.sendln();
        tester.send(0x1f, 0x8b, 0x08, 0x00, 0xc8, 0x9a,
                    0xae, 0x5c, 0x00, 0x03, 0xf3, 0x48,
                    0xcd, 0xc9, 0xc9, 0x0f, 0xcf, 0x2f,
                    0xca, 0x49, 0x51, 0x04, 0x00, 0x47,
                    0x41, 0xc5, 0xb, 0x70, 0xb0, 0x00);

        /**
         * The server will send back a response containing
         * the request body in uncompressed form.
         */
        tester.recvln("HTTP/1.1 200 OK");
        tester.recvln("Connection: close");
        tester.recvln("Content-Type: text/plain");
        tester.recvln("Content-Length: 11");
        tester.recvln();
        tester.recv("HelloWorld!");
        tester.closed();

        /**
         * Shutdown the server.
         */
        awaitServerShutdown(engine);

        assertEquals(1, counter.countOnAccepted());
        assertEquals(1, counter.countOnConnect());
        assertEquals(0, counter.countOnDenied());
        assertEquals(1, counter.countOnDisconnect());
        assertEquals(0, counter.countOnDownlinkTimeout());
        assertEquals(0, counter.countOnException());
        assertEquals(1, counter.countExtend());
        assertEquals(0, counter.countOnRejected());
        assertEquals(1, counter.countOnRequest());
        assertEquals(1, counter.countOnResponse());
        assertEquals(0, counter.countOnResponseTimeout());
        assertEquals(0, counter.countOnTooManyConnections());
        assertEquals(0, counter.countOnUplinkTimeout());
    }

    /**
     * Test: 20190308003457842680
     *
     * <p>
     * Type: End-To-End Throughput Test.
     * </p>
     *
     * <p>
     * Case: Compressed Response.
     * </p>
     *
     * @throws java.lang.Throwable
     */
    @Test
    public void test20190308003457842680 ()
            throws Throwable
    {
        /**
         * This is the server under test.
         *
         * Notice that the compression-threshold is zero;
         * therefore, all responses will be compressed.
         */
        final WebServer engine = WebServer
                .newWebServer()
                .withDefaultSettings()
                .withLogger(logger)
                .withBindAddress(address)
                .withPort(port)
                .withAcceptFilter()
                .withCompressionThreshold(0)
                .withCompressionLevel(9)
                .build();

        /**
         * Create a web-application based on the server.
         */
        final TestApplication app = new TestApplication(engine);
        app.start();

        /**
         * Send a request to the server, which advertises that
         * we are able to accept GZIP-encoded responses.
         */
        final SocketTester tester = new SocketTester(address, port);
        tester.sendln("GET /zero?count=1000 HTTP/1.1");
        tester.sendln("Accept-Encoding: gzip");
        tester.sendln();

        /**
         * The server will send a compressed response back to us.
         *
         * Note: This test is somewhat brittle, since the expected
         * size of the response is hard-coded, for simplicity.
         * This may cause the test to break due to library differences.
         * If so, this test will need to be improved. A task for later.
         */
        tester.recvln("HTTP/1.1 200 OK");
        tester.recvln("Connection: close");
        tester.recvln("Content-Type: text/plain");
        tester.recvln("content-encoding: gzip");
        tester.recvln("content-length: 35");
        tester.recvln();
        final long length = tester.exhaust();
        tester.closed();

        /**
         * The length of the compressed response equals
         * the content-length header from the server.
         */
        assertEquals(35, length);

        /**
         * Shutdown the server.
         */
        awaitServerShutdown(engine);

        assertEquals(1, counter.countOnAccepted());
        assertEquals(1, counter.countOnConnect());
        assertEquals(0, counter.countOnDenied());
        assertEquals(1, counter.countOnDisconnect());
        assertEquals(0, counter.countOnDownlinkTimeout());
        assertEquals(0, counter.countOnException());
        assertEquals(1, counter.countExtend());
        assertEquals(0, counter.countOnRejected());
        assertEquals(1, counter.countOnRequest());
        assertEquals(1, counter.countOnResponse());
        assertEquals(0, counter.countOnResponseTimeout());
        assertEquals(0, counter.countOnTooManyConnections());
        assertEquals(0, counter.countOnUplinkTimeout());
    }

    /**
     * Test: 20190308003051428434
     *
     * <p>
     * Type: End-To-End Throughput Test.
     * </p>
     *
     * <p>
     * Case: Successful Chunked Request with Content-Length Header.
     * </p>
     *
     * @throws java.lang.Throwable
     */
    @Test
    public void test20190308003051428434 ()
            throws Throwable
    {
        /**
         * This is the server under test.
         */
        final WebServer engine = WebServer
                .newWebServer()
                .withDefaultSettings()
                .withLogger(logger)
                .withBindAddress(address)
                .withPort(port)
                .withAcceptFilter()
                .build();

        /**
         * Create a web-application based on the server.
         */
        final TestApplication app = new TestApplication(engine);
        app.start();

        /**
         * Send a request to the server using a chunked request.
         */
        final SocketTester tester = new SocketTester(address, port);
        tester.sendln("GET /print HTTP/1.1");
        tester.sendln("Transfer-Encoding: chunked");
        tester.sendln("Content-Length: 9");
        tester.sendln();
        tester.sendln("5"); // hex(len("Hello"))
        tester.sendln("Hello");
        tester.sendln("4"); // hex(len("Mars"))
        tester.sendln("Mars");
        tester.sendln("0"); // hex(0)
        tester.sendln();

        /**
         * The server will send a non-chunked response back to us.
         */
        tester.recvln("HTTP/1.1 200 OK");
        tester.recvln("Connection: close");
        tester.recvln("Content-Type: text/plain");
        tester.recvln("Content-Length: 9");
        tester.recvln();
        tester.recv("HelloMars");
        tester.closed();

        /**
         * Shutdown the server.
         */
        awaitServerShutdown(engine);

        assertEquals(1, counter.countOnAccepted());
        assertEquals(1, counter.countOnConnect());
        assertEquals(0, counter.countOnDenied());
        assertEquals(1, counter.countOnDisconnect());
        assertEquals(0, counter.countOnDownlinkTimeout());
        assertEquals(0, counter.countOnException());
        assertEquals(1, counter.countExtend());
        assertEquals(0, counter.countOnRejected());
        assertEquals(1, counter.countOnRequest());
        assertEquals(1, counter.countOnResponse());
        assertEquals(0, counter.countOnResponseTimeout());
        assertEquals(0, counter.countOnTooManyConnections());
        assertEquals(0, counter.countOnUplinkTimeout());
    }

    /**
     * Test: 20190414114130386776
     *
     * <p>
     * Type: End-To-End Throughput Test.
     * </p>
     *
     * <p>
     * Case: Successful Chunked Request with Content-Length Header.
     * </p>
     *
     * @throws java.lang.Throwable
     */
    @Test
    public void test20190414114130386776 ()
            throws Throwable
    {
        /**
         * This is the server under test.
         */
        final WebServer engine = WebServer
                .newWebServer()
                .withDefaultSettings()
                .withLogger(logger)
                .withBindAddress(address)
                .withPort(port)
                .withAcceptFilter()
                .build();

        /**
         * Create a web-application based on the server.
         */
        final TestApplication app = new TestApplication(engine);
        app.start();

        /**
         * Send a request to the server using a chunked request.
         */
        final SocketTester tester = new SocketTester(address, port);
        tester.sendln("GET /print HTTP/1.1");
        tester.sendln("Transfer-Encoding: chunked");
        tester.sendln();
        tester.sendln("5"); // hex(len("Hello"))
        tester.sendln("Hello");
        tester.sendln("4"); // hex(len("Mars"))
        tester.sendln("Mars");
        tester.sendln("0"); // hex(0)
        tester.sendln();

        /**
         * The server will send a non-chunked response back to us.
         */
        tester.recvln("HTTP/1.1 200 OK");
        tester.recvln("Connection: close");
        tester.recvln("Content-Type: text/plain");
        tester.recvln("Content-Length: 9");
        tester.recvln();
        tester.recv("HelloMars");
        tester.closed();

        /**
         * Shutdown the server.
         */
        awaitServerShutdown(engine);

        assertEquals(1, counter.countOnAccepted());
        assertEquals(1, counter.countOnConnect());
        assertEquals(0, counter.countOnDenied());
        assertEquals(1, counter.countOnDisconnect());
        assertEquals(0, counter.countOnDownlinkTimeout());
        assertEquals(0, counter.countOnException());
        assertEquals(1, counter.countExtend());
        assertEquals(0, counter.countOnRejected());
        assertEquals(1, counter.countOnRequest());
        assertEquals(1, counter.countOnResponse());
        assertEquals(0, counter.countOnResponseTimeout());
        assertEquals(0, counter.countOnTooManyConnections());
        assertEquals(0, counter.countOnUplinkTimeout());
    }

    /**
     * Test: 20190308003311329885
     *
     * <p>
     * Type: End-To-End Throughput Test.
     * </p>
     *
     * <p>
     * Case: Chunked Request Unrecognized Expectation Code (Expectation Failed).
     * </p>
     *
     * @throws java.lang.Throwable
     */
    @Test
    public void test20190308003311329885 ()
            throws Throwable
    {
        /**
         * This is the server under test.
         */
        final WebServer engine = WebServer
                .newWebServer()
                .withDefaultSettings()
                .withLogger(logger)
                .withBindAddress(address)
                .withPort(port)
                .withAcceptFilter()
                .build();

        /**
         * Create a web-application based on the server.
         */
        final TestApplication app = new TestApplication(engine);
        app.start();

        /**
         * Notice that the expect is "101-continue" is ficticious.
         * Normally, we would send s "100-continue".
         * However, Netty will only respond with (HTTP 417),
         * if the expectation code is unrecognized.
         */
        final SocketTester tester = new SocketTester(address, port);
        tester.sendln("GET /print HTTP/1.1");
        tester.sendln("Transfer-Encoding: chunked");
        tester.sendln("Expect: 101-continue");
        tester.sendln("Content-Length: 3");
        tester.sendln();

        /**
         * The server will send a response forbidding the request.
         */
        tester.recvln("HTTP/1.1 417 Expectation Failed");
        tester.recvln("content-length: 0");
        tester.exhaust();
        tester.closed();

        /**
         * Shutdown the server.
         */
        awaitServerShutdown(engine);

        assertEquals(1, counter.countOnAccepted());
        assertEquals(1, counter.countOnConnect());
        assertEquals(0, counter.countOnDenied());
        assertEquals(1, counter.countOnDisconnect());
        assertEquals(0, counter.countOnDownlinkTimeout());
        assertEquals(0, counter.countOnException());
        assertEquals(1, counter.countExtend());
        assertEquals(0, counter.countOnRejected());
        assertEquals(0, counter.countOnRequest());
        assertEquals(0, counter.countOnResponse());
        assertEquals(0, counter.countOnResponseTimeout());
        assertEquals(0, counter.countOnTooManyConnections());
        assertEquals(0, counter.countOnUplinkTimeout());
    }

    /**
     * Test: 20190414110651419646
     *
     * <p>
     * Type: End-To-End Throughput Test.
     * </p>
     *
     * <p>
     * Case: Chunked Request Too Large (Expectation Failed).
     * </p>
     *
     * @throws java.lang.Throwable
     */
    @Test
    public void test20190414110651419646 ()
            throws Throwable
    {
        /**
         * This is the server under test.
         */
        final WebServer engine = WebServer
                .newWebServer()
                .withDefaultSettings()
                .withLogger(logger)
                .withBindAddress(address)
                .withPort(port)
                .withAcceptFilter()
                .withMaxRequestSize(1024)
                .build();

        /**
         * Create a web-application based on the server.
         */
        final TestApplication app = new TestApplication(engine);
        app.start();

        /**
         * Send a request to the server, which indicates that
         * the body of the message will be larger than allowed.
         */
        final SocketTester tester = new SocketTester(address, port);
        tester.sendln("GET /print HTTP/1.1");
        tester.sendln("Transfer-Encoding: chunked");
        tester.sendln("Expect: 100-continue");
        tester.sendln("Content-Length: 1025"); // Max Request Size + 1
        tester.sendln();

        /**
         * The server will send a response forbidding the request.
         */
        tester.recvln("HTTP/1.1 413 Request Entity Too Large");
        tester.recvln("content-length: 0");
        tester.exhaust();
        tester.closed();

        /**
         * Shutdown the server.
         */
        awaitServerShutdown(engine);

        assertEquals(1, counter.countOnAccepted());
        assertEquals(1, counter.countOnConnect());
        assertEquals(0, counter.countOnDenied());
        assertEquals(1, counter.countOnDisconnect());
        assertEquals(0, counter.countOnDownlinkTimeout());
        assertEquals(0, counter.countOnException());
        assertEquals(1, counter.countExtend());
        assertEquals(0, counter.countOnRejected());
        assertEquals(0, counter.countOnRequest());
        assertEquals(0, counter.countOnResponse());
        assertEquals(0, counter.countOnResponseTimeout());
        assertEquals(0, counter.countOnTooManyConnections());
        assertEquals(0, counter.countOnUplinkTimeout());
    }

    /**
     * Test: 20190414195205239061
     *
     * <p>
     * Type: End-To-End Throughput Test.
     * </p>
     *
     * <p>
     * Case: Secure Sockets using Self Signed Certificate.
     * </p>
     */
    @Test
    public void test20190414195205239061 ()
    {
        System.out.println("Test: 20190414195205239061");
        fail();
    }

    /**
     * Test: 20190414195205239414
     *
     * <p>
     * Type: End-To-End Throughput Test.
     * </p>
     *
     * <p>
     * Case: Extraneous bytes sent after the syntactic end of the HTTP request.
     * </p>
     */
    @Test
    public void test20190414195205239414 ()
    {
        System.out.println("Test: 20190414195205239414");
        fail();
    }

    /**
     * Test: 20190414195205239451
     *
     * <p>
     * Type: End-To-End Throughput Test.
     * </p>
     *
     * <p>
     * Case: Customized Default Responses.
     * </p>
     */
    @Test
    public void test20190414195205239451 ()
    {
        System.out.println("Test: 20190414195205239451");
        fail();
    }
}
