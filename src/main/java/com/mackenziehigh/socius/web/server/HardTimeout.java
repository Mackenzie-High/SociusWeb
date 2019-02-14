package com.mackenziehigh.socius.web.server;

import io.netty.channel.socket.SocketChannel;
import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Forcibly close a connection, if a timeout has expired,
 * in order to protect against slow read/write attacks.
 */
final class HardTimeout
{
    public static void enforce (final ScheduledExecutorService service,
                                final SocketChannel channel,
                                final Duration timeout)
    {
        final Runnable task = () ->
        {
            if (channel.isOpen())
            {
                channel.close();
            }
        };

        service.schedule(task, timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

}
