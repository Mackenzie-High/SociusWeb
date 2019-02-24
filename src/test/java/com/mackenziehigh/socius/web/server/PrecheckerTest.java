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

import com.google.common.collect.ImmutableList;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpMethod.POST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_0;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class PrecheckerTest
{
    private final Precheck precheck1 = x -> x.getMethod().equals("POST") ? Precheck.Result.REJECT : Precheck.Result.FORWARD;

    private final Precheck precheck2 = x -> x.getPath().startsWith("/upload") ? Precheck.Result.ACCEPT : Precheck.Result.FORWARD;

    private final Translator translator = new Translator("Sname", "Sid", "ReplyTo");

    private final Prechecker prechecker = new Prechecker(translator, ImmutableList.of(precheck1, precheck2));

    private final EmbeddedChannel channel = new EmbeddedChannel(prechecker);

    /**
     * Test: 20190217201251643844
     *
     * <p>
     * Case: Rejected due to Check #1.
     * </p>
     */
    @Test
    public void test20190217201251643844 ()
    {
        final FullHttpRequest request = new DefaultFullHttpRequest(HTTP_1_0, POST, "/upload");

        channel.writeInbound(request);

        assertNull(channel.readInbound());

        final FullHttpResponse response = channel.readOutbound();

        assertEquals(FORBIDDEN.code(), response.status().code());
    }

    /**
     * Test: 20190217201251716070
     *
     * <p>
     * Case: Accepted due to Check #2.
     * </p>
     */
    @Test
    public void test20190217201251716070 ()
    {
        final FullHttpRequest request = new DefaultFullHttpRequest(HTTP_1_0, GET, "/upload");

        channel.writeInbound(request);

        assertNull(channel.readOutbound());

        final FullHttpRequest result = channel.readInbound();

        assertSame(request, result);
    }

    /**
     * Test: 20190217201251744141
     *
     * <p>
     * Case: Rejected, by default.
     * </p>
     */
    @Test
    public void test20190217201251744141 ()
    {
        final FullHttpRequest request = new DefaultFullHttpRequest(HTTP_1_0, GET, "/download");

        channel.writeInbound(request);

        assertNull(channel.readInbound());

        final FullHttpResponse response = channel.readOutbound();

        assertEquals(FORBIDDEN.code(), response.status().code());
    }

    /**
     * Test: 20190217201453425830
     *
     * <p>
     * Case: Rejected due to exception in check method.
     * </p>
     */
    @Test
    public void test20190217201453425830 ()
    {
        final Precheck check = x ->
        {
            throw new RuntimeException();
        };

        final Prechecker checker = new Prechecker(translator, ImmutableList.of(check));
        final EmbeddedChannel tester = new EmbeddedChannel(checker);

        final FullHttpRequest request = new DefaultFullHttpRequest(HTTP_1_0, GET, "/upload");

        tester.writeInbound(request);

        assertNull(tester.readInbound());

        final FullHttpResponse response = tester.readOutbound();

        assertEquals(FORBIDDEN.code(), response.status().code());
    }
}
