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

import com.mackenziehigh.socius.web.messages.web_m.ServerSideHttpRequest;
import com.mackenziehigh.socius.web.messages.web_m.ServerSideHttpResponse;

/**
 *
 */
public interface WebLogger
{
    public WebLogger extend ();

    public default void setRemoteAddess (String address,
                                         int port)
    {
        // Pass.
    }

    public default void onStart ()
    {
        // Pass.
    }

    public default void onStop ()
    {
        // Pass.
    }

    public default void onConnect ()
    {
        // Pass.
    }

    public default void onDisconnect ()
    {
        // Pass.
    }

    public default void onReceivedRequest (ServerSideHttpRequest request)
    {
        // Pass.
    }

    public default void onAcceptedRequest (ServerSideHttpRequest request)
    {
        // Pass.
    }

    public default void onRejectedRequest (ServerSideHttpRequest request,
                                           ServerSideHttpResponse response)
    {
        // Pass.
    }

    public default void onDeniedRequest (ServerSideHttpRequest request)
    {
        // Pass.
    }

    public default void onResponse (ServerSideHttpRequest request,
                                    ServerSideHttpResponse response)
    {
        // Pass.
    }

    public default void onSlowUplinkTimeout ()
    {
        // Pass.
    }

    public default void onSlowDownlinkTimeout ()
    {
        // Pass.
    }

    public default void onResponseTimeout ()
    {
        // Pass.
    }

    public default void onConnectionTimeout ()
    {
        // Pass.
    }

    public default void onException (Throwable ex)
    {
        // Pass.
    }

}
