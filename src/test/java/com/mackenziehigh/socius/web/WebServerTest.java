package com.mackenziehigh.socius.web;

import com.google.common.io.ByteSource;
import com.google.common.io.ByteStreams;
import com.google.protobuf.ByteString;
import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor;
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
public final class WebServerTest
{
    private final Stage stage = Cascade.newStage();

    private final WebServer server = WebServer
            .newWebServer()
            .withResponseTimeout(Duration.ofSeconds(1))
            .withHost("127.0.0.1")
            .withPort(8089)
            .withReplyTo("Mars")
            .withServerName("Alien")
            .withAggregationCapacity(1 * 1024 * 1024)
            .build();

    @Before
    public void setup ()
            throws InterruptedException
    {
        final Actor<http_m.HttpRequest, http_m.HttpResponse> website = stage.newActor().withScript(this::onRequest).create();
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

    private http_m.HttpResponse onRequest (final http_m.HttpRequest request)
    {
        assertEquals(1, server.getConnectionCount());

        final byte[] bytes = request.toByteArray();

        final http_m.HttpResponse response = http_m.HttpResponse
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
        assertEquals("127.0.0.1", server.getHost());
        assertEquals(8089, server.getPort());
        assertEquals("Mars", server.getReplyTo());
        assertEquals(36, server.getServerId().length());
        assertEquals(0, server.getConnectionCount());
        assertEquals(0, server.getSeqnum());

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
            final http_m.HttpRequest response = http_m.HttpRequest.parseFrom(replyBytes);
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
