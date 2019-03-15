/*
 * Copyright 2019 Michael Mackenzie High
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mackenziehigh.socius.web.server;

import com.mackenziehigh.socius.web.messages.web_m;
import java.util.logging.Logger;

/**
 * Utility methods for <code>ConnectionLogger</code>s.
 */
public final class WebLoggers
{
    /**
     * Create a new logger that does not actually log anything.
     *
     * @return the new logger.
     */
    public static WebLogger newNullLogger ()
    {
        return () -> newNullLogger();
    }

    /**
     * Create a new logger that logs using a Java logger.
     *
     * @param name identifies the Java logger.
     * @return the new logger.
     */
    public static WebLogger newLogger (final String name)
    {
        final Logger logger = Logger.getLogger(name);

        return new WebLogger()
        {
            private volatile String remote;

            private volatile int port;

            @Override
            public void setRemoteAddess (String address,
                                         int port)
            {
                remote = address;
            }

            @Override
            public void onException (Throwable ex)
            {
                WebLogger.super.onException(ex);
            }

            @Override
            public void onConnectionTimeout ()
            {
                WebLogger.super.onConnectionTimeout();
            }

            @Override
            public void onResponseTimeout ()
            {
                WebLogger.super.onResponseTimeout();
            }

            @Override
            public void onSlowDownlinkTimeout ()
            {
                WebLogger.super.onSlowDownlinkTimeout();
            }

            @Override
            public void onSlowUplinkTimeout ()
            {
                WebLogger.super.onSlowUplinkTimeout();
            }

            @Override
            public void onResponse (web_m.ServerSideHttpRequest request,
                                    web_m.ServerSideHttpResponse response)
            {
                WebLogger.super.onResponse(request, response);
            }

            @Override
            public void onDeniedRequest (web_m.ServerSideHttpRequest request)
            {
                WebLogger.super.onDeniedRequest(request);
            }

            @Override
            public void onRejectedRequest (web_m.ServerSideHttpRequest request,
                                           web_m.ServerSideHttpResponse response)
            {
                WebLogger.super.onRejectedRequest(request, response);
            }

            @Override
            public void onAcceptedRequest (web_m.ServerSideHttpRequest request)
            {
                WebLogger.super.onAcceptedRequest(request);
            }

            @Override
            public void onReceivedRequest (web_m.ServerSideHttpRequest request)
            {
                WebLogger.super.onReceivedRequest(request);
            }

            @Override
            public void onDisconnect ()
            {
                WebLogger.super.onDisconnect();
            }

            @Override
            public void onConnect ()
            {
                WebLogger.super.onConnect();
            }

            @Override
            public void onStop ()
            {
                WebLogger.super.onStop();
            }

            @Override
            public void onStart ()
            {
                WebLogger.super.onStart();
            }

            @Override
            public WebLogger extend ()
            {
                throw new UnsupportedOperationException("Not supported yet.");
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
    public static WebLogger newSafeLogger (final WebLogger delegate)
    {
        return new WebLogger()
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
            public void setRemoteAddess (String address,
                                         int port)
            {
                try
                {
                    delegate.setRemoteAddess(address, port);
                }
                catch (Throwable ignored)
                {
                    pass();
                }
            }

            @Override
            public WebLogger extend ()
            {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public void onStart ()
            {
                WebLogger.super.onStart();
            }

            @Override
            public void onStop ()
            {
                WebLogger.super.onStop();
            }

            @Override
            public void onRejectedRequest (web_m.ServerSideHttpRequest request,
                                           web_m.ServerSideHttpResponse response)
            {
                WebLogger.super.onRejectedRequest(request, response);
            }

            @Override
            public void onResponse (web_m.ServerSideHttpRequest request,
                                    web_m.ServerSideHttpResponse response)
            {
                WebLogger.super.onResponse(request, response);
            }

            @Override
            public void onSlowUplinkTimeout ()
            {
                WebLogger.super.onSlowUplinkTimeout();
            }

            @Override
            public void onSlowDownlinkTimeout ()
            {
                WebLogger.super.onSlowDownlinkTimeout();
            }

            @Override
            public void onResponseTimeout ()
            {
                WebLogger.super.onResponseTimeout();
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
