package com.mackenziehigh.socius.web.server;

import com.mackenziehigh.socius.web.messages.web_m.ServerSideHttpRequest;
import com.mackenziehigh.socius.web.messages.web_m.ServerSideHttpResponse;

/**
 *
 */
public interface ConnectionLogger
{
    public default void setRemoteAddess (String address)
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

    public default void onReceivedRequest (ServerSideHttpRequest message)
    {
        // Pass.
    }

    public default void onAcceptedRequest (ServerSideHttpRequest message)
    {
        // Pass.
    }

    public default void onRejectedRequest (ServerSideHttpRequest message)
    {
        // Pass.
    }

    public default void onDeniedRequest (ServerSideHttpRequest message)
    {
        // Pass.
    }

    public default void onResponse (ServerSideHttpResponse message)
    {
        // Pass.
    }

    public default void onUplinkTimeout ()
    {
        // Pass.
    }

    public default void onDownlinkTimeout ()
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
