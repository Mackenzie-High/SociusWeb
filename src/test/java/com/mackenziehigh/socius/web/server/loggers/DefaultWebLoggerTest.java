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
package com.mackenziehigh.socius.web.server.loggers;

import com.mackenziehigh.socius.web.messages.web_m.HttpProtocol;
import com.mackenziehigh.socius.web.messages.web_m.ServerSideHttpRequest;
import com.mackenziehigh.socius.web.messages.web_m.ServerSideHttpResponse;
import com.mackenziehigh.socius.web.server.WebServer;
import java.net.InetSocketAddress;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class DefaultWebLoggerTest
{
    private final HttpProtocol HTTP_PROTOCOL = HttpProtocol.newBuilder()
            .setMajorVersion(1)
            .setMinorVersion(0)
            .setName("HTTP")
            .setText("HTTP/1.0")
            .build();

    private final StringBuilder output = new StringBuilder();

    private final DefaultWebLogger logger = new DefaultWebLogger()
    {
        @Override
        protected void log (final String message)
        {
            super.log(message);
            output.append(message).append('\n');
        }

    };

    private void expect (final String message)
    {
        assertTrue(output.toString().contains(message + "\n"));
    }

    /**
     * Test: 20190316225041694886
     *
     * <p>
     * Method: <code>extend()</code>
     * </p>
     */
    @Test
    public void test20190316225041694886 ()
    {
        final DefaultWebLogger extended = (DefaultWebLogger) logger.extend();

        assertNotNull(extended);
        assertNotSame(extended, logger);
        assertTrue(extended.objectId > logger.objectId);
    }

    /**
     * Test: 20190316225041694992
     *
     * <p>
     * Method: <code>onStarted()</code>
     * </p>
     */
    @Test
    public void test20190316225041694992 ()
    {
        final WebServer server = WebServer.newWebServer()
                .withDefaultSettings()
                .withServerName("Seti")
                .withReplyTo("Mars")
                .build();

        logger.onStarted(server);

        expect("Server Started");
        expect("ServerName: Seti");
        expect("ServerId: " + server.getServerId());
        expect("ReplyTo: Mars");
        expect("BindAddress: " + server.getBindAddress());
        expect("Port: " + server.getPort());
        expect("MaxMessagesPerRead: " + server.getMaxMessagesPerRead());
        expect("RecvBufferInitialSize: " + server.getRecvBufferInitialSize());
        expect("RecvBufferMinSize: " + server.getRecvBufferMinSize());
        expect("RecvBufferMaxSize: " + server.getRecvBufferMaxSize());
        expect("SoftConnectionLimit: " + server.getSoftConnectionLimit());
        expect("HardConnectionLimit: " + server.getHardConnectionLimit());
        expect("MaxPauseTime: " + server.getMaxPauseTime());
        expect("MaxRequestSize: " + server.getMaxRequestSize());
        expect("MaxInitialLineSize: " + server.getMaxInitialLineSize());
        expect("MaxHeaderSize: " + server.getMaxHeaderSize());
        expect("CompressionLevel: " + server.getCompressionLevel());
        expect("CompressionWindowBits: " + server.getCompressionWindowBits());
        expect("CompressionMemoryLevel: " + server.getCompressionMemoryLevel());
        expect("CompressionThreshold: " + server.getCompressionThreshold());
        expect("UplinkTimeout: " + server.getUplinkTimeout());
        expect("DownlinkTimeout: " + server.getDownlinkTimeout());
        expect("ResponseTimeout: " + server.getResponseTimeout());
    }

    /**
     * Test: 20190316225041695023
     *
     * <p>
     * Method: <code>onStopped()</code>
     * </p>
     */
    @Test
    public void test20190316225041695023 ()
    {
        logger.onStopped();
        expect("Server Stopped");
    }

    /**
     * Test: 20190316225041695050
     *
     * <p>
     * Method: <code>onConnect()</code>
     * </p>
     */
    @Test
    public void test20190316225041695050 ()
    {
        final InetSocketAddress local = new InetSocketAddress("127.0.0.42", 9001);
        final InetSocketAddress remote = new InetSocketAddress("127.0.0.43", 9002);

        logger.onConnect(local, remote);

        expect("Connection Established");
        expect("LocalAddress: " + local);
        expect("RemoteAddress: " + remote);
    }

    /**
     * Test: 20190316225041695077
     *
     * <p>
     * Method: <code>onDisconnect()</code>
     * </p>
     */
    @Test
    public void test20190316225041695077 ()
    {
        final InetSocketAddress local = new InetSocketAddress("127.0.0.42", 9003);
        final InetSocketAddress remote = new InetSocketAddress("127.0.0.43", 9004);

        logger.onDisconnect(local, remote);

        expect("Connection Closed");
        expect("LocalAddress: " + local);
        expect("RemoteAddress: " + remote);
    }

    /**
     * Test: 20190316225041695100
     *
     * <p>
     * Method: <code>onAccepted()</code>
     * </p>
     */
    @Test
    public void test20190316225041695100 ()
    {
        final ServerSideHttpRequest request = ServerSideHttpRequest
                .newBuilder()
                .setMethod("GET")
                .setUri("/index.html")
                .setProtocol(HTTP_PROTOCOL)
                .build();

        logger.onAccepted(request);

        expect("Accepted Request");
        expect("RequestMethod: GET");
        expect("RequestURI: /index.html");
        expect("RequestProtocol: HTTP/1.0");
    }

    /**
     * Test: 20190316225041695123
     *
     * <p>
     * Method: <code>onRejected()</code>
     * </p>
     */
    @Test
    public void test20190316225041695123 ()
    {
        final ServerSideHttpRequest request = ServerSideHttpRequest
                .newBuilder()
                .setMethod("GET")
                .setUri("/index.html")
                .setProtocol(HTTP_PROTOCOL)
                .build();

        logger.onRejected(request, 499);

        expect("Rejected Request");
        expect("RequestMethod: GET");
        expect("RequestURI: /index.html");
        expect("RequestProtocol: HTTP/1.0");
        expect("ResponseStatus: 499");
    }

    /**
     * Test: 20190316225041695150
     *
     * <p>
     * Method: <code>onDenied()</code>
     * </p>
     */
    @Test
    public void test20190316225041695150 ()
    {
        final ServerSideHttpRequest request = ServerSideHttpRequest
                .newBuilder()
                .setMethod("GET")
                .setUri("/index.html")
                .setProtocol(HTTP_PROTOCOL)
                .build();

        logger.onDenied(request);

        expect("Denied Request");
        expect("RequestMethod: GET");
        expect("RequestURI: /index.html");
        expect("RequestProtocol: HTTP/1.0");
    }

    /**
     * Test: 20190316225041695174
     *
     * <p>
     * Method: <code>onRequest()</code>
     * </p>
     */
    @Test
    public void test20190316225041695174 ()
    {
        final ServerSideHttpRequest request = ServerSideHttpRequest
                .newBuilder()
                .setMethod("GET")
                .setUri("/index.html")
                .setProtocol(HTTP_PROTOCOL)
                .build();

        logger.onRequest(request);

        expect("Dispatch Request");
    }

    /**
     * Test: 20190316225041695196
     *
     * <p>
     * Method: <code>onResponse()</code>
     * </p>
     */
    @Test
    public void test20190316225041695196 ()
    {
        final ServerSideHttpResponse response = ServerSideHttpResponse
                .newBuilder()
                .setStatus(499)
                .build();

        logger.onResponse(response);

        expect("Dispatch Response");
        expect("ResponseStatus: 499");
    }

    /**
     * Test: 20190316231039944835
     *
     * <p>
     * Method: <code>onUplinkTimeout()</code>
     * </p>
     */
    @Test
    public void test20190316231039944835 ()
    {
        logger.onUplinkTimeout();

        expect("Uplink Timeout Expired");
    }

    /**
     * Test: 20190316231039944862
     *
     * <p>
     * Method: <code>onDownlinkTimeout()</code>
     * </p>
     */
    @Test
    public void test20190316231039944862 ()
    {
        logger.onDownlinkTimeout();

        expect("Downlink Timeout Expired");
    }

    /**
     * Test: 20190316231039944885
     *
     * <p>
     * Method: <code>onResponseTimeout</code>
     * </p>
     */
    @Test
    public void test20190316231039944885 ()
    {
        logger.onResponseTimeout();

        expect("Response Timeout Expired");
    }

    /**
     * Test: 20190316231039944930
     *
     * <p>
     * Method: <code>onTooManyConnections()</code>
     * </p>
     */
    @Test
    public void test20190316231039944930 ()
    {
        logger.onTooManyConnections(42);

        expect("Too Many Connections");
        expect("ConnectionCount: 42");
    }

    /**
     * Test: 20190316231039944949
     *
     * <p>
     * Method: <code>onException</code>
     * </p>
     */
    @Test
    public void test20190316231039944949 ()
    {
        final ArithmeticException ex = new ArithmeticException();

        logger.onException(ex);

        expect("Exception Thrown");
        expect(ex.toString());
    }

}
