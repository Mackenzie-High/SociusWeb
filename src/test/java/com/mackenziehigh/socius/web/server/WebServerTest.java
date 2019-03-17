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

import com.mackenziehigh.socius.web.server.filters.RequestFilters;
import com.google.common.base.Strings;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import static junit.framework.Assert.*;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class WebServerTest
{

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
        assertEquals(WebServer.DEFAULT_SERVER_UPLINK_BANDWIDTH, server.getMaxServerUplinkBandwidth());
        assertEquals(WebServer.DEFAULT_SERVER_DOWNLINK_BANDWIDTH, server.getMaxServerDownlinkBandwidth());
        assertEquals(WebServer.DEFAULT_CONNECTION_UPLINK_BANDWIDTH, server.getMaxConnectionUplinkBandwidth());
        assertEquals(WebServer.DEFAULT_CONNECTION_DOWNLINK_BANDWIDTH, server.getMaxConnectionDownlinkBandwidth());
        assertEquals(WebServer.DEFAULT_MAX_PAUSE_TIME, server.getMaxPauseTime());
        assertEquals(WebServer.DEFAULT_MAX_REQUEST_SIZE, server.getMaxRequestSize());
        assertEquals(WebServer.DEFAULT_MAX_INITIAL_LINE_SIZE, server.getMaxInitialLineSize());
        assertEquals(WebServer.DEFAULT_MAX_HEADER_SIZE, server.getMaxHeaderSize());
        assertEquals(WebServer.DEFAULT_COMPRESSION_LEVEL, server.getCompressionLevel());
        assertEquals(WebServer.DEFAULT_COMPRESSION_WINDOW_BITS, server.getCompressionWindowBits());
        assertEquals(WebServer.DEFAULT_COMPRESSION_MEMORY_LEVEL, server.getCompressionMemoryLevel());
        assertEquals(WebServer.DEFAULT_COMPRESSION_THRESHOLD, server.getCompressionThreshold());
        assertEquals(WebServer.DEFAULT_SLOW_UPLINK_TIMEOUT, server.getSlowUplinkTimeout());
        assertEquals(WebServer.DEFAULT_SLOW_DOWNLINK_TIMEOUT, server.getSlowDownlinkTimeout());
        assertEquals(WebServer.DEFAULT_RESPONSE_TIMEOUT, server.getResponseTimeout());
        assertEquals(WebServer.DEFAULT_CONNECTION_TIMEOUT, server.getConnectionTimeout());
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
                .withMaxServerUplinkBandwidth(47)
                .withMaxServerDownlinkBandwidth(49)
                .withMaxConnectionUplinkBandwidth(51)
                .withMaxConnectionDownlinkBandwidth(53)
                .withMaxPauseTime(Duration.ofSeconds(57))
                .withMaxRequestSize(61)
                .withMaxInitialLineSize(63)
                .withMaxHeaderSize(67)
                .withCompressionLevel(WebServer.DEFAULT_COMPRESSION_LEVEL - 2)
                .withCompressionWindowBits(WebServer.DEFAULT_COMPRESSION_WINDOW_BITS - 2)
                .withCompressionMemoryLevel(WebServer.DEFAULT_COMPRESSION_MEMORY_LEVEL - 2)
                .withCompressionThreshold(71)
                .withSlowUplinkTimeout(Duration.ofSeconds(73))
                .withSlowDownlinkTimeout(Duration.ofSeconds(79))
                .withResponseTimeout(Duration.ofSeconds(83))
                .withConnectionTimeout(Duration.ofSeconds(87))
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
        assertEquals(47, server.getMaxServerUplinkBandwidth());
        assertEquals(49, server.getMaxServerDownlinkBandwidth());
        assertEquals(51, server.getMaxConnectionUplinkBandwidth());
        assertEquals(53, server.getMaxConnectionDownlinkBandwidth());
        assertEquals(Duration.ofSeconds(57), server.getMaxPauseTime());
        assertEquals(61, server.getMaxRequestSize());
        assertEquals(63, server.getMaxInitialLineSize());
        assertEquals(67, server.getMaxHeaderSize());
        assertEquals(4, server.getCompressionLevel());
        assertEquals(13, server.getCompressionWindowBits());
        assertEquals(6, server.getCompressionMemoryLevel());
        assertEquals(71, server.getCompressionThreshold());
        assertEquals(Duration.ofSeconds(73), server.getSlowUplinkTimeout());
        assertEquals(Duration.ofSeconds(79), server.getSlowDownlinkTimeout());
        assertEquals(Duration.ofSeconds(83), server.getResponseTimeout());
        assertEquals(Duration.ofSeconds(87), server.getConnectionTimeout());
    }

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
//                .withBindAddress("127.0.42.1")
//                .withPrecheckAccept()
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
//    }
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
//                .withBindAddress("127.0.42.3")
//                .withPrecheckAccept()
//                .withSlowUplinkTimeout(Duration.ofSeconds(1))
//                .withConnectionTimeout(Duration.ofSeconds(60))
//                .withResponseTimeout(Duration.ofSeconds(60))
//                .build();
//
//        final TestServer server = new TestServer(engine);
//        server.start();
//
//        final SocketTester tester = new SocketTester("127.0.42.3", 8080);
//        tester.sendln("GET");
//        tester.closed();
//    }
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
//        final WebServer engine = WebServer
//                .newWebServer()
//                .withDefaultSettings()
//                .withBindAddress("127.0.42.4")
//                .withPrecheckAccept()
//                .withSlowDownlinkTimeout(Duration.ofSeconds(1))
//                .withConnectionTimeout(Duration.ofSeconds(60))
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
//        tester.sleep(2_000);
//        tester.closed();
//    }
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
//                .withBindAddress("127.0.42.5")
//                .withPrecheckAccept()
//                .withConnectionTimeout(Duration.ofSeconds(60))
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
//    }
//    /**
//     * Test: 20190308002223203694
//     *
//     * <p>
//     * Type: End-To-End Throughput Test.
//     * </p>
//     *
//     * <p>
//     * Case: Connection Timeout.
//     * </p>
//     *
//     * @throws java.lang.Throwable
//     */
//    @Test
//    public void test20190308002223203694 ()
//            throws Throwable
//    {
//        final WebServer engine = WebServer
//                .newWebServer()
//                .withDefaultSettings()
//                .withBindAddress("127.0.42.2")
//                .withPrecheckAccept()
//                .withConnectionTimeout(Duration.ofSeconds(1))
//                .withResponseTimeout(Duration.ofSeconds(60))
//                .build();
//
//        final TestServer server = new TestServer(engine);
//        server.start();
//
//        final SocketTester tester = new SocketTester("127.0.42.2", 8080);
//        tester.sendln("GET /sleep?period=5000 HTTP/1.1");
//        tester.sendln();
//        tester.closed();
//
//    }
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
        final WebServer engine = WebServer
                .newWebServer()
                .withDefaultSettings()
                .withBindAddress("127.0.42.6")
                .build();

        final TestServer server = new TestServer(engine);
        server.start();

        final SocketTester tester = new SocketTester("127.0.42.6", 8080);
        tester.sendln("GET /zero?count=1 HTTP/1.1");
        tester.sendln();
        tester.closed();
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
        final WebServer engine = WebServer
                .newWebServer()
                .withDefaultSettings()
                .withBindAddress("127.0.42.7")
                .withRequestFilter(RequestFilters.deny(x -> x.getPath().contains("zero")))
                .withAcceptFilter()
                .build();

        final TestServer server = new TestServer(engine);
        server.start();

        final SocketTester tester = new SocketTester("127.0.42.7", 8080);
        tester.sendln("GET /zero?count=17 HTTP/1.1");
        tester.sendln();
        tester.closed();
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
        final WebServer engine = WebServer
                .newWebServer()
                .withDefaultSettings()
                .withBindAddress("127.0.42.8")
                .withRequestFilter(RequestFilters.accept(x -> (2 / (1 - 1)) == 0)) // div by zero
                .withAcceptFilter()
                .build();

        final TestServer server = new TestServer(engine);
        server.start();

        final SocketTester tester = new SocketTester("127.0.42.8", 8080);
        tester.sendln("GET /zero?count=17 HTTP/1.1");
        tester.sendln();
        tester.closed();
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
        final WebServer engine = WebServer
                .newWebServer()
                .withDefaultSettings()
                .withBindAddress("127.0.42.9")
                .withRequestFilter(RequestFilters.reject(403, x -> x.getPath().contains("zero")))
                .withAcceptFilter()
                .build();

        final TestServer server = new TestServer(engine);
        server.start();

        final SocketTester tester = new SocketTester("127.0.42.9", 8080);
        tester.sendln("GET /zero?count=1 HTTP/1.1");
        tester.sendln();
        tester.recvln("HTTP/1.0 403 Forbidden");
        tester.recvln("Connection: close");
        tester.recvln("Content-Type: text/html");
        tester.recvln("Content-Length: 74");
        tester.recvln();
        tester.recvln("<head> <meta http-equiv=\"refresh\" content=\"5; URL=\"/403.html\" /> </head>");
        tester.closed();
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
        final WebServer engine = WebServer
                .newWebServer()
                .withDefaultSettings()
                .withBindAddress("127.0.42.10")
                .withAcceptFilter()
                .build();

        final TestServer server = new TestServer(engine);
        server.start();

        for (int i = 0; i < 100; i++)
        {
            final SocketTester tester = new SocketTester("127.0.42.10", 8080);
            tester.sendln("GET /zero?count=3 HTTP/1.1");
            tester.sendln();
            tester.recvln("HTTP/1.0 200 OK");
            tester.recvln("Connection: close");
            tester.recvln("Content-Type: text/plain");
            tester.recvln("Content-Length: 3");
            tester.recvln();
            tester.recv("000");
            tester.closed();
        }
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

        final WebServer engine = WebServer
                .newWebServer()
                .withDefaultSettings()
                .withBindAddress("127.0.42.11")
                .withAcceptFilter()
                .build();

        final TestServer server = new TestServer(engine);
        server.start();

        final CountDownLatch latch = new CountDownLatch(128);

        final Runnable client = () ->
        {
            try
            {
                for (int i = 0; i < 10; i++)
                {
                    final SocketTester tester = new SocketTester("127.0.42.11", 8080);
                    tester.sendln("GET /zero?count=3 HTTP/1.1");
                    tester.sendln();
                    tester.recvln("HTTP/1.0 200 OK");
                    tester.recvln("Connection: close");
                    tester.recvln("Content-Type: text/plain");
                    tester.recvln("Content-Length: 3");
                    tester.recvln();
                    tester.recv("000");
                    tester.closed();
                }
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

        for (int i = 0; i < 128; i++)
        {
            service.submit(client);
        }

        latch.await();

        service.shutdown();
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
//
//    /**
//     * Test: 20190308003457842596
//     *
//     * <p>
//     * Type: End-To-End Throughput Test.
//     * </p>
//     *
//     * <p>
//     * Case: Compressed Request.
//     * </p>
//     */
//    @Test
//    public void test20190308003457842596 ()
//    {
//        System.out.println("Test: 20190308003457842596");
//        fail();
//    }
//
//    /**
//     * Test: 20190308003457842680
//     *
//     * <p>
//     * Type: End-To-End Throughput Test.
//     * </p>
//     *
//     * <p>
//     * Case: Compressed Response.
//     * </p>
//     */
//    @Test
//    public void test20190308003457842680 ()
//    {
//        System.out.println("Test: 20190308003457842680");
//        fail();
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
//        fail();
//    }
//
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
        final WebServer engine = WebServer
                .newWebServer()
                .withDefaultSettings()
                .withBindAddress("127.0.42.12")
                .withAcceptFilter()
                .withMaxInitialLineSize(1024)
                .build();

        final TestServer server = new TestServer(engine);
        server.start();

        final String tooLong = Strings.repeat("1", 2048);

        final SocketTester tester = new SocketTester("127.0.42.12", 8080);
        tester.sendln(String.format("GET /zero?count=%s HTTP/1.1", tooLong));
        tester.sendln();
        tester.readAndPrint();
        tester.recvln("HTTP/1.0 400 Bad Request");
        tester.recvln("Connection: close");
        tester.recvln("Content-Type: text/html");
        tester.recvln("Content-Length: 74");
        tester.recvln();
        tester.recvln("<head> <meta http-equiv=\"refresh\" content=\"5; URL=\"/400.html\" /> </head>");
        tester.closed();
    }

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
//     */
//    @Test
//    public void test20190308003654044008 ()
//    {
//        System.out.println("Test: 20190308003654044008");
//        fail();
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
//     */
//    @Test
//    public void test20190308004116128009 ()
//    {
//        System.out.println("Test: 20190308004116128009");
//        fail();
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
//     */
//    @Test
//    public void test20190308004116128086 ()
//    {
//        System.out.println("Test: 20190308004116128086");
//        fail();
//    }
//
//    /**
//     * Test: 20190308004116128116
//     *
//     * <p>
//     * Type: End-To-End Throughput Test.
//     * </p>
//     *
//     * <p>
//     * Case: Uplink Bandwidth Rate-Limiting.
//     * </p>
//     */
//    @Test
//    public void test20190308004116128116 ()
//    {
//        System.out.println("Test: 20190308004116128116");
//        fail();
//    }
//    /**
//     * Test: 20190308004116128144
//     *
//     * <p>
//     * Type: End-To-End Throughput Test.
//     * </p>
//     *
//     * <p>
//     * Case: Downlink Bandwidth Rate-Limiting.
//     * </p>
//     *
//     * @throws java.io.IOException
//     */
//    @Test
//    public void test20190308004116128144 ()
//            throws IOException
//    {
////        final WebServer server = WebServer
////                .newWebServer()
////                .withDefaultSettings()
////                .withPrecheckAccept()
////                .withConnectionTimeout(Duration.ofSeconds(60))
////                .withResponseTimeout(Duration.ofSeconds(60))
////                .withSlowUplinkTimeout(Duration.ofSeconds(1))
////                .build();
//
//        final SocketTester tester = new SocketTester("127.0.0.1", 8080);
//        tester.sendln("GET /echo HTTP/1.1");
//        tester.recv("Vulcan");
//        tester.closed();
//
//        //final TestServer tester = new TestServer(server);
//    }
}
