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
    public static final WebLogger NULL_LOGGER = new BaseWebLogger();

    private final WebLogger delegate;

    public SafeWebLogger (final WebLogger delegate)
    {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WebLogger extend ()
    {
        try
        {
            return new SafeWebLogger(delegate.extend());
        }
        catch (Throwable ex)
        {
            return NULL_LOGGER;
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
            pass();
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
            pass();
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
            pass();
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
            pass();
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
            pass();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRejected (final web_m.ServerSideHttpRequest request,
                            final int status)
    {
        try
        {
            delegate.onRejected(request, status);
        }
        catch (Throwable ex)
        {
            pass();
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
            pass();
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
            pass();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onResponse (final web_m.ServerSideHttpResponse response)
    {
        try
        {
            delegate.onResponse(response);
        }
        catch (Throwable ex)
        {
            pass();
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
            pass();
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
            pass();
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
            pass();
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
            pass();
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

}
