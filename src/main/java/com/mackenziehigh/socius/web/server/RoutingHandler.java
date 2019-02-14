package com.mackenziehigh.socius.web.server;

import com.mackenziehigh.socius.web.messages.web_m.HttpRequest;
import com.mackenziehigh.socius.web.messages.web_m.HttpResponse;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An instance of this class will be used to translate
 * an incoming HTTP Request into a Protocol Buffer
 * and then send the Protocol Buffer to external handlers.
 *
 * <p>
 * A new instance of this class will be created per connection!
 * </p>
 */
final class RoutingHandler
        extends SimpleChannelInboundHandler<HttpRequest>
{
    private final Router router;

    private final AtomicBoolean firstRequest = new AtomicBoolean();

    public RoutingHandler (final Router router)
    {
        this.router = router;
    }

    @Override
    public void channelReadComplete (final ChannelHandlerContext ctx)
            throws Exception
    {
        ctx.flush();
    }

    @Override
    protected void channelRead0 (final ChannelHandlerContext ctx,
                                 final HttpRequest msg)
            throws URISyntaxException
    {
        if (firstRequest.compareAndSet(false, true))
        {
            router.open(msg, response -> reply(ctx, response));
        }
    }

    private void reply (final ChannelHandlerContext ctx,
                        final HttpResponse response)
    {
//        final ByteBuf buffer = Unpooled.copiedBuffer(response.toByteArray());
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void exceptionCaught (final ChannelHandlerContext ctx,
                                 final Throwable cause)
            throws Exception
    {
        /**
         * Log the exception.
         */
//        logger.warn(cause);
        cause.printStackTrace();
//        cause.getCause().printStackTrace();

        /**
         * Notify the client of the error, but do not tell them exactly why (for security).
         */
        final FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, INTERNAL_SERVER_ERROR);

        /**
         * Send the response to the client.
         */
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
}
