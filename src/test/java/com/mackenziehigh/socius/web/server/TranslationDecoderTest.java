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

import com.mackenziehigh.socius.web.messages.web_m;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class TranslationDecoderTest
{
    /**
     * Test: 20190217175444588597
     *
     * <p>
     * Case: Basic Throughput.
     * </p>
     */
    @Test
    public void test20190217175444588597 ()
    {
        final Translator translator = new Translator("Sname", "Sid", "Srep");
        final TranslationDecoder decoder = new TranslationDecoder(translator);
        final EmbeddedChannel channel = new EmbeddedChannel(decoder);

        final FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_0, HttpMethod.POST, "/upload");

        channel.writeOneInbound(request);

        final web_m.ServerSideHttpRequest gpb = channel.readInbound();

        assertEquals("POST", gpb.getMethod());
        assertEquals("/upload", gpb.getPath());
    }
}
