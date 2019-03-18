package com.mackenziehigh.socius.web.server.loggers;

import com.mackenziehigh.socius.web.messages.web_m.ServerSideHttpRequest;
import com.mackenziehigh.socius.web.messages.web_m.ServerSideHttpResponse;
import com.mackenziehigh.socius.web.server.WebServer;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A <code>WebLogger</code> that tracks how many times each log-method was invoked.
 *
 * <p>
 * This method is primarily useful in unit-testing.
 * </p>
 */
public final class CountingWebLogger
        implements WebLogger
{

    private final AtomicLong countExtend = new AtomicLong();

    private final AtomicLong countOnStarted = new AtomicLong();

    private final AtomicLong countOnStopped = new AtomicLong();

    private final AtomicLong countOnConnect = new AtomicLong();

    private final AtomicLong countOnDisconnect = new AtomicLong();

    private final AtomicLong countOnAccepted = new AtomicLong();

    private final AtomicLong countOnRejected = new AtomicLong();

    private final AtomicLong countOnDenied = new AtomicLong();

    private final AtomicLong countOnRequest = new AtomicLong();

    private final AtomicLong countOnResponse = new AtomicLong();

    private final AtomicLong countOnUplinkTimeout = new AtomicLong();

    private final AtomicLong countOnDownlinkTimeout = new AtomicLong();

    private final AtomicLong countOnResponseTimeout = new AtomicLong();

    private final AtomicLong countOnConnectionTimeout = new AtomicLong();

    private final AtomicLong countOnTooManyConnections = new AtomicLong();

    private final AtomicLong countOnException = new AtomicLong();

    private CountingWebLogger ()
    {
        // Pass.
    }

    /**
     * Factory Method.
     *
     * @return the new logger.
     */
    public static CountingWebLogger create ()
    {
        return new CountingWebLogger();
    }

    /**
     * Get the number of times that <code>extend()</code> was invoked.
     *
     * @return the number of method calls up until now.
     */
    public long countExtend ()
    {
        return countExtend.get();
    }

    /**
     * Get the number of times that <code>onStarted()</code> was invoked.
     *
     * @return the number of method calls up until now.
     */
    public long countOnStarted ()
    {
        return countOnStarted.get();
    }

    /**
     * Get the number of times that <code>onStopped()</code> was invoked.
     *
     * @return the number of method calls up until now.
     */
    public long countOnStopped ()
    {
        return countOnStopped.get();
    }

    /**
     * Get the number of times that <code>onConnect()</code> was invoked.
     *
     * @return the number of method calls up until now.
     */
    public long countOnConnect ()
    {
        return countOnConnect.get();
    }

    /**
     * Get the number of times that <code>onDisconnect()</code> was invoked.
     *
     * @return the number of method calls up until now.
     */
    public long countOnDisconnect ()
    {
        return countOnDisconnect.get();
    }

    /**
     * Get the number of times that <code>onAccepted()</code> was invoked.
     *
     * @return the number of method calls up until now.
     */
    public long countOnAccepted ()
    {
        return countOnAccepted.get();
    }

    /**
     * Get the number of times that <code>onRejected()</code> was invoked.
     *
     * @return the number of method calls up until now.
     */
    public long countOnRejected ()
    {
        return countOnRejected.get();
    }

    /**
     * Get the number of times that <code>onDenied()</code> was invoked.
     *
     * @return the number of method calls up until now.
     */
    public long countOnDenied ()
    {
        return countOnDenied.get();
    }

    /**
     * Get the number of times that <code>onRequest()</code> was invoked.
     *
     * @return the number of method calls up until now.
     */
    public long countOnRequest ()
    {
        return countOnRequest.get();
    }

    /**
     * Get the number of times that <code>onResponse()</code> was invoked.
     *
     * @return the number of method calls up until now.
     */
    public long countOnResponse ()
    {
        return countOnResponse.get();
    }

    /**
     * Get the number of times that <code>onUplinkTimeout()</code> was invoked.
     *
     * @return the number of method calls up until now.
     */
    public long countOnUplinkTimeout ()
    {
        return countOnUplinkTimeout.get();
    }

    /**
     * Get the number of times that <code>onDownlinkTimeout()</code> was invoked.
     *
     * @return the number of method calls up until now.
     */
    public long countOnDownlinkTimeout ()
    {
        return countOnDownlinkTimeout.get();
    }

    /**
     * Get the number of times that <code>onResponseTimeout()</code> was invoked.
     *
     * @return the number of method calls up until now.
     */
    public long countOnResponseTimeout ()
    {
        return countOnResponseTimeout.get();
    }

    /**
     * Get the number of times that <code>onConnectionTimeout()</code> was invoked.
     *
     * @return the number of method calls up until now.
     */
    public long countOnConnectionTimeout ()
    {
        return countOnConnectionTimeout.get();
    }

    /**
     * Get the number of times that <code>onTooManyConnections()</code> was invoked.
     *
     * @return the number of method calls up until now.
     */
    public long countOnTooManyConnections ()
    {
        return countOnTooManyConnections.get();
    }

    /**
     * Get the number of times that <code>onException()</code> was invoked.
     *
     * @return the number of method calls up until now.
     */
    public long countOnException ()
    {
        return countOnException.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WebLogger extend ()
    {
        countExtend.incrementAndGet();
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStarted (final WebServer server)
    {
        countOnStarted.incrementAndGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStopped ()
    {
        countOnStopped.incrementAndGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onConnect (final InetSocketAddress local,
                           final InetSocketAddress remote)
    {
        countOnConnect.incrementAndGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDisconnect (final InetSocketAddress local,
                              final InetSocketAddress remote)
    {
        countOnDisconnect.incrementAndGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAccepted (final ServerSideHttpRequest request)
    {
        countOnAccepted.incrementAndGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRejected (final ServerSideHttpRequest request,
                            final int status)
    {
        countOnRejected.incrementAndGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDenied (final ServerSideHttpRequest request)
    {
        countOnDenied.incrementAndGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRequest (final ServerSideHttpRequest request)
    {
        countOnRequest.incrementAndGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onResponse (final ServerSideHttpResponse response)
    {
        countOnResponse.incrementAndGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onUplinkTimeout ()
    {
        countOnUplinkTimeout.incrementAndGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDownlinkTimeout ()
    {
        countOnDownlinkTimeout.incrementAndGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onResponseTimeout ()
    {
        countOnResponseTimeout.incrementAndGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onConnectionTimeout ()
    {
        countOnConnectionTimeout.incrementAndGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onTooManyConnections (final int count)
    {
        countOnTooManyConnections.incrementAndGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onException (final Throwable cause)
    {
        countOnException.incrementAndGet();
    }

}
