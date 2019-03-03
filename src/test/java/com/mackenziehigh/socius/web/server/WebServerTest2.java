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

import com.google.common.io.ByteSource;
import com.google.common.io.ByteStreams;
import com.google.protobuf.ByteString;
import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor;
import com.mackenziehigh.socius.web.messages.web_m;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.time.Duration;
import java.util.UUID;
import static junit.framework.Assert.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class WebServerTest2
{
    private final Stage stage = Cascade.newStage();

    private final WebServer server = WebServer
            .newWebServer()
            .withResponseTimeout(Duration.ofSeconds(1))
            .withBindAddress("127.0.0.1")
            .withPort(8089)
            .withReplyTo("Mars")
            .withServerName("Alien")
            .withMaxRequestSize(1 * 1024 * 1024)
            .build();

    @Before
    public void setup ()
            throws InterruptedException
    {
        final Actor<web_m.HttpRequest, web_m.HttpResponse> website = stage.newActor().withScript(this::onRequest).create();
        server.requestsOut().connect(website.input());
        server.responsesIn().connect(website.output());

        server.start();

        Thread.sleep(2000);
    }

    @After
    public void destroy ()
            throws InterruptedException
    {
        server.stop();
        stage.close();
    }

    private web_m.HttpResponse onRequest (final web_m.HttpRequest request)
    {
        assertEquals(1, server.getConnectionCount());

        final byte[] bytes = request.toByteArray();

        final web_m.HttpResponse response = web_m.HttpResponse
                .newBuilder()
                .setRequest(request)
                .setContentType("text/martian")
                .setStatus(200)
                .setBody(ByteString.copyFrom(bytes))
                .build();

        return response;
    }

    /**
     * Test: 20181122192226083802
     *
     * <p>
     * Case: Basic Throughput.
     * </p>
     *
     * @throws java.lang.Throwable
     */
    @Test
    public void test20181122192226083802 ()
            throws Throwable
    {
        assertEquals("Alien", server.getServerName());
        assertEquals("127.0.0.1", server.getBindAddress());
        assertEquals(8089, server.getPort());
        assertEquals("Mars", server.getReplyTo());
        assertEquals(36, server.getServerId().length());
        assertEquals(0, server.getConnectionCount());
        assertEquals(0, server.getSequenceCount());

        for (int i = 0; i < 100; i++)
        {

            /**
             * Send an HTTP POST request.
             */
            final String body = "InvadeAt1300: " + UUID.randomUUID().toString();
            final URL url = new URL(String.format("http://127.0.0.1:8089/sum?x=10&y=20"));
            URLConnection con = url.openConnection();
            HttpURLConnection http = (HttpURLConnection) con;
            http.setRequestMethod("POST");
            http.setDoOutput(true);
            ByteSource.wrap(body.getBytes()).copyTo(http.getOutputStream());
            http.connect();

            /**
             * The server echos the request back to us in binary form.
             * Go ahead an parse the returned bytes to obtain the request that we sent.
             */
            final byte[] replyBytes = ByteStreams.toByteArray(http.getInputStream());
            final web_m.HttpRequest response = web_m.HttpRequest.parseFrom(replyBytes);
            assertTrue(server.getConnectionCount() <= 1);
            assertEquals(200, http.getResponseCode());
            assertEquals(replyBytes.length, http.getContentLength());
            assertEquals("text/martian", http.getContentType());
            assertEquals("Alien", response.getServerName());
            assertEquals(server.getServerName(), response.getServerName());
            assertEquals(server.getServerId(), response.getServerId());
            assertEquals(server.getReplyTo(), response.getReplyTo());
            assertEquals(i, response.getSequenceNumber());
            assertEquals("HTTP", response.getProtocol().getName());
            assertEquals(1, response.getProtocol().getMajorVersion());
            assertEquals(1, response.getProtocol().getMinorVersion());
            assertEquals("POST", response.getMethod());
            assertEquals("/sum", response.getPath());
            assertEquals("/sum", response.getRawPath());
            assertEquals("/sum?x=10&y=20", response.getUri());
            assertEquals(2, response.getParametersMap().size());
            assertEquals("x", response.getParametersMap().get("x").getKey());
            assertEquals("y", response.getParametersMap().get("y").getKey());
            assertEquals("10", response.getParametersMap().get("x").getValues(0));
            assertEquals("20", response.getParametersMap().get("y").getValues(0));
            assertEquals(36, response.getCorrelationId().length());
            assertEquals(body.length(), response.getContentLength());
            assertEquals(body.length(), response.getBody().size());
            assertEquals(body, response.getBody().toStringUtf8());
        }
    }

}
