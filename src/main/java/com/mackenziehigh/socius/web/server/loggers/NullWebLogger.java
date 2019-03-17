package com.mackenziehigh.socius.web.server.loggers;

import com.mackenziehigh.socius.web.messages.web_m;
import com.mackenziehigh.socius.web.server.WebServer;
import java.net.InetSocketAddress;

/**
 * A <code>WebLogger</code> that does not actually log anything.
 *
 * <p>
 * This class is intentionally non-final in order to facilitate
 * sub-classing and selective overriding of the methods herein.
 * </p>
 */
public class NullWebLogger
        implements WebLogger
{
    private static final NullWebLogger instance = new NullWebLogger();

    /**
     * This constructor is public to allow sub-classing.
     */
    public NullWebLogger ()
    {
        // Pass.
    }

    /**
     * Factory Method.
     *
     * <p>
     * This method always returns the same object.
     * </p>
     *
     * @return the new logger.
     */
    public static NullWebLogger create ()
    {
        return instance;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WebLogger extend ()
    {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStarted (final WebServer server)
    {
        pass();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStopped ()
    {
        pass();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onConnect (final InetSocketAddress local,
                           final InetSocketAddress remote)
    {
        pass();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDisconnect (final InetSocketAddress local,
                              final InetSocketAddress remote)
    {
        pass();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAccepted (final web_m.ServerSideHttpRequest request)
    {
        pass();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRejected (final web_m.ServerSideHttpRequest request,
                            final web_m.ServerSideHttpResponse response)
    {
        pass();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDenied (final web_m.ServerSideHttpRequest request)
    {
        pass();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRequest (final web_m.ServerSideHttpRequest request)
    {
        pass();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onResponse (final web_m.ServerSideHttpRequest request,
                            final web_m.ServerSideHttpResponse response)
    {
        pass();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onUplinkTimeout ()
    {
        pass();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDownlinkTimeout ()
    {
        pass();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onResponseTimeout ()
    {
        pass();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onConnectionTimeout ()
    {
        pass();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onTooManyConnections (final int count)
    {
        pass();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onException (final Throwable cause)
    {
        pass();
    }

    private static void pass ()
    {
        // Pass.
    }
}
