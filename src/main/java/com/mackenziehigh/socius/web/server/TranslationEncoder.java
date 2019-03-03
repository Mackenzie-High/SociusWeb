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

import com.mackenziehigh.socius.web.messages.web_m.ServerSideHttpResponse;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.util.List;

/**
 * An encoder that translates GPB-based HTTP responses to Netty-based HTTP responses.
 */
final class TranslationEncoder
        extends MessageToMessageEncoder<ServerSideHttpResponse>
{
    private final Translator translator;

    public TranslationEncoder (final Translator translator)
    {
        this.translator = translator;
    }

    @Override
    protected void encode (final ChannelHandlerContext ctx,
                           final ServerSideHttpResponse msg,
                           final List<Object> out)
    {
        try
        {
            final FullHttpResponse response = translator.responseFromGPB(msg);
            response.retain();
            out.add(response);
        }
        catch (Throwable ex)
        {
            final FullHttpResponse error = Translator.newErrorResponse(HttpResponseStatus.BAD_REQUEST);
            ctx.writeAndFlush(error).addListener(ChannelFutureListener.CLOSE);
        }
    }
}
