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
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.util.List;

/**
 * Performs a pre-check on an inbound Netty-based HTTP request.
 */
final class Prechecker
        extends MessageToMessageDecoder<HttpRequest>
{
    private final Translator translator;

    private final ImmutableList<Precheck> chain;

    public Prechecker (final Translator translator,
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
            /**
             * Convert the Netty-based request to a GPB-based request.
             * Some parts of the request will be omitted,
             * as they are not needed at this stage.
             */
            final web_m.HttpRequest prefix = translator.prefixOf(msg);

            /**
             * Iterate over the ordered list of rules.
             * If a rule is willing to accept the request,
             * then the request will be accepted without further ado.
             * If a rule is willing to reject the request,
             * then the request will be rejected without further ado.
             * If none of the rules either to accept or reject,
             * then play it safe and reject the request.
             */
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

            /**
             * None of the rules accepted or rejected the request,
             * so play it safe and reject the request.
             */
            closeConnection(ctx);
        }
        catch (Throwable ex)
        {
            /**
             * The request was not explicitly accepted,
             * so play it safe and reject the request.
             */
            closeConnection(ctx);
        }
    }

    private void closeConnection (final ChannelHandlerContext ctx)
    {

        /**
         * Notify the client of the error, but do not tell them exactly why (for security).
         */
        final FullHttpResponse response = Translator.newErrorResponse(HttpResponseStatus.FORBIDDEN);

        /**
         * Send the response to the client.
         */
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
}
