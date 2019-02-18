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

import com.mackenziehigh.socius.web.messages.web_m.HttpRequest;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.util.List;

/**
 * A decoder that translates Netty-based HTTP requests to GPB-based HTTP requests.
 */
final class TranslationDecoder
        extends MessageToMessageDecoder<FullHttpRequest>
{
    private final Translator translator;

    public TranslationDecoder (final Translator translator)
    {
        this.translator = translator;
    }

    @Override
    protected void decode (final ChannelHandlerContext ctx,
                           final FullHttpRequest msg,
                           final List<Object> out)
    {
        try
        {
            final HttpRequest request = translator.requestToGPB(msg);
            out.add(request);
        }
        catch (Throwable e)
        {
            final FullHttpResponse error = Translator.newErrorResponse(HttpResponseStatus.BAD_REQUEST);
            ctx.writeAndFlush(error).addListener(ChannelFutureListener.CLOSE);
        }

    }

}
