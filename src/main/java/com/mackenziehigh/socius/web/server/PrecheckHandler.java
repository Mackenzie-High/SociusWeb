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
import com.mackenziehigh.socius.web.messages.web_m;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import java.util.List;

/**
 * Checks a partial HTTP message for authorization, connection limits, etc.
 */
final class PrecheckHandler
        extends MessageToMessageDecoder<HttpRequest>
{
    private final Translator translator;

    private final ImmutableList<Precheck> chain;

    public PrecheckHandler (final Translator translator,
                            final ImmutableList<Precheck> chain)
    {
        this.translator = translator;
        this.chain = chain;
    }

    @Override
    protected void decode (final ChannelHandlerContext ctx,
                           final HttpRequest msg,
                           final List<Object> out)
    {
        try
        {
            final web_m.HttpRequest prefix = translator.prefixOf(msg);

            for (Precheck check : chain)
            {
                final Precheck.Result result = check.check(prefix);

                if (Precheck.Result.ACCEPT == result)
                {
                    out.add(msg);
                    return;
                }
                else if (Precheck.Result.DENY == result)
                {
                    closeConnection(ctx);
                    return;
                }
            }

            closeConnection(ctx);
        }
        catch (Throwable ex)
        {
            closeConnection(ctx);
        }
    }

    private void closeConnection (final ChannelHandlerContext ctx)
    {

        /**
         * Notify the client of the error, but do not tell them exactly why (for security).
         */
        final FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST, Unpooled.EMPTY_BUFFER);
        response.headers().add("connection", "close");
        response.headers().add("content-length", "0");

        /**
         * Send the response to the client.
         */
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
}
