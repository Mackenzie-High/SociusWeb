package com.mackenziehigh.socius.web.server.loggers;

import com.mackenziehigh.socius.web.messages.web_m;
import com.mackenziehigh.socius.web.server.WebServer;
import java.net.InetSocketAddress;
import java.util.Objects;

/**
 * A <code>WebLogger</code> that combines two loggers into one.
 */
public final class ChainWebLogger
        implements WebLogger
{
    private final WebLogger logger1;

    private final WebLogger logger2;

    public ChainWebLogger (final WebLogger logger1,
                           final WebLogger logger2)
    {
        this.logger1 = Objects.requireNonNull(logger1, "logger1");
        this.logger2 = Objects.requireNonNull(logger2, "logger2");
    }

    /**
     * Factory Method.
     *
     * @param logger1 provides the methods to invoke first.
     * @param logger2 provides the methods to invoke second.
     * @return the new logger.
     */
    public static ChainWebLogger chain (final WebLogger logger1,
                                        final WebLogger logger2)
    {
        return new ChainWebLogger(logger1, logger2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WebLogger extend ()
    {
        return chain(logger1.extend(), logger2.extend());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStarted (final WebServer server)
    {
        logger1.onStarted(server);
        logger2.onStarted(server);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStopped ()
    {
        logger1.onStopped();
        logger2.onStopped();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onConnect (final InetSocketAddress local,
                           final InetSocketAddress remote)
    {
        logger1.onConnect(local, remote);
        logger2.onConnect(local, remote);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDisconnect (final InetSocketAddress local,
                              final InetSocketAddress remote)
    {
        logger1.onDisconnect(local, remote);
        logger2.onDisconnect(local, remote);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAccepted (final web_m.ServerSideHttpRequest request)
    {
        logger1.onAccepted(request);
        logger2.onAccepted(request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRejected (final web_m.ServerSideHttpRequest request,
                            final int status)
    {
        logger1.onRejected(request, status);
        logger2.onRejected(request, status);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDenied (final web_m.ServerSideHttpRequest request)
    {
        logger1.onDenied(request);
        logger2.onDenied(request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRequest (final web_m.ServerSideHttpRequest request)
    {
        logger1.onRequest(request);
        logger2.onRequest(request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onResponse (final web_m.ServerSideHttpResponse response)
    {
        logger1.onResponse(response);
        logger2.onResponse(response);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onUplinkTimeout ()
    {
        logger1.onUplinkTimeout();
        logger2.onUplinkTimeout();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDownlinkTimeout ()
    {
        logger1.onDownlinkTimeout();
        logger2.onDownlinkTimeout();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onResponseTimeout ()
    {
        logger1.onResponseTimeout();
        logger2.onResponseTimeout();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onConnectionTimeout ()
    {
        logger1.onConnectionTimeout();
        logger2.onConnectionTimeout();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onTooManyConnections (final int count)
    {
        logger1.onTooManyConnections(count);
        logger2.onTooManyConnections(count);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onException (final Throwable cause)
    {
        logger1.onException(cause);
        logger2.onException(cause);
    }

}
