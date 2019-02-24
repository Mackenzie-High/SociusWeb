package com.mackenziehigh.socius.web.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.HttpRequest;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

final class SlowReadTimeout
        extends MessageToMessageDecoder<HttpRequest>
{

    private final SocketChannel channel;

    private final AtomicBoolean flag = new AtomicBoolean();

    private SlowReadTimeout (final SocketChannel channel)
    {
        this.channel = channel;
    }

    public static SlowReadTimeout newHandler (final SocketChannel channel,
                                              final ScheduledExecutorService service,
                                              final Duration timeout)
    {
        final SlowReadTimeout object = new SlowReadTimeout(channel);
        service.schedule(object::onTimeout, timeout.toMillis(), TimeUnit.MILLISECONDS);
        return object;
    }

    private void onTimeout ()
    {
        if (flag.compareAndSet(false, true))
        {
            channel.close();
        }
    }

    @Override
    protected void decode (final ChannelHandlerContext ctx,
                           final HttpRequest msg,
                           final List<Object> out)
    {
        if (flag.compareAndSet(false, true))
        {
            out.add(msg);
        }
    }
}
