package com.mackenziehigh.socius.web.server.loggers;

import com.mackenziehigh.socius.web.messages.web_m.ServerSideHttpRequest;
import com.mackenziehigh.socius.web.messages.web_m.ServerSideHttpResponse;
import com.mackenziehigh.socius.web.server.WebServer;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Default <code>WebLogger</code>.
 *
 * <p>
 * This class is intentionally non-final in order to facilitate
 * sub-classing and selective overriding of the methods herein.
 * </p>
 */
public class DefaultWebLogger
        implements WebLogger
{
    private static final AtomicLong counter = new AtomicLong();

    protected final long objectId = counter.incrementAndGet();

    /**
     * Override this method in order to change the destination of log-messages.
     *
     * @param message is a log-message to log.
     */
    protected void log (final String message)
    {
        final String time = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        final String output = String.format("[%s][%d]: %s", time, objectId, message);
        System.out.println(output);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WebLogger extend ()
    {
        return new DefaultWebLogger();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStarted (final WebServer server)
    {
        log(String.format("Server Started"));
        log(String.format("ServerName: %s", server.getServerName()));
        log(String.format("ServerId: %s", server.getServerId()));
        log(String.format("ReplyTo: %s", server.getReplyTo()));
        log(String.format("BindAddress: %s", server.getBindAddress()));
        log(String.format("Port: %s", server.getPort()));
        log(String.format("MaxMessagesPerRead: %s", server.getMaxMessagesPerRead()));
        log(String.format("RecvBufferInitialSize: %s", server.getRecvBufferInitialSize()));
        log(String.format("RecvBufferMinSize: %s", server.getRecvBufferMinSize()));
        log(String.format("RecvBufferMaxSize: %s", server.getRecvBufferMaxSize()));
        log(String.format("SoftConnectionLimit: %s", server.getSoftConnectionLimit()));
        log(String.format("HardConnectionLimit: %s", server.getHardConnectionLimit()));
        log(String.format("MaxPauseTime: %s", server.getMaxPauseTime()));
        log(String.format("MaxRequestSize: %s", server.getMaxRequestSize()));
        log(String.format("MaxInitialLineSize: %s", server.getMaxInitialLineSize()));
        log(String.format("MaxHeaderSize: %s", server.getMaxHeaderSize()));
        log(String.format("CompressionLevel: %s", server.getCompressionLevel()));
        log(String.format("CompressionWindowBits: %s", server.getCompressionWindowBits()));
        log(String.format("CompressionMemoryLevel: %s", server.getCompressionMemoryLevel()));
        log(String.format("CompressionThreshold: %s", server.getCompressionThreshold()));
        log(String.format("UplinkTimeout: %s", server.getUplinkTimeout()));
        log(String.format("DownlinkTimeout: %s", server.getDownlinkTimeout()));
        log(String.format("ResponseTimeout: %s", server.getResponseTimeout()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStopped ()
    {
        log(String.format("Server Stopped"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onConnect (final InetSocketAddress local,
                           final InetSocketAddress remote)
    {
        log(String.format("Connection Established"));
        log(String.format("LocalAddress: %s", local));
        log(String.format("RemoteAddress: %s", remote));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDisconnect (final InetSocketAddress local,
                              final InetSocketAddress remote)
    {
        log(String.format("Connection Closed"));
        log(String.format("LocalAddress: %s", local));
        log(String.format("RemoteAddress: %s", remote));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAccepted (final ServerSideHttpRequest request)
    {
        log(String.format("Accepted Request"));
        log(String.format("RequestMethod: %s", request.getMethod()));
        log(String.format("RequestURI: %s", request.getUri()));
        log(String.format("RequestProtocol: %s", request.getProtocol().getText()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRejected (final ServerSideHttpRequest request,
                            final int status)
    {
        log(String.format("Rejected Request"));
        log(String.format("RequestMethod: %s", request.getMethod()));
        log(String.format("RequestURI: %s", request.getUri()));
        log(String.format("RequestProtocol: %s", request.getProtocol().getText()));
        log(String.format("ResponseStatus: %s", status));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDenied (final ServerSideHttpRequest request)
    {
        log(String.format("Denied Request"));
        log(String.format("RequestMethod: %s", request.getMethod()));
        log(String.format("RequestURI: %s", request.getUri()));
        log(String.format("RequestProtocol: %s", request.getProtocol().getText()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRequest (final ServerSideHttpRequest request)
    {
        log(String.format("Dispatch Request"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onResponse (final ServerSideHttpResponse response)
    {
        log(String.format("Dispatch Response"));
        log(String.format("ResponseStatus: %s", response.getStatus()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onUplinkTimeout ()
    {
        log(String.format("Uplink Timeout Expired"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDownlinkTimeout ()
    {
        log(String.format("Downlink Timeout Expired"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onResponseTimeout ()
    {
        log(String.format("Response Timeout Expired"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onTooManyConnections (final int count)
    {
        log(String.format("Too Many Connections"));
        log(String.format("ConnectionCount: %s", count));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onException (final Throwable cause)
    {
        log("Exception Thrown");
        log(cause.toString());
    }

}
