package com.mackenziehigh.socius.web.server;

import com.mackenziehigh.socius.web.messages.web_m;

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

    public default void onReceivedRequest (web_m.HttpRequest message)
    {
        // Pass.
    }

    public default void onAcceptedRequest (web_m.HttpRequest message)
    {
        // Pass.
    }

    public default void onRejectedRequest (web_m.HttpRequest message)
    {
        // Pass.
    }

    public default void onDeniedRequest (web_m.HttpRequest message)
    {
        // Pass.
    }

    public default void onResponse (web_m.HttpResponse message)
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

    public default void onConnectionTimeout ()
    {
        // Pass.
    }

    public default void onException (Throwable ex)
    {
        // Pass.
    }

}
