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

import com.mackenziehigh.socius.web.server.loggers.BaseWebLogger;
import com.mackenziehigh.socius.web.server.loggers.ChainWebLogger;
import com.mackenziehigh.socius.web.server.loggers.CountingWebLogger;
import com.mackenziehigh.socius.web.server.loggers.DefaultWebLogger;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class WebServerTest
{
    private final CountingWebLogger counter = CountingWebLogger.create();

    private final DefaultWebLogger slogger = new DefaultWebLogger();

    private final ChainWebLogger logger = new ChainWebLogger(counter, slogger);

//    /**
//     * Test: 20190303101911066030
//     *
//     * <p>
//     * Method: <code>withDefaultSettings</code>
//     * </p>
//     *
//     * <p>
//     * Case: Verify that the correct defaults are applied.
//     * </p>
//     */
//    @Test
//    public void test20190303101911066030 ()
//    {
//        final WebServer server = WebServer.newWebServer().withDefaultSettings().build();
//
//        assertEquals(WebServer.DEFAULT_SERVER_NAME, server.getServerName());
//        assertEquals(WebServer.DEFAULT_REPLY_TO, server.getReplyTo());
//        assertEquals(WebServer.DEFAULT_BIND_ADDRESS, server.getBindAddress());
//        assertEquals(WebServer.DEFAULT_PORT, server.getPort());
//        assertEquals(WebServer.DEFAULT_MAX_MESSAGES_PER_READ, server.getMaxMessagesPerRead());
//        assertEquals(WebServer.DEFAULT_RECV_ALLOCATOR_INITIAL, server.getRecvBufferInitialSize());
//        assertEquals(WebServer.DEFAULT_RECV_ALLOCATOR_MAX, server.getRecvBufferMaxSize());
//        assertEquals(WebServer.DEFAULT_RECV_ALLOCATOR_MIN, server.getRecvBufferMinSize());
//        assertEquals(WebServer.DEFAULT_SOFT_CONNECTION_LIMIT, server.getSoftConnectionLimit());
//        assertEquals(WebServer.DEFAULT_HARD_CONNECTION_LIMIT, server.getHardConnectionLimit());
//        assertEquals(WebServer.DEFAULT_MAX_PAUSE_TIME, server.getMaxPauseTime());
//        assertEquals(WebServer.DEFAULT_MAX_REQUEST_SIZE, server.getMaxRequestSize());
//        assertEquals(WebServer.DEFAULT_MAX_INITIAL_LINE_SIZE, server.getMaxInitialLineSize());
//        assertEquals(WebServer.DEFAULT_MAX_HEADER_SIZE, server.getMaxHeaderSize());
//        assertEquals(WebServer.DEFAULT_COMPRESSION_LEVEL, server.getCompressionLevel());
//        assertEquals(WebServer.DEFAULT_COMPRESSION_WINDOW_BITS, server.getCompressionWindowBits());
//        assertEquals(WebServer.DEFAULT_COMPRESSION_MEMORY_LEVEL, server.getCompressionMemoryLevel());
//        assertEquals(WebServer.DEFAULT_COMPRESSION_THRESHOLD, server.getCompressionThreshold());
//        assertEquals(WebServer.DEFAULT_SLOW_UPLINK_TIMEOUT, server.getUplinkTimeout());
//        assertEquals(WebServer.DEFAULT_SLOW_DOWNLINK_TIMEOUT, server.getDownlinkTimeout());
//        assertEquals(WebServer.DEFAULT_RESPONSE_TIMEOUT, server.getResponseTimeout());
//    }
//
//    /**
//     * Test: 20190303102051831715
//     *
//     * <p>
//     * Case: Verify that the builder sets all settings correctly.
//     * </p>
//     */
//    @Test
//    public void test20190303102051831715 ()
//    {
//        final WebServer server = WebServer
//                .newWebServer()
//                .withServerName("Voyager")
//                .withReplyTo("Earth")
//                .withBindAddress("127.0.0.17")
//                .withPort(8008)
//                .withMaxMessagesPerRead(23)
//                .withRecvBufferAllocator(31, 33, 37)
//                .withSoftConnectionLimit(41)
//                .withHardConnectionLimit(43)
//                .withMaxPauseTime(Duration.ofSeconds(57))
//                .withMaxRequestSize(61)
//                .withMaxInitialLineSize(63)
//                .withMaxHeaderSize(67)
//                .withCompressionLevel(WebServer.DEFAULT_COMPRESSION_LEVEL - 2)
//                .withCompressionWindowBits(WebServer.DEFAULT_COMPRESSION_WINDOW_BITS - 2)
//                .withCompressionMemoryLevel(WebServer.DEFAULT_COMPRESSION_MEMORY_LEVEL - 2)
//                .withCompressionThreshold(71)
//                .withUplinkTimeout(Duration.ofSeconds(73))
//                .withDownlinkTimeout(Duration.ofSeconds(79))
//                .withResponseTimeout(Duration.ofSeconds(83))
//                .withUnsecureSockets()
//                .build();
//
//        assertEquals("Voyager", server.getServerName());
//        assertEquals("Earth", server.getReplyTo());
//        assertEquals("127.0.0.17", server.getBindAddress());
//        assertEquals(8008, server.getPort());
//        assertEquals(23, server.getMaxMessagesPerRead());
//        assertEquals(31, server.getRecvBufferMinSize());
//        assertEquals(33, server.getRecvBufferInitialSize());
//        assertEquals(37, server.getRecvBufferMaxSize());
//        assertEquals(41, server.getSoftConnectionLimit());
//        assertEquals(43, server.getHardConnectionLimit());
//        assertEquals(Duration.ofSeconds(57), server.getMaxPauseTime());
//        assertEquals(61, server.getMaxRequestSize());
//        assertEquals(63, server.getMaxInitialLineSize());
//        assertEquals(67, server.getMaxHeaderSize());
//        assertEquals(4, server.getCompressionLevel());
//        assertEquals(13, server.getCompressionWindowBits());
//        assertEquals(6, server.getCompressionMemoryLevel());
//        assertEquals(71, server.getCompressionThreshold());
//        assertEquals(Duration.ofSeconds(73), server.getUplinkTimeout());
//        assertEquals(Duration.ofSeconds(79), server.getDownlinkTimeout());
//        assertEquals(Duration.ofSeconds(83), server.getResponseTimeout());
//        assertFalse(server.isSecure());
//    }
//
//    /**
//     * Test: 20190308002223203440
//     *
//     * <p>
//     * Type: End-To-End Throughput Test.
//     * </p>
//     *
//     * <p>
//     * Case: Basic Throughput.
//     * </p>
//     *
//     * @throws java.lang.InterruptedException
//     */
//    @Test
//    public void test20190308002223203440 ()
//            throws Throwable
//    {
//        final WebServer engine = WebServer
//                .newWebServer()
//                .withDefaultSettings()
//                .withLogger(logger)
//                .withBindAddress("127.0.42.1")
//                .withRequestFilter(RequestFilters.accept())
//                .build();
//
//        final TestServer server = new TestServer(engine);
//        server.start();
//
//        final SocketTester tester = new SocketTester("127.0.42.1", 8080);
//        tester.sendln("GET /sleep?period=1 HTTP/1.1");
//        tester.sendln();
//        tester.recvln("HTTP/1.0 200 OK");
//        tester.recvln("Connection: close");
//        tester.recvln("Content-Type: text/plain");
//        tester.recvln("Content-Length: 14");
//        tester.recvln();
//        tester.recv("SleepComplete\n");
//        tester.closed();
//
//        assertEquals(1, counter.countOnAccepted());
//        assertEquals(1, counter.countOnConnect());
//        assertEquals(0, counter.countOnDenied());
//        assertEquals(1, counter.countOnDisconnect());
//        assertEquals(0, counter.countOnDownlinkTimeout());
//        assertEquals(0, counter.countOnException());
//        assertEquals(1, counter.countExtend());
//        assertEquals(0, counter.countOnRejected());
//        assertEquals(1, counter.countOnRequest());
//        assertEquals(1, counter.countOnResponse());
//        assertEquals(0, counter.countOnResponseTimeout());
//        assertEquals(0, counter.countOnTooManyConnections());
//        assertEquals(0, counter.countOnUplinkTimeout());
//    }
//
//    /**
//     * Test: 20190308002223203558
//     *
//     * <p>
//     * Type: End-To-End Throughput Test.
//     * </p>
//     *
//     * <p>
//     * Case: Uplink Timeout.
//     * </p>
//     *
//     * @throws java.lang.Throwable
//     */
//    @Test
//    public void test20190308002223203558 ()
//            throws Throwable
//    {
//        final WebServer engine = WebServer
//                .newWebServer()
//                .withDefaultSettings()
//                .withLogger(logger)
//                .withBindAddress("127.0.42.3")
//                .withRequestFilter(RequestFilters.accept())
//                .withUplinkTimeout(Duration.ofMillis(1))
//                .withResponseTimeout(Duration.ofSeconds(60))
//                .build();
//
//        final TestServer server = new TestServer(engine);
//        server.start();
//
//        final SocketTester tester = new SocketTester("127.0.42.3", 8080);
//        tester.send("GET");
//        tester.sleep(100);
//        tester.closed();
//
//        assertEquals(0, counter.countOnAccepted());
//        assertEquals(1, counter.countOnConnect());
//        assertEquals(0, counter.countOnDenied());
//        assertEquals(1, counter.countOnDisconnect());
//        assertEquals(0, counter.countOnDownlinkTimeout());
//        assertEquals(0, counter.countOnException());
//        assertEquals(1, counter.countExtend());
//        assertEquals(0, counter.countOnRejected());
//        assertEquals(0, counter.countOnRequest());
//        assertEquals(0, counter.countOnResponse());
//        assertEquals(0, counter.countOnResponseTimeout());
//        assertEquals(0, counter.countOnTooManyConnections());
//        assertEquals(1, counter.countOnUplinkTimeout());
//    }
//
//    /**
//     * Test: 20190308002223203594
//     *
//     * <p>
//     * Type: End-To-End Throughput Test.
//     * </p>
//     *
//     * <p>
//     * Case: Downlink Timeout.
//     * </p>
//     *
//     * @throws java.lang.Throwable
//     */
//    @Test
//    public void test20190308002223203594 ()
//            throws Throwable
//    {
//        fail();
//
//        final WebServer engine = WebServer
//                .newWebServer()
//                .withDefaultSettings()
//                .withBindAddress("127.0.42.4")
//                .withRequestFilter(RequestFilters.accept())
//                .withDownlinkTimeout(Duration.ofMillis(1))
//                .withResponseTimeout(Duration.ofSeconds(60))
//                .build();
//
//        final TestServer server = new TestServer(engine);
//        server.start();
//
//        final SocketTester tester = new SocketTester("127.0.42.4", 8080);
//        tester.sendln("GET /zero?count=3200100 HTTP/1.1");
//        tester.sendln();
//        tester.recvln("HTTP/1.0 200 OK");
//        tester.sleep(100);
//        final long discarded = tester.exhaust();
//        tester.closed();
//
//        assertTrue(discarded < 3200100);
//    }
//
//    /**
//     * Test: 20190308002223203627
//     *
//     * <p>
//     * Type: End-To-End Throughput Test.
//     * </p>
//     *
//     * <p>
//     * Case: Response Timeout.
//     * </p>
//     *
//     * @throws java.lang.Throwable
//     */
//    @Test
//    public void test20190308002223203627 ()
//            throws Throwable
//    {
//        final WebServer engine = WebServer
//                .newWebServer()
//                .withDefaultSettings()
//                .withLogger(logger)
//                .withBindAddress("127.0.42.5")
//                .withAcceptFilter()
//                .withResponseTimeout(Duration.ofMillis(10))
//                .build();
//
//        final TestServer server = new TestServer(engine);
//        server.start();
//
//        final SocketTester tester = new SocketTester("127.0.42.5", 8080);
//        tester.sendln("GET /sleep?period=2000 HTTP/1.1");
//        tester.sendln();
//        tester.recvln("HTTP/1.0 408 Request Timeout");
//        tester.recvln("Connection: close");
//        tester.recvln("Content-Type: text/html");
//        tester.recvln("Content-Length: 74");
//        tester.recvln();
//        tester.recvln("<head> <meta http-equiv=\"refresh\" content=\"5; URL=\"/408.html\" /> </head>");
//        tester.closed();
//
//        assertEquals(1, counter.countOnAccepted());
//        assertEquals(1, counter.countOnConnect());
//        assertEquals(0, counter.countOnDenied());
//        assertEquals(1, counter.countOnDisconnect());
//        assertEquals(0, counter.countOnDownlinkTimeout());
//        assertEquals(0, counter.countOnException());
//        assertEquals(1, counter.countExtend());
//        assertEquals(0, counter.countOnRejected());
//        assertEquals(1, counter.countOnRequest());
//        assertEquals(1, counter.countOnResponse());
//        assertEquals(1, counter.countOnResponseTimeout());
//        assertEquals(0, counter.countOnTooManyConnections());
//        assertEquals(0, counter.countOnUplinkTimeout());
//    }
//
//    /**
//     * Test: 20190308002223203722
//     *
//     * <p>
//     * Type: End-To-End Throughput Test.
//     * </p>
//     *
//     * <p>
//     * Case: Request denied due to the default pre-check rule.
//     * </p>
//     *
//     * @throws java.lang.Throwable
//     */
//    @Test
//    public void test20190308002223203722 ()
//            throws Throwable
//    {
//        final WebServer engine = WebServer
//                .newWebServer()
//                .withDefaultSettings()
//                .withLogger(logger)
//                .withBindAddress("127.0.42.6")
//                .build();
//
//        final TestServer server = new TestServer(engine);
//        server.start();
//
//        final SocketTester tester = new SocketTester("127.0.42.6", 8080);
//        tester.sendln("GET /zero?count=1 HTTP/1.1");
//        tester.sendln();
//        tester.closed();
//
//        assertEquals(0, counter.countOnAccepted());
//        assertEquals(1, counter.countOnConnect());
//        assertEquals(1, counter.countOnDenied());
//        assertEquals(1, counter.countOnDisconnect());
//        assertEquals(0, counter.countOnDownlinkTimeout());
//        assertEquals(0, counter.countOnException());
//        assertEquals(1, counter.countExtend());
//        assertEquals(0, counter.countOnRejected());
//        assertEquals(0, counter.countOnRequest());
//        assertEquals(0, counter.countOnResponse());
//        assertEquals(0, counter.countOnResponseTimeout());
//        assertEquals(0, counter.countOnTooManyConnections());
//        assertEquals(0, counter.countOnUplinkTimeout());
//    }
//
//    /**
//     * Test: 20190308002223203750
//     *
//     * <p>
//     * Type: End-To-End Throughput Test.
//     * </p>
//     *
//     * <p>
//     * Case: Request denied due to custom pre-check rule.
//     * </p>
//     *
//     * @throws java.lang.Throwable
//     */
//    @Test
//    public void test20190308002223203750 ()
//            throws Throwable
//    {
//        final WebServer engine = WebServer
//                .newWebServer()
//                .withDefaultSettings()
//                .withLogger(logger)
//                .withBindAddress("127.0.42.7")
//                .withRequestFilter(RequestFilters.deny(x -> x.getPath().contains("zero")))
//                .withAcceptFilter()
//                .build();
//
//        final TestServer server = new TestServer(engine);
//        server.start();
//
//        final SocketTester tester = new SocketTester("127.0.42.7", 8080);
//        tester.sendln("GET /zero?count=17 HTTP/1.1");
//        tester.sendln();
//        tester.closed();
//
//        assertEquals(0, counter.countOnAccepted());
//        assertEquals(1, counter.countOnConnect());
//        assertEquals(1, counter.countOnDenied());
//        assertEquals(1, counter.countOnDisconnect());
//        assertEquals(0, counter.countOnDownlinkTimeout());
//        assertEquals(0, counter.countOnException());
//        assertEquals(1, counter.countExtend());
//        assertEquals(0, counter.countOnRejected());
//        assertEquals(0, counter.countOnRequest());
//        assertEquals(0, counter.countOnResponse());
//        assertEquals(0, counter.countOnResponseTimeout());
//        assertEquals(0, counter.countOnTooManyConnections());
//        assertEquals(0, counter.countOnUplinkTimeout());
//    }
//
//    /**
//     * Test: 20190308003051428365
//     *
//     * <p>
//     * Type: End-To-End Throughput Test.
//     * </p>
//     *
//     * <p>
//     * Case: Request denied due to an exception thrown
//     * inside of a custom pre-check rule.
//     * </p>
//     *
//     * @throws java.lang.Throwable
//     */
//    @Test
//    public void test20190308003051428365 ()
//            throws Throwable
//    {
//        final WebServer engine = WebServer
//                .newWebServer()
//                .withDefaultSettings()
//                .withLogger(logger)
//                .withBindAddress("127.0.42.8")
//                .withRequestFilter(RequestFilters.accept(x -> (2 / (1 - 1)) == 0)) // div by zero
//                .withAcceptFilter()
//                .build();
//
//        final TestServer server = new TestServer(engine);
//        server.start();
//
//        final SocketTester tester = new SocketTester("127.0.42.8", 8080);
//        tester.sendln("GET /zero?count=17 HTTP/1.1");
//        tester.sendln();
//        tester.closed();
//
//        assertEquals(0, counter.countOnAccepted());
//        assertEquals(1, counter.countOnConnect());
//        assertEquals(1, counter.countOnDenied());
//        assertEquals(1, counter.countOnDisconnect());
//        assertEquals(0, counter.countOnDownlinkTimeout());
//        assertEquals(1, counter.countOnException());
//        assertEquals(1, counter.countExtend());
//        assertEquals(0, counter.countOnRejected());
//        assertEquals(0, counter.countOnRequest());
//        assertEquals(0, counter.countOnResponse());
//        assertEquals(0, counter.countOnResponseTimeout());
//        assertEquals(0, counter.countOnTooManyConnections());
//        assertEquals(0, counter.countOnUplinkTimeout());
//    }
//
//    /**
//     * Test: 20190308002223203777
//     *
//     * <p>
//     * Type: End-To-End Throughput Test.
//     * </p>
//     *
//     * <p>
//     * Case: Request rejected due to custom pre-check rule.
//     * </p>
//     *
//     * @throws java.lang.Throwable
//     */
//    @Test
//    public void test20190308002223203777 ()
//            throws Throwable
//    {
//        final WebServer engine = WebServer
//                .newWebServer()
//                .withDefaultSettings()
//                .withLogger(logger)
//                .withBindAddress("127.0.42.9")
//                .withRequestFilter(RequestFilters.reject(403, x -> x.getPath().contains("zero")))
//                .withAcceptFilter()
//                .build();
//
//        final TestServer server = new TestServer(engine);
//        server.start();
//
//        final SocketTester tester = new SocketTester("127.0.42.9", 8080);
//        tester.sendln("GET /zero?count=1 HTTP/1.1");
//        tester.sendln();
//        tester.recvln("HTTP/1.0 403 Forbidden");
//        tester.recvln("Connection: close");
//        tester.recvln("Content-Type: text/html");
//        tester.recvln("Content-Length: 74");
//        tester.recvln();
//        tester.recvln("<head> <meta http-equiv=\"refresh\" content=\"5; URL=\"/403.html\" /> </head>");
//        tester.closed();
//
//        assertEquals(0, counter.countOnAccepted());
//        assertEquals(1, counter.countOnConnect());
//        assertEquals(0, counter.countOnDenied());
//        assertEquals(1, counter.countOnDisconnect());
//        assertEquals(0, counter.countOnDownlinkTimeout());
//        assertEquals(0, counter.countOnException());
//        assertEquals(1, counter.countExtend());
//        assertEquals(1, counter.countOnRejected());
//        assertEquals(0, counter.countOnRequest());
//        assertEquals(1, counter.countOnResponse());
//        assertEquals(0, counter.countOnResponseTimeout());
//        assertEquals(0, counter.countOnTooManyConnections());
//        assertEquals(0, counter.countOnUplinkTimeout());
//    }
//
//    /**
//     * Test: 20190308002223203806
//     *
//     * <p>
//     * Type: End-To-End Throughput Test.
//     * </p>
//     *
//     * <p>
//     * Case: Sequential Requests.
//     * </p>
//     *
//     * @throws java.lang.Throwable
//     */
//    @Test
//    public void test20190308002223203806 ()
//            throws Throwable
//    {
//        final WebServer engine = WebServer
//                .newWebServer()
//                .withDefaultSettings()
//                .withLogger(logger)
//                .withBindAddress("127.0.42.10")
//                .withAcceptFilter()
//                .build();
//
//        final TestServer server = new TestServer(engine);
//        server.start();
//
//        final int requestCount = 100;
//
//        for (int i = 0; i < requestCount; i++)
//        {
//            final SocketTester tester = new SocketTester("127.0.42.10", 8080);
//            tester.sendln("GET /zero?count=3 HTTP/1.1");
//            tester.sendln();
//            tester.recvln("HTTP/1.0 200 OK");
//            tester.recvln("Connection: close");
//            tester.recvln("Content-Type: text/plain");
//            tester.recvln("Content-Length: 3");
//            tester.recvln();
//            tester.recv("000");
//            tester.closed();
//        }
//
//        assertEquals(requestCount, counter.countOnAccepted());
//        assertEquals(requestCount, counter.countOnConnect());
//        assertEquals(0, counter.countOnDenied());
//        assertEquals(requestCount, counter.countOnDisconnect());
//        assertEquals(0, counter.countOnDownlinkTimeout());
//        assertEquals(0, counter.countOnException());
//        assertEquals(requestCount, counter.countExtend());
//        assertEquals(0, counter.countOnRejected());
//        assertEquals(requestCount, counter.countOnRequest());
//        assertEquals(requestCount, counter.countOnResponse());
//        assertEquals(0, counter.countOnResponseTimeout());
//        assertEquals(0, counter.countOnTooManyConnections());
//        assertEquals(0, counter.countOnUplinkTimeout());
//    }
//
//    /**
//     * Test: 20190308002223203832
//     *
//     * <p>
//     * Type: End-To-End Throughput Test.
//     * </p>
//     *
//     * <p>
//     * Case: Parallel Requests.
//     * </p>
//     *
//     * @throws java.lang.Throwable
//     */
//    @Test
//    public void test20190308002223203832 ()
//            throws Throwable
//    {
//        final ExecutorService service = Executors.newFixedThreadPool(16);
//
//        final WebServer engine = WebServer
//                .newWebServer()
//                .withDefaultSettings()
//                .withLogger(logger)
//                .withBindAddress("127.0.42.11")
//                .withAcceptFilter()
//                .build();
//
//        final int requestCount = 1280;
//
//        final TestServer server = new TestServer(engine);
//        server.start();
//
//        final CountDownLatch latch = new CountDownLatch(requestCount);
//
//        final Runnable client = () ->
//        {
//            try
//            {
//                final SocketTester tester = new SocketTester("127.0.42.11", 8080);
//                tester.sendln("GET /zero?count=3 HTTP/1.1");
//                tester.sendln();
//                tester.recvln("HTTP/1.0 200 OK");
//                tester.recvln("Connection: close");
//                tester.recvln("Content-Type: text/plain");
//                tester.recvln("Content-Length: 3");
//                tester.recvln();
//                tester.recv("000");
//                tester.closed();
//            }
//            catch (Throwable ex)
//            {
//                fail(ex.getMessage());
//            }
//            finally
//            {
//                latch.countDown();
//            }
//        };
//
//        for (int i = 0; i < requestCount; i++)
//        {
//            service.submit(client);
//        }
//
//        latch.await();
//
//        service.shutdown();
//
//        assertEquals(requestCount, counter.countOnAccepted());
//        assertEquals(requestCount, counter.countOnConnect());
//        assertEquals(0, counter.countOnDenied());
//        assertEquals(requestCount, counter.countOnDisconnect());
//        assertEquals(0, counter.countOnDownlinkTimeout());
//        assertEquals(0, counter.countOnException());
//        assertEquals(requestCount, counter.countExtend());
//        assertEquals(0, counter.countOnRejected());
//        assertEquals(requestCount, counter.countOnRequest());
//        assertEquals(requestCount, counter.countOnResponse());
//        assertEquals(0, counter.countOnResponseTimeout());
//        assertEquals(0, counter.countOnTooManyConnections());
//        assertEquals(0, counter.countOnUplinkTimeout());
//    }
//
//    /**
//     * Test: 20190308003457842738
//     *
//     * <p>
//     * Type: End-To-End Throughput Test.
//     * </p>
//     *
//     * <p>
//     * Case: Initial Line Too Large.
//     * </p>
//     *
//     * @throws java.lang.InterruptedException
//     */
//    @Test
//    public void test20190308003457842738 ()
//            throws Throwable
//    {
//        final WebServer engine = WebServer
//                .newWebServer()
//                .withDefaultSettings()
//                .withLogger(logger)
//                .withBindAddress("127.0.42.12")
//                .withAcceptFilter()
//                .withMaxInitialLineSize(1024)
//                .build();
//
//        final TestServer server = new TestServer(engine);
//        server.start();
//
//        final String tooLong = Strings.repeat("1", 2048);
//
//        final SocketTester tester = new SocketTester("127.0.42.12", 8080);
//        tester.sendln(String.format("GET /zero?count=%s HTTP/1.1", tooLong));
//        tester.sendln();
//        tester.recvln("HTTP/1.0 400 Bad Request");
//        tester.recvln("Connection: close");
//        tester.recvln("Content-Type: text/html");
//        tester.recvln("Content-Length: 74");
//        tester.recvln();
//        tester.recvln("<head> <meta http-equiv=\"refresh\" content=\"5; URL=\"/400.html\" /> </head>");
//        tester.closed();
//
//        assertEquals(0, counter.countOnAccepted());
//        assertEquals(1, counter.countOnConnect());
//        assertEquals(0, counter.countOnDenied());
//        assertEquals(1, counter.countOnDisconnect());
//        assertEquals(0, counter.countOnDownlinkTimeout());
//        assertEquals(0, counter.countOnException());
//        assertEquals(1, counter.countExtend());
//        assertEquals(0, counter.countOnRejected());
//        assertEquals(0, counter.countOnRequest());
//        assertEquals(1, counter.countOnResponse());
//        assertEquals(0, counter.countOnResponseTimeout());
//        assertEquals(0, counter.countOnTooManyConnections());
//        assertEquals(0, counter.countOnUplinkTimeout());
//    }
//
//    /**
//     * Test: 20190308003457842710
//     *
//     * <p>
//     * Type: End-To-End Throughput Test.
//     * </p>
//     *
//     * <p>
//     * Case: Request Too Large.
//     * </p>
//     *
//     * @throws java.lang.Throwable
//     */
//    @Test
//    public void test20190308003457842710 ()
//            throws Throwable
//    {
//        final WebServer engine = WebServer
//                .newWebServer()
//                .withDefaultSettings()
//                .withLogger(logger)
//                .withBindAddress("127.0.42.13")
//                .withAcceptFilter()
//                .withMaxInitialLineSize(1024)
//                .build();
//
//        final TestServer server = new TestServer(engine);
//        server.start();
//
//        final String tooLong = Strings.repeat("1", 2048);
//
//        final SocketTester tester = new SocketTester("127.0.42.13", 8080);
//        tester.sendln(String.format("GET /zero?count=%s HTTP/1.1", tooLong));
//        tester.sendln();
//        tester.recvln("HTTP/1.0 400 Bad Request");
//        tester.recvln("Connection: close");
//        tester.recvln("Content-Type: text/html");
//        tester.recvln("Content-Length: 74");
//        tester.recvln();
//        tester.recvln("<head> <meta http-equiv=\"refresh\" content=\"5; URL=\"/400.html\" /> </head>");
//        tester.closed();
//
//        assertEquals(0, counter.countOnAccepted());
//        assertEquals(1, counter.countOnConnect());
//        assertEquals(0, counter.countOnDenied());
//        assertEquals(1, counter.countOnDisconnect());
//        assertEquals(0, counter.countOnDownlinkTimeout());
//        assertEquals(0, counter.countOnException());
//        assertEquals(1, counter.countExtend());
//        assertEquals(0, counter.countOnRejected());
//        assertEquals(0, counter.countOnRequest());
//        assertEquals(1, counter.countOnResponse());
//        assertEquals(0, counter.countOnResponseTimeout());
//        assertEquals(0, counter.countOnTooManyConnections());
//        assertEquals(0, counter.countOnUplinkTimeout());
//    }
//
//    /**
//     * Test: 20190308003654044008
//     *
//     * <p>
//     * Type: End-To-End Throughput Test.
//     * </p>
//     *
//     * <p>
//     * Case: Headers Too Large.
//     * </p>
//     *
//     * @throws java.lang.Throwable
//     */
//    @Test
//    public void test20190308003654044008 ()
//            throws Throwable
//    {
//        fail();
//
//        final WebServer engine = WebServer
//                .newWebServer()
//                .withDefaultSettings()
//                .withBindAddress("127.0.42.14")
//                .withAcceptFilter()
//                .withMaxHeaderSize(1024)
//                .build();
//
//        final TestServer server = new TestServer(engine);
//        server.start();
//
//        final String tooLong = Strings.repeat("1", 2048);
//
//        final SocketTester tester = new SocketTester("127.0.42.14", 8080);
//        tester.sendln("GET /zero?count=3 HTTP/1.1");
//        tester.sendln("X: " + tooLong);
//        tester.sendln("Y: " + tooLong);
//        tester.sendln();
//        tester.readAndPrint();
//        tester.recvln("HTTP/1.0 400 Bad Request");
//        tester.recvln("Connection: close");
//        tester.recvln("Content-Type: text/html");
//        tester.recvln("Content-Length: 74");
//        tester.recvln();
//        tester.recvln("<head> <meta http-equiv=\"refresh\" content=\"5; URL=\"/400.html\" /> </head>");
//        tester.closed();
//    }
//
//    /**
//     * Test: 20190308004116128009
//     *
//     * <p>
//     * Type: End-To-End Throughput Test.
//     * </p>
//     *
//     * <p>
//     * Case: Soft Connection Limit Exceeded.
//     * </p>
//     *
//     * @throws java.lang.Throwable
//     */
//    @Test
//    public void test20190308004116128009 ()
//            throws Throwable
//    {
//        final WebServer engine = WebServer
//                .newWebServer()
//                .withDefaultSettings()
//                .withLogger(logger)
//                .withBindAddress("127.0.42.15")
//                .withAcceptFilter()
//                .withSoftConnectionLimit(0)
//                .withHardConnectionLimit(100)
//                .build();
//
//        final TestServer server = new TestServer(engine);
//        server.start();
//
//        final SocketTester tester = new SocketTester("127.0.42.15", 8080);
//        tester.sendln("GET /zero?count=3 HTTP/1.1");
//        tester.sendln();
//        tester.recvln("HTTP/1.0 429 Too Many Requests");
//        tester.recvln("Connection: close");
//        tester.recvln("Content-Type: text/html");
//        tester.recvln("Content-Length: 74");
//        tester.recvln();
//        tester.recvln("<head> <meta http-equiv=\"refresh\" content=\"5; URL=\"/429.html\" /> </head>");
//        tester.closed();
//
//        assertEquals(0, counter.countOnAccepted());
//        assertEquals(1, counter.countOnConnect());
//        assertEquals(0, counter.countOnDenied());
//        assertEquals(1, counter.countOnDisconnect());
//        assertEquals(0, counter.countOnDownlinkTimeout());
//        assertEquals(0, counter.countOnException());
//        assertEquals(1, counter.countExtend());
//        assertEquals(0, counter.countOnRejected());
//        assertEquals(0, counter.countOnRequest());
//        assertEquals(1, counter.countOnResponse());
//        assertEquals(0, counter.countOnResponseTimeout());
//        assertEquals(1, counter.countOnTooManyConnections());
//        assertEquals(0, counter.countOnUplinkTimeout());
//    }
//
//    /**
//     * Test: 20190308004116128086
//     *
//     * <p>
//     * Type: End-To-End Throughput Test.
//     * </p>
//     *
//     * <p>
//     * Case: Hard Connection Limit Exceeded.
//     * </p>
//     *
//     * @throws java.lang.Throwable
//     */
//    @Test
//    public void test20190308004116128086 ()
//            throws Throwable
//    {
//        final WebServer engine = WebServer
//                .newWebServer()
//                .withDefaultSettings()
//                .withLogger(logger)
//                .withBindAddress("127.0.42.16")
//                .withAcceptFilter()
//                .withSoftConnectionLimit(100)
//                .withHardConnectionLimit(0)
//                .build();
//
//        final TestServer server = new TestServer(engine);
//        server.start();
//
//        final SocketTester tester = new SocketTester("127.0.42.16", 8080);
//        tester.sendln("GET /zero?count=3 HTTP/1.1");
//        tester.sendln();
//
//        try
//        {
//            tester.closed();
//        }
//        catch (SocketException ex)
//        {
//            assertTrue(ex.getMessage().toLowerCase().contains("connection reset"));
//        }
//
//        assertEquals(0, counter.countOnAccepted());
//        assertEquals(0, counter.countOnConnect());
//        assertEquals(0, counter.countOnDenied());
//        assertEquals(0, counter.countOnDisconnect());
//        assertEquals(0, counter.countOnDownlinkTimeout());
//        assertEquals(0, counter.countOnException());
//        assertEquals(1, counter.countExtend());
//        assertEquals(0, counter.countOnRejected());
//        assertEquals(0, counter.countOnRequest());
//        assertEquals(0, counter.countOnResponse());
//        assertEquals(0, counter.countOnResponseTimeout());
//        assertEquals(1, counter.countOnTooManyConnections());
//        assertEquals(0, counter.countOnUplinkTimeout());
//    }
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
        final WebServer engine = WebServer
                .newWebServer()
                .withDefaultSettings()
                .withLogger(new BaseWebLogger())
                .withBindAddress("127.0.42.17")
                .withAcceptFilter()
                .withCompressionThreshold(0)
                .build();

        final TestServer server = new TestServer(engine);
        server.start();

        final SocketTester tester = new SocketTester("127.0.42.17", 8080);
        tester.sendln("GET /zero?count=1000 HTTP/1.1");
        tester.sendln("Accept-Encoding: deflate");
        tester.sendln();

        tester.readAndPrint();

        tester.recvln("HTTP/1.0 200 OK");
        tester.recvln("Connection: close");
        tester.recvln("Content-Type: text/plain");
        tester.recvln("Content-Length: 1");
        tester.recvln();
        tester.recv("0");
        tester.closed();

        assertTrue(true);
//
//        assertEquals(0, counter.countOnAccepted());
//        assertEquals(0, counter.countOnConnect());
//        assertEquals(0, counter.countOnDenied());
//        assertEquals(0, counter.countOnDisconnect());
//        assertEquals(0, counter.countOnDownlinkTimeout());
//        assertEquals(0, counter.countOnException());
//        assertEquals(1, counter.countExtend());
//        assertEquals(0, counter.countOnRejected());
//        assertEquals(0, counter.countOnRequest());
//        assertEquals(0, counter.countOnResponse());
//        assertEquals(0, counter.countOnResponseTimeout());
//        assertEquals(1, counter.countOnTooManyConnections());
//        assertEquals(0, counter.countOnUplinkTimeout());
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
     */
    @Test
    public void test20190308003457842680 ()
    {
        System.out.println("Test: 20190308003457842680");
        fail();
    }
//
//    /**
//     * Test: 20190308003051428434
//     *
//     * <p>
//     * Type: End-To-End Throughput Test.
//     * </p>
//     *
//     * <p>
//     * Case: Successful Chunked Request.
//     * </p>
//     */
//    @Test
//    public void test20190308003051428434 ()
//    {
//        System.out.println("Test: 20190308003051428434");
//        fail();
//    }
//
//    /**
//     * Test: 20190308003311329885
//     *
//     * <p>
//     * Type: End-To-End Throughput Test.
//     * </p>
//     *
//     * <p>
//     * Case: Chunked Request Too Large (Expectation Failed).
//     * </p>
//     */
//    @Test
//    public void test20190308003311329885 ()
//    {
//        System.out.println("Test: 20190308003311329885");
//        fail();
//    }
}
