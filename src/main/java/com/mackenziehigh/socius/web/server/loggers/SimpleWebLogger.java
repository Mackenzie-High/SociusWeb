package com.mackenziehigh.socius.web.server.loggers;

import com.mackenziehigh.socius.web.messages.web_m;
import com.mackenziehigh.socius.web.server.WebServer;
import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A <code>WebLogger</code> that logs using <i>Log4j2</i>.
 *
 * <p>
 * This class is intentionally non-final in order to facilitate
 * sub-classing and selective overriding of the methods herein.
 * </p>
 */
public abstract class SimpleWebLogger
        implements WebLogger
{
    /**
     * Return the logger to use.
     *
     * @return the underlying logger.
     */
    public abstract Logger logger ();

    private static final AtomicLong counter = new AtomicLong();

    private final String id = Long.toString(counter.getAndIncrement());

    /**
     * Factory Method.
     *
     * @param name identifies the <i>Log4j2</i> logger to use.
     * @return the new logger.
     */
    public static SimpleWebLogger create (final String name)
    {
        Objects.requireNonNull(name, "name");
        final Logger logger = LogManager.getLogger(name);
        return create(logger);
    }

    /**
     * Factory Method.
     *
     * @param logger is the <i>Log4j2</i> logger to use.
     * @return the new logger.
     */
    public static SimpleWebLogger create (final Logger logger)
    {
        Objects.requireNonNull(logger, "logger");

        return new SimpleWebLogger()
        {
            @Override
            public Logger logger ()
            {
                return logger;
            }

            @Override
            public WebLogger extend ()
            {
                return create(logger);
            }
        };
    }

    /**
     * Get the object-identifier used in each default log-message created herein.
     *
     * <p>
     * By default, the object-identifier is a number based on an instance-counter.
     * </p>
     *
     * @return the object-identifier to include in the log-messages.
     */
    public String objectId ()
    {
        return id;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStarted (final WebServer server)
    {
        logger().info("{}: Server Started", objectId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStopped ()
    {
        logger().info("{}: Server Stopped", objectId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onConnect (final InetSocketAddress local,
                           final InetSocketAddress remote)
    {
        logger().info("{}: Connection Established from remote ({}) to local ({}).", objectId(), remote, local);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDisconnect (final InetSocketAddress local,
                              final InetSocketAddress remote)
    {
        logger().info("{}: Connection from remote ({}) to local ({}) is now closed.", objectId(), remote, local);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAccepted (final web_m.ServerSideHttpRequest request)
    {
        logger().info("{}: Accepted request ({} {} {}).",
                      objectId(),
                      request.getMethod(),
                      request.getUri(),
                      request.getProtocol().getText());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRejected (final web_m.ServerSideHttpRequest request,
                            final web_m.ServerSideHttpResponse response)
    {
        logger().info("{}: Rejected request ({} {} {}) with code ({}).",
                      objectId(),
                      request.getMethod(),
                      request.getUri(),
                      request.getProtocol().getText(),
                      response.getStatus());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDenied (final web_m.ServerSideHttpRequest request)
    {
        logger().info("{}: Denied request ({} {} {}).",
                      objectId(),
                      request.getMethod(),
                      request.getUri(),
                      request.getProtocol().getText());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRequest (final web_m.ServerSideHttpRequest request)
    {
        logger().info("{}: Got Request ({} {} {}).",
                      objectId(),
                      request.getMethod(),
                      request.getUri(),
                      request.getProtocol().getText());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onResponse (final web_m.ServerSideHttpRequest request,
                            final web_m.ServerSideHttpResponse response)
    {
        logger().info("{}: Got Response ({}).",
                      objectId(),
                      response.getStatus());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onUplinkTimeout ()
    {
        logger().info("{}: Uplink Timeout Expired.", objectId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDownlinkTimeout ()
    {
        logger().info("{}: Downlink Timeout Expired.", objectId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onResponseTimeout ()
    {
        logger().info("{}: Response Timeout Expired.", objectId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onConnectionTimeout ()
    {
        logger().info("{}: Connection Timeout Expired.", objectId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onTooManyConnections (final int count)
    {
        logger().info("{}: Too Many Open Connections ({}).", objectId(), count);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onException (final Throwable cause)
    {
        logger().info("{}: Exception Thrown ({}).", objectId(), cause.getClass().getSimpleName());
    }
}
