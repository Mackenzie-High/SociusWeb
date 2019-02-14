package com.mackenziehigh.socius.web.server;

import com.mackenziehigh.socius.web.messages.web_m.HttpResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.FullHttpResponse;
import java.util.List;

/**
 *
 * @author mackenzie
 */
final class TranslationEncoder
        extends MessageToMessageEncoder<HttpResponse>
{
    private final Translator translator;

    public TranslationEncoder (final Translator translator)
    {
        this.translator = translator;
    }

    @Override
    protected void encode (final ChannelHandlerContext ctx,
                           final HttpResponse msg,
                           final List<Object> out)
    {
        final FullHttpResponse response = translator.responseFromGPB(msg);
        out.add(response);
    }
}
