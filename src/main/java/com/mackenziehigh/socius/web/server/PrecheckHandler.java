package com.mackenziehigh.socius.web.server;

import com.google.common.collect.ImmutableList;
import com.mackenziehigh.socius.web.messages.web_m.HttpPrefix;
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
 * Check HTTP Partial message for authorization, connection limits, etc.
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
            final HttpPrefix prefix = translator.prefixOf(msg);

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
