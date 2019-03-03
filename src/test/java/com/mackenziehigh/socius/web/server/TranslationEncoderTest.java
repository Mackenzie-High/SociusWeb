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
import com.mackenziehigh.socius.web.messages.web_m.ServerSideHttpResponse;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.FullHttpResponse;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class TranslationEncoderTest
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
        final TranslationEncoder encoder = new TranslationEncoder(translator);
        final EmbeddedChannel channel = new EmbeddedChannel(encoder);

        final String content = "It's a mad, mad, mad, mad world!";

        final ServerSideHttpResponse response = ServerSideHttpResponse
                .newBuilder()
                .setStatus(200)
                .setBody(ByteString.copyFrom(content, StandardCharsets.US_ASCII))
                .build();

        channel.writeOneOutbound(response);
        channel.flush();

        final FullHttpResponse netty = channel.readOutbound();

        assertEquals(content, netty.content().toString(StandardCharsets.US_ASCII));
    }
}
