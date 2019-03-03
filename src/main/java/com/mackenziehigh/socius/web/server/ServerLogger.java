package com.mackenziehigh.socius.web.server;

/**
 *
 */
public interface ServerLogger
{

    public default void onStart ()
    {
        // Pass.
    }

    public default void onStop ()
    {
        // Pass.
    }

    public default void onException (Throwable ex)
    {
        // Pass.
    }

}
