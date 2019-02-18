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

import com.google.protobuf.ByteString;
import com.mackenziehigh.socius.web.messages.web_m;
import com.mackenziehigh.socius.web.messages.web_m.HttpHeader;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class TranslatorTest
{
    private final String serverName = "MyServer";

    private final String serverId = "Server01";

    private final String replyTo = "Server02";

    private final String rawQuery = "Name=Planet&Value=Venus&Value=Earth&value=Mars";

    private final String uri = "/Large%20Upload?" + rawQuery;

    private final Translator translator = new Translator(serverName, serverId, replyTo);

    private FullHttpRequest newRequest ()
    {
        final ByteBuf content = Unpooled.copiedBuffer("Vulcan", StandardCharsets.US_ASCII);

        final FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_0,
                                                                   HttpMethod.GET,
                                                                   uri,
                                                                   content);

        request.headers().add("H1", "100");
        request.headers().add("H1", Arrays.asList("200", "300"));
        request.headers().add("H1", "400");
        request.headers().add("h1", "500");
        request.headers().add("h1", "600");
        request.headers().add("h2", "700");
        request.headers().add("h3", "");
        request.headers().add("h4", "800");
        request.headers().add("Host", "www.mackenziehigh.com");
        request.headers().add("Content-Type", "Text/AES");
        request.headers().add("Content-Length", content.readableBytes());

        request.trailingHeaders().add("T1", "100");
        request.trailingHeaders().add("T1", Arrays.asList("200", "300"));
        request.trailingHeaders().add("T1", "400");
        request.trailingHeaders().add("t1", "500");
        request.trailingHeaders().add("t1", "600");
        request.trailingHeaders().add("t2", "700");
        request.trailingHeaders().add("t3", "");
        request.trailingHeaders().add("t4", "800");

        return request;
    }

    /**
     * Test: 20190217102515063425
     *
     * <p>
     * Method: <code>requestToGPB</code>
     * </p>
     *
     * <p>
     * Case: Basic Translation.
     * </p>
     */
    @Test
    public void test20190217102515063425 ()
    {
        final FullHttpRequest netty = newRequest();
        final web_m.HttpRequest gpb = translator.requestToGPB(netty);

        assertTrue(gpb.hasBody());
        assertTrue(gpb.hasSequenceNumber());
        assertTrue(gpb.hasCorrelationId());

        /**
         * Server Assigned Values.
         */
        assertEquals(serverName, gpb.getServerName());
        assertEquals(serverId, gpb.getServerId());
        assertEquals(replyTo, gpb.getReplyTo());
        assertEquals((double) System.currentTimeMillis(), gpb.getTimestamp(), 60_000.0);

        /**
         * Method.
         */
        assertEquals("GET", gpb.getMethod());

        /**
         * Protocol.
         */
        assertEquals("HTTP", gpb.getProtocol().getName());
        assertEquals("HTTP/1.0", gpb.getProtocol().getText());
        assertEquals(1, gpb.getProtocol().getMajorVersion());
        assertEquals(0, gpb.getProtocol().getMinorVersion());

        /**
         * Path.
         */
        assertEquals(uri, gpb.getUri());
        assertEquals("/Large Upload", gpb.getPath());
        assertEquals("/Large%20Upload", gpb.getRawPath());
        assertEquals(rawQuery, gpb.getRawQuery());

        /**
         * Query Parameters.
         *
         * Note: The keys of query parameters are case-sensitive.
         * Note: If multiple parameters have the same key, then they will be merged.
         */
        assertEquals(3, gpb.getParametersCount());
        assertEquals(1, gpb.getParametersMap().get("Name").getValuesCount());
        assertEquals("Name", gpb.getParametersMap().get("Name").getKey());
        assertEquals("Planet", gpb.getParametersMap().get("Name").getValues(0));
        assertEquals(2, gpb.getParametersMap().get("Value").getValuesCount());
        assertEquals("Value", gpb.getParametersMap().get("Value").getKey());
        assertEquals("Venus", gpb.getParametersMap().get("Value").getValues(0));
        assertEquals("Earth", gpb.getParametersMap().get("Value").getValues(1));
        assertEquals(1, gpb.getParametersMap().get("value").getValuesCount());
        assertEquals("value", gpb.getParametersMap().get("value").getKey());
        assertEquals("Mars", gpb.getParametersMap().get("value").getValues(0));

        /**
         * Headers.
         *
         * Note: The names of headers are case-insensitive.
         * Note: If multiple headers have the same name, then they will be merged.
         */
        assertEquals(7, gpb.getHeadersCount());

        assertEquals("h1", gpb.getHeadersMap().get("h1").getKey());
        assertEquals(6, gpb.getHeadersMap().get("h1").getValuesCount());
        assertEquals("100", gpb.getHeadersMap().get("h1").getValues(0));
        assertEquals("200", gpb.getHeadersMap().get("h1").getValues(1));
        assertEquals("300", gpb.getHeadersMap().get("h1").getValues(2));
        assertEquals("400", gpb.getHeadersMap().get("h1").getValues(3));
        assertEquals("500", gpb.getHeadersMap().get("h1").getValues(4));
        assertEquals("600", gpb.getHeadersMap().get("h1").getValues(5));

        assertEquals("h2", gpb.getHeadersMap().get("h2").getKey());
        assertEquals(1, gpb.getHeadersMap().get("h2").getValuesCount());
        assertEquals("700", gpb.getHeadersMap().get("h2").getValues(0));

        assertEquals("h3", gpb.getHeadersMap().get("h3").getKey());
        assertEquals(1, gpb.getHeadersMap().get("h3").getValuesCount());
        assertEquals("", gpb.getHeadersMap().get("h3").getValues(0));

        assertEquals("h4", gpb.getHeadersMap().get("h4").getKey());
        assertEquals(1, gpb.getHeadersMap().get("h4").getValuesCount());
        assertEquals("800", gpb.getHeadersMap().get("h4").getValues(0));

        assertEquals("host", gpb.getHeadersMap().get("host").getKey());
        assertEquals(1, gpb.getHeadersMap().get("host").getValuesCount());
        assertEquals("www.mackenziehigh.com", gpb.getHeadersMap().get("host").getValues(0));
        assertEquals("www.mackenziehigh.com", gpb.getHost());

        assertEquals("content-type", gpb.getHeadersMap().get("content-type").getKey());
        assertEquals(1, gpb.getHeadersMap().get("content-type").getValuesCount());
        assertEquals("Text/AES", gpb.getHeadersMap().get("content-type").getValues(0));
        assertEquals("Text/AES", gpb.getContentType());

        assertEquals("content-length", gpb.getHeadersMap().get("content-length").getKey());
        assertEquals(1, gpb.getHeadersMap().get("content-length").getValuesCount());
        assertEquals("6", gpb.getHeadersMap().get("content-length").getValues(0));
        assertEquals(6, gpb.getContentLength());
        assertEquals(6, gpb.getBody().size());

        /**
         * Trailers.
         *
         * Note: The names of headers are case-insensitive.
         * Note: If multiple headers have the same name, then they will be merged.
         */
        assertEquals("t1", gpb.getTrailersMap().get("t1").getKey());
        assertEquals(6, gpb.getTrailersMap().get("t1").getValuesCount());
        assertEquals("100", gpb.getTrailersMap().get("t1").getValues(0));
        assertEquals("200", gpb.getTrailersMap().get("t1").getValues(1));
        assertEquals("300", gpb.getTrailersMap().get("t1").getValues(2));
        assertEquals("400", gpb.getTrailersMap().get("t1").getValues(3));
        assertEquals("500", gpb.getTrailersMap().get("t1").getValues(4));
        assertEquals("600", gpb.getTrailersMap().get("t1").getValues(5));

        assertEquals("t2", gpb.getTrailersMap().get("t2").getKey());
        assertEquals(1, gpb.getTrailersMap().get("t2").getValuesCount());
        assertEquals("700", gpb.getTrailersMap().get("t2").getValues(0));

        assertEquals("t3", gpb.getTrailersMap().get("t3").getKey());
        assertEquals(1, gpb.getTrailersMap().get("t3").getValuesCount());
        assertEquals("", gpb.getTrailersMap().get("t3").getValues(0));

        assertEquals("t4", gpb.getTrailersMap().get("t4").getKey());
        assertEquals(1, gpb.getTrailersMap().get("t4").getValuesCount());
        assertEquals("800", gpb.getTrailersMap().get("t4").getValues(0));
    }

    /**
     * Test: 20190217125526819834
     *
     * <p>
     * Method: <code>requestToGPB</code>
     * </p>
     *
     * <p>
     * Case: Sequence Numbers Increment.
     * </p>
     */
    @Test
    public void test20190217125526819834 ()
    {
        final web_m.HttpRequest request1 = translator.requestToGPB(newRequest());
        final web_m.HttpRequest request2 = translator.requestToGPB(newRequest());
        final web_m.HttpRequest request3 = translator.requestToGPB(newRequest());

        assertEquals(1, request1.getSequenceNumber());
        assertEquals(2, request2.getSequenceNumber());
        assertEquals(3, request3.getSequenceNumber());
    }

    /**
     * Test: 20190217125651847530
     *
     * <p>
     * Method: <code>requestToGPB</code>
     * </p>
     *
     * <p>
     * Case: Correlation-ID Changes.
     * </p>
     */
    @Test
    public void test20190217125651847530 ()
    {
        final web_m.HttpRequest request1 = translator.requestToGPB(newRequest());
        final web_m.HttpRequest request2 = translator.requestToGPB(newRequest());

        assertNotEquals(request1.getCorrelationId(), request2.getCorrelationId());
    }

    /**
     * Test: 20190217125526820026
     *
     * <p>
     * Method: <code>requestToGPB</code>
     * </p>
     *
     * <p>
     * Case: Empty Entity.
     * </p>
     */
    @Test
    public void test20190217125526820026 ()
    {
        final FullHttpRequest netty = newRequest();
        netty.headers().clear();
        netty.content().clear();
        final web_m.HttpRequest gpb = translator.requestToGPB(netty);

        assertEquals(0, gpb.getContentLength());
        assertEquals(0, gpb.getBody().size());
    }

    /**
     * Test: 20190217130719286798
     *
     * <p>
     * Method: <code>prefixOf</code>
     * </p>
     *
     * <p>
     * Case: Basic Translation.
     * </p>
     */
    @Test
    public void test20190217130719286798 ()
    {
        final FullHttpRequest full = newRequest();
        final HttpRequest partial = full;
        final web_m.HttpRequest fullGPB = translator.requestToGPB(full);
        final web_m.HttpRequest partialGPB = translator.prefixOf(partial);

        /**
         * Debug.
         */
        assertEquals(0, fullGPB.getDebugCount());
        assertEquals(0, partialGPB.getDebugCount());

        /**
         * Server Name.
         */
        assertEquals(serverName, fullGPB.getServerName());
        assertEquals(serverName, partialGPB.getServerName());

        /**
         * Server ID.
         */
        assertEquals(serverId, fullGPB.getServerId());
        assertEquals(serverId, partialGPB.getServerId());

        /**
         * Reply-To.
         */
        assertTrue(fullGPB.hasReplyTo());
        assertFalse(partialGPB.hasReplyTo());

        /**
         * Correlation ID.
         */
        assertTrue(fullGPB.hasCorrelationId());
        assertFalse(partialGPB.hasCorrelationId());

        /**
         * Sequence Number.
         */
        assertTrue(fullGPB.hasSequenceNumber());
        assertFalse(partialGPB.hasSequenceNumber());

        /**
         * Timestamp.
         */
        assertEquals((double) System.currentTimeMillis(), fullGPB.getTimestamp(), 60_000);
        assertEquals((double) System.currentTimeMillis(), partialGPB.getTimestamp(), 60_000);

        /**
         * Protocol.
         */
        assertEquals(fullGPB.getProtocol(), partialGPB.getProtocol());

        /**
         * Method.
         */
        assertEquals(fullGPB.getMethod(), partialGPB.getMethod());

        /**
         * URI.
         */
        assertEquals(fullGPB.getUri(), partialGPB.getUri());

        /**
         * Path.
         */
        assertEquals(fullGPB.getPath(), partialGPB.getPath());

        /**
         * Raw Path.
         */
        assertEquals(fullGPB.getRawPath(), partialGPB.getRawPath());

        /**
         * Raw Query.
         */
        assertEquals(fullGPB.getRawQuery(), partialGPB.getRawQuery());

        /**
         * Query Parameters.
         */
        assertEquals(fullGPB.getParametersMap(), partialGPB.getParametersMap());

        /**
         * Headers.
         */
        assertEquals(fullGPB.getHeadersMap(), partialGPB.getHeadersMap());

        /**
         * Host Header.
         */
        assertEquals(fullGPB.getHost(), partialGPB.getHost());

        /**
         * Content Type
         */
        assertEquals(fullGPB.getContentType(), partialGPB.getContentType());

        /**
         * Content Length.
         */
        assertEquals(fullGPB.getContentLength(), partialGPB.getContentLength());

        /**
         * Entity.
         */
        assertTrue(fullGPB.hasBody());
        assertFalse(partialGPB.hasBody());

        /**
         * Trailing HTTP Headers.
         */
        assertTrue(fullGPB.getTrailersCount() > 0);
        assertEquals(0, partialGPB.getTrailersCount());
    }

    /**
     * Test: 20190217132919161946
     *
     * <p>
     * Method: <code>prefixOf</code>
     * </p>
     *
     * <p>
     * Case: The prefixOf() method does <b>not</b> increment the sequence-number.
     * </p>
     */
    @Test
    public void test20190217132919161946 ()
    {
        final web_m.HttpRequest request1 = translator.requestToGPB(newRequest());
        translator.prefixOf(newRequest());
        final web_m.HttpRequest request2 = translator.requestToGPB(newRequest());
        translator.prefixOf(newRequest());
        final web_m.HttpRequest request3 = translator.requestToGPB(newRequest());

        assertEquals(1, request1.getSequenceNumber());
        assertEquals(2, request2.getSequenceNumber());
        assertEquals(3, request3.getSequenceNumber());
    }

    /**
     * Test: 20190217133128304755
     *
     * <p>
     * Method: <code>responseFromGPB</code>
     * </p>
     *
     * <p>
     * Case: Basic Translation.
     * </p>
     */
    @Test
    public void test20190217133128304755 ()
    {
        final int status = HttpResponseStatus.USE_PROXY.code();
        final web_m.HttpResponse gpb = web_m.HttpResponse.newBuilder()
                .setStatus(status)
                .setContentType("Text/English")
                .setBody(ByteString.copyFrom("London", StandardCharsets.US_ASCII))
                .build();
        final FullHttpResponse full = translator.responseFromGPB(gpb);

        assertEquals(gpb.getStatus(), full.status().code());
        assertEquals("Text/English", full.headers().get("content-type"));
        assertEquals("London", full.content().toString(StandardCharsets.US_ASCII));
    }

    /**
     * Test: 20190217134711793081
     *
     * <p>
     * Method: <code>responseFromGPB</code>
     * </p>
     *
     * <p>
     * Case: No Status Code.
     * </p>
     */
    @Test
    public void test20190217134711793081 ()
    {
        final web_m.HttpResponse gpb = web_m.HttpResponse.newBuilder()
                .setContentType("Text/English")
                .setBody(ByteString.copyFrom("London", StandardCharsets.US_ASCII))
                .build();

        assertFalse(gpb.hasStatus());

        final FullHttpResponse full = translator.responseFromGPB(gpb);

        assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), full.status().code());
        assertEquals("Text/English", full.headers().get("content-type"));
        assertEquals("London", full.content().toString(StandardCharsets.US_ASCII));
    }

    /**
     * Test: 20190217134711793155
     *
     * <p>
     * Method: <code>responseFromGPB</code>
     * </p>
     *
     * <p>
     * Case: No Content-Type, at all.
     * </p>
     */
    @Test
    public void test20190217134711793155 ()
    {
        final int status = HttpResponseStatus.OK.code();
        final web_m.HttpResponse gpb = web_m.HttpResponse.newBuilder()
                .setStatus(status)
                .setBody(ByteString.copyFrom("London", StandardCharsets.US_ASCII))
                .build();

        assertFalse(gpb.hasContentType());
        assertEquals(0, gpb.getHeadersCount());

        final FullHttpResponse full = translator.responseFromGPB(gpb);

        assertEquals(gpb.getStatus(), full.status().code());
        assertEquals("application/octet-stream", full.headers().get("content-type"));
        assertEquals("London", full.content().toString(StandardCharsets.US_ASCII));
    }

    /**
     * Test: 20190217134711793178
     *
     * <p>
     * Method: <code>responseFromGPB</code>
     * </p>
     *
     * <p>
     * Case: Implicit Content-Type.
     * </p>
     */
    @Test
    public void test20190217134711793178 ()
    {
        final int status = HttpResponseStatus.OK.code();
        final web_m.HttpResponse gpb = web_m.HttpResponse.newBuilder()
                .setStatus(status)
                .addHeaders(HttpHeader.newBuilder().setKey("Content-Type").addValues("Text/Canadian"))
                .setBody(ByteString.copyFrom("London", StandardCharsets.US_ASCII))
                .build();

        assertFalse(gpb.hasContentType());
        assertTrue(gpb.getHeadersList().stream().anyMatch(x -> x.hasKey() && x.getKey().equalsIgnoreCase("Content-Type")));

        final FullHttpResponse full = translator.responseFromGPB(gpb);

        assertEquals(gpb.getStatus(), full.status().code());
        assertEquals("Text/Canadian", full.headers().get("content-type"));
        assertEquals("London", full.content().toString(StandardCharsets.US_ASCII));
    }

    /**
     * Test: 20190217134711793199
     *
     * <p>
     * Method: <code>responseFromGPB</code>
     * </p>
     *
     * <p>
     * Case: Explicit Content-Type overrides Implicit Content-Type.
     * </p>
     */
    @Test
    public void test20190217134711793199 ()
    {
        final int status = HttpResponseStatus.OK.code();
        final web_m.HttpResponse gpb = web_m.HttpResponse.newBuilder()
                .setStatus(status)
                .setContentType("Text/English")
                .addHeaders(HttpHeader.newBuilder().setKey("Content-Type").addValues("Text/Canadian"))
                .setBody(ByteString.copyFrom("London", StandardCharsets.US_ASCII))
                .build();

        assertTrue(gpb.hasContentType());
        assertTrue(gpb.getHeadersList().stream().anyMatch(x -> x.hasKey() && x.getKey().equalsIgnoreCase("Content-Type")));

        final FullHttpResponse full = translator.responseFromGPB(gpb);

        assertEquals(gpb.getStatus(), full.status().code());
        assertEquals("Text/English", full.headers().get("content-type"));
        assertEquals("London", full.content().toString(StandardCharsets.US_ASCII));
    }

    /**
     * Test: 20190217135552148148
     *
     * <p>
     * Method: <code>responseFromGPB</code>
     * </p>
     *
     * <p>
     * Case: Absent Entity.
     * </p>
     */
    @Test
    public void test20190217135552148148 ()
    {
        final web_m.HttpResponse gpb = web_m.HttpResponse.newBuilder().build();

        assertFalse(gpb.hasBody());

        final FullHttpResponse full = translator.responseFromGPB(gpb);

        assertEquals(0, (int) full.headers().getInt("Content-Length"));
        assertTrue(full.content().toString(StandardCharsets.US_ASCII).isEmpty());
    }

    /**
     * Test: 20190217144750129741
     *
     * <p>
     * Method: <code>responseFromGPB</code>
     * </p>
     *
     * <p>
     * Case: Entity Present.
     * </p>
     */
    @Test
    public void test20190217144750129741 ()
    {
        final web_m.HttpResponse gpb = web_m.HttpResponse.newBuilder()
                .setBody(ByteString.copyFrom("London", StandardCharsets.US_ASCII))
                .build();

        assertTrue(gpb.hasBody());

        final FullHttpResponse full = translator.responseFromGPB(gpb);

        assertEquals(6, (int) full.headers().getInt("Content-Length"));
        assertEquals("London", full.content().toString(StandardCharsets.US_ASCII));
    }

    /**
     * Test: 20190217145247679242
     *
     * <p>
     * Method: <code>responseFromGPB</code>
     * </p>
     *
     * <p>
     * Case: Protocol is HTTP 1.0 with HTTP 1.1 Connection Close Header.
     * </p>
     */
    @Test
    public void test20190217145247679242 ()
    {
        final web_m.HttpResponse gpb = web_m.HttpResponse.newBuilder().build();

        final FullHttpResponse full = translator.responseFromGPB(gpb);

        assertEquals(1, full.protocolVersion().majorVersion());
        assertEquals(0, full.protocolVersion().minorVersion());

        assertEquals("close", full.headers().get("connection"));
    }

    /**
     * Test: 20190217145701767499
     *
     * <p>
     * Method: <code>responseFromGPB</code>
     * </p>
     *
     * <p>
     * Case: Implicit Content-Length is overridden.
     * </p>
     */
    @Test
    public void test20190217145701767499 ()
    {
        final web_m.HttpResponse gpb = web_m.HttpResponse.newBuilder()
                .addHeaders(HttpHeader.newBuilder().setKey("Content-Length").addValues("9999"))
                .setBody(ByteString.copyFrom("London", StandardCharsets.US_ASCII))
                .build();

        final FullHttpResponse full = translator.responseFromGPB(gpb);

        assertEquals(6, (int) full.headers().getInt("Content-Length"));
        assertEquals("London", full.content().toString(StandardCharsets.US_ASCII));
    }

    /**
     * Test: 20190217150125185028
     *
     * <p>
     * Method: <code>responseFromGPB</code>
     * </p>
     *
     * <p>
     * Case: Header with no name.
     * </p>
     */
    @Test (expected = IllegalArgumentException.class)
    public void test20190217150125185028 ()
    {
        final web_m.HttpResponse gpb = web_m.HttpResponse.newBuilder()
                .addHeaders(HttpHeader.newBuilder().addValues("9999"))
                .build();

        translator.responseFromGPB(gpb);
    }

    /**
     * Test: 20190217150125185106
     *
     * <p>
     * Method: <code>responseFromGPB</code>
     * </p>
     *
     * <p>
     * Case: Header with no value.
     * </p>
     */
    @Test
    public void test20190217150125185106 ()
    {
        final web_m.HttpResponse gpb = web_m.HttpResponse.newBuilder()
                .addHeaders(HttpHeader.newBuilder().setKey("XX"))
                .build();

        final FullHttpResponse full = translator.responseFromGPB(gpb);

        assertTrue(full.headers().contains("XX"));
        assertEquals("", full.headers().get("XX"));
    }

    /**
     * Test: 20190217151233295516
     *
     * <p>
     * Method: <code>responseFromGPB</code>
     * </p>
     *
     * <p>
     * Case: Header with multiple values.
     * </p>
     */
    @Test
    public void test20190217151233295516 ()
    {
        final web_m.HttpResponse gpb = web_m.HttpResponse.newBuilder()
                .addHeaders(HttpHeader.newBuilder().setKey("Planet").addValues("Venus"))
                .addHeaders(HttpHeader.newBuilder().setKey("Planet").addValues("Earth"))
                .addHeaders(HttpHeader.newBuilder().setKey("Planet").addValues("Mars"))
                .build();

        final FullHttpResponse full = translator.responseFromGPB(gpb);

        assertEquals(3, full.headers().getAll("planet").size());
        assertEquals("Venus", full.headers().getAll("planet").get(0));
        assertEquals("Earth", full.headers().getAll("planet").get(1));
        assertEquals("Mars", full.headers().getAll("planet").get(2));
    }

    /**
     * Test: 20190217153857824014
     *
     * <p>
     * Method: <code>newErrorResponse</code>
     * </p>
     *
     * <p>
     * Case: Normal.
     * </p>
     */
    @Test
    public void test20190217153857824014 ()
    {
        final FullHttpResponse full = Translator.newErrorResponse(HttpResponseStatus.NOT_FOUND);
        assertTrue(full.content().toString(StandardCharsets.US_ASCII).contains("/404.html"));
        assertEquals("close", full.headers().get("connection"));
        assertEquals("0", full.headers().get("content-length"));
        assertEquals("text/html", full.headers().get("content-type"));
    }
}
