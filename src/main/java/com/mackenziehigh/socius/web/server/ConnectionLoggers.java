package com.mackenziehigh.socius.web.server;

import com.mackenziehigh.socius.web.messages.web_m;
import java.util.logging.Logger;

/**
 * Utility methods for <code>ConnectionLogger</code>s.
 */
public final class ConnectionLoggers
{
    /**
     * Create a new logger that does not actually log anything.
     *
     * @return the new logger.
     */
    public static ConnectionLogger newNullLogger ()
    {
        return new ConnectionLogger()
        {
            // Pass.
        };
    }

    /**
     * Create a new logger that logs using a Java logger.
     *
     * @param name identifies the Java logger.
     * @return the new logger.
     */
    public static ConnectionLogger newLogger (final String name)
    {
        final Logger logger = Logger.getLogger(name);

        return new ConnectionLogger()
        {
            private volatile String remote;

            @Override
            public void setRemoteAddess (String address)
            {
                remote = address;
            }

            @Override
            public void onConnect ()
            {
                //logger.info(String.format("127.0.0.1 user-identifier frank [10/Oct/2000:13:55:36 -0700] \"GET /apache_pb.gif HTTP/1.0\" 200 2326", os));
            }

            @Override
            public void onDisconnect ()
            {
                ConnectionLogger.super.onDisconnect();
            }

            @Override
            public void onReceivedRequest (web_m.ServerSideHttpRequest message)
            {
                ConnectionLogger.super.onReceivedRequest(message);
            }

            @Override
            public void onAcceptedRequest (web_m.ServerSideHttpRequest message)
            {
                ConnectionLogger.super.onAcceptedRequest(message);
            }

            @Override
            public void onRejectedRequest (web_m.ServerSideHttpRequest message)
            {
                ConnectionLogger.super.onRejectedRequest(message);
            }

            @Override
            public void onDeniedRequest (web_m.ServerSideHttpRequest message)
            {
                ConnectionLogger.super.onDeniedRequest(message);
            }

            @Override
            public void onResponse (web_m.ServerSideHttpResponse message)
            {
                final String line = "";
                final int status = message.getStatus();
                final int length = message.getBody().size();
//                logger.info(String.format("%s [%s] \"%s\" %d %d", remote, time(), line, status, length));
            }

            @Override
            public void onUplinkTimeout ()
            {
                ConnectionLogger.super.onUplinkTimeout();
            }

            @Override
            public void onDownlinkTimeout ()
            {
                ConnectionLogger.super.onDownlinkTimeout();
            }

            @Override
            public void onConnectionTimeout ()
            {
                ConnectionLogger.super.onConnectionTimeout();
            }

            @Override
            public void onException (Throwable ex)
            {
                ConnectionLogger.super.onException(ex);
            }
        };
    }

    /**
     * Create a new logger that does not allow exceptions thrown
     * in the logging methods to propagate outside of the logger.
     *
     * @param delegate will be prevented from throwing exceptions.
     * @return the new wrapper logger.
     */
    public static ConnectionLogger newSafeLogger (final ConnectionLogger delegate)
    {
        return new ConnectionLogger()
        {
            @Override
            public void onException (Throwable ex)
            {
                try
                {
                    delegate.onException(ex);
                }
                catch (Throwable ignored)
                {
                    pass();
                }
            }

            @Override
            public void onConnectionTimeout ()
            {
                try
                {
                    delegate.onConnectionTimeout();
                }
                catch (Throwable ignored)
                {
                    pass();
                }
            }

            @Override
            public void onDownlinkTimeout ()
            {
                try
                {
                    delegate.onDownlinkTimeout();
                }
                catch (Throwable ignored)
                {
                    pass();
                }
            }

            @Override
            public void onUplinkTimeout ()
            {
                try
                {
                    delegate.onUplinkTimeout();
                }
                catch (Throwable ignored)
                {
                    pass();
                }
            }

            @Override
            public void onResponse (web_m.ServerSideHttpResponse message)
            {
                try
                {
                    delegate.onResponse(message);
                }
                catch (Throwable ignored)
                {
                    pass();
                }
            }

            @Override
            public void onDeniedRequest (web_m.ServerSideHttpRequest message)
            {
                try
                {
                    delegate.onDeniedRequest(message);
                }
                catch (Throwable ignored)
                {
                    pass();
                }
            }

            @Override
            public void onRejectedRequest (web_m.ServerSideHttpRequest message)
            {
                try
                {
                    delegate.onRejectedRequest(message);
                }
                catch (Throwable ignored)
                {
                    pass();
                }
            }

            @Override
            public void onAcceptedRequest (web_m.ServerSideHttpRequest message)
            {
                try
                {
                    delegate.onAcceptedRequest(message);
                }
                catch (Throwable ignored)
                {
                    pass();
                }
            }

            @Override
            public void onReceivedRequest (web_m.ServerSideHttpRequest message)
            {
                try
                {
                    delegate.onReceivedRequest(message);
                }
                catch (Throwable ignored)
                {
                    pass();
                }
            }

            @Override
            public void onDisconnect ()
            {
                try
                {
                    delegate.onDisconnect();
                }
                catch (Throwable ignored)
                {
                    pass();
                }
            }

            @Override
            public void onConnect ()
            {
                try
                {
                    delegate.onConnect();
                }
                catch (Throwable ignored)
                {
                    pass();
                }
            }

            @Override
            public void setRemoteAddess (String address)
            {
                try
                {
                    delegate.setRemoteAddess(address);
                }
                catch (Throwable ignored)
                {
                    pass();
                }
            }

        };
    }

    private static String time ()
    {
        return "";
    }

    private static void pass ()
    {
        // Pass.
    }
}
