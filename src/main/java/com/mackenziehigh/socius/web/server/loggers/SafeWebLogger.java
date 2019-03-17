package com.mackenziehigh.socius.web.server.loggers;

import com.mackenziehigh.socius.web.messages.web_m;
import com.mackenziehigh.socius.web.server.WebServer;
import java.net.InetSocketAddress;
import java.util.Objects;

/**
 * A <code>WebLogger</code> that does not allow exceptions thrown
 * in the logging methods to propagate outside of the logger.
 */
public final class SafeWebLogger
        implements WebLogger
{
    private final WebLogger delegate;

    private SafeWebLogger (final WebLogger delegate)
    {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    public static WebLogger create (final WebLogger logger)
    {
        return new SafeWebLogger(logger);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WebLogger extend ()
    {
        try
        {
            return create(delegate.extend());
        }
        catch (Throwable ex)
        {
            return NullWebLogger.create();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStarted (final WebServer server)
    {
        try
        {
            delegate.onStarted(server);
        }
        catch (Throwable ex)
        {
            catchAndLog(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStopped ()
    {
        try
        {
            delegate.onStopped();
        }
        catch (Throwable ex)
        {
            catchAndLog(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onConnect (final InetSocketAddress local,
                           final InetSocketAddress remote)
    {
        try
        {
            delegate.onConnect(local, remote);
        }
        catch (Throwable ex)
        {
            catchAndLog(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDisconnect (final InetSocketAddress local,
                              final InetSocketAddress remote)
    {
        try
        {
            delegate.onDisconnect(local, remote);
        }
        catch (Throwable ex)
        {
            catchAndLog(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAccepted (final web_m.ServerSideHttpRequest request)
    {
        try
        {
            delegate.onAccepted(request);
        }
        catch (Throwable ex)
        {
            catchAndLog(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRejected (final web_m.ServerSideHttpRequest request,
                            final web_m.ServerSideHttpResponse response)
    {
        try
        {
            delegate.onRejected(request, response);
        }
        catch (Throwable ex)
        {
            catchAndLog(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDenied (final web_m.ServerSideHttpRequest request)
    {
        try
        {
            delegate.onDenied(request);
        }
        catch (Throwable ex)
        {
            catchAndLog(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRequest (final web_m.ServerSideHttpRequest request)
    {
        try
        {
            delegate.onRequest(request);
        }
        catch (Throwable ex)
        {
            catchAndLog(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onResponse (final web_m.ServerSideHttpRequest request,
                            final web_m.ServerSideHttpResponse response)
    {
        try
        {
            delegate.onResponse(request, response);
        }
        catch (Throwable ex)
        {
            catchAndLog(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onUplinkTimeout ()
    {
        try
        {
            delegate.onUplinkTimeout();
        }
        catch (Throwable ex)
        {
            catchAndLog(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDownlinkTimeout ()
    {
        try
        {
            delegate.onDownlinkTimeout();
        }
        catch (Throwable ex)
        {
            catchAndLog(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onResponseTimeout ()
    {
        try
        {
            delegate.onResponseTimeout();
        }
        catch (Throwable ex)
        {
            catchAndLog(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onConnectionTimeout ()
    {
        try
        {
            delegate.onConnectionTimeout();
        }
        catch (Throwable ex)
        {
            catchAndLog(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onTooManyConnections (final int count)
    {
        try
        {
            delegate.onTooManyConnections(count);
        }
        catch (Throwable ex)
        {
            catchAndLog(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onException (final Throwable cause)
    {
        try
        {
            delegate.onException(cause);
        }
        catch (Throwable ex)
        {
            /**
             * Ignore the exception, because trying to catch-and-log
             * could lead to a stack-overflow in this particular case.
             */
            pass();
        }
    }

    private static void pass ()
    {
        // Pass.
    }

    private void catchAndLog (final Throwable ex)
    {
        try
        {
            onException(ex);
        }
        catch (Exception e)
        {
            pass();
        }
    }
}
