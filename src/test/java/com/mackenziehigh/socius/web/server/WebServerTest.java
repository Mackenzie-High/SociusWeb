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
import java.time.Duration;
import static junit.framework.Assert.*;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class WebServerTest
{
    private final Stage stage = Cascade.newStage();

    private void run (final WebServer server,
                      final String testCase)
    {
        server.start();

    }

    private void onEcho ()
    {

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

}
