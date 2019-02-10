package com.mackenziehigh.socius.web;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * Check HTTP Partial message for authorization, connection limits, etc.
 */
final class HttpPrecheckHandler
        extends SimpleChannelInboundHandler<Object>
{
    private final SharedState shared;

    public HttpPrecheckHandler (final SharedState shared)
    {
        this.shared = shared;
    }

    @Override
    protected void channelRead0 (final ChannelHandlerContext ctx,
                                 final Object msg)

    {
    }

}
