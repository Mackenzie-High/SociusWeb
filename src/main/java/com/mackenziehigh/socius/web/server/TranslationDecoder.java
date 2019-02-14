package com.mackenziehigh.socius.web.server;

import com.mackenziehigh.socius.web.messages.web_m.HttpRequest;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.FullHttpRequest;
import java.util.List;

/**
 *
 * @author mackenzie
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
        System.out.println("Decoded!");

        final HttpRequest request = translator.requestToGPB(msg);
        out.add(request);
    }

}
