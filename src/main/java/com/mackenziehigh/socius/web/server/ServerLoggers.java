package com.mackenziehigh.socius.web.server;

/**
 * Utility methods for <code>ServerLogger</code>s.
 */
public final class ServerLoggers
{
    /**
     * Create a new logger that does not actually log anything.
     *
     * @return the new logger.
     */
    public static ServerLogger newNullLogger ()
    {
        return new ServerLogger()
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
    public static ServerLogger newLogger (final String name)
    {
        return null;
    }

    /**
     * Create a new logger that does not allow exceptions thrown
     * in the logging methods to propagate outside of the logger.
     *
     * @param delegate will be prevented from throwing exceptions.
     * @return the new wrapper logger.
     */
    public static ServerLogger newSafeLogger (final ServerLogger delegate)
    {
        return new ServerLogger()
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
            public void onStop ()
            {
                try
                {
                    delegate.onStop();
                }
                catch (Throwable ignored)
                {
                    pass();
                }
            }

            @Override
            public void onStart ()
            {
                try
                {
                    delegate.onStart();
                }
                catch (Throwable ignored)
                {
                    pass();
                }
            }

        };
    }

    private static void pass ()
    {
        // Pass.
    }
}
