package com.mackenziehigh.socius.web.server.loggers;

import com.mackenziehigh.socius.web.messages.web_m.ServerSideHttpRequest;
import com.mackenziehigh.socius.web.messages.web_m.ServerSideHttpResponse;
import com.mackenziehigh.socius.web.server.WebServer;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class SafeWebLoggerTest
{
    /**
     * Test: 20190316225041694886
     *
     * <p>
     * Method: <code>extend()</code>
     * </p>
     */
    @Test
    public void test20190316225041694886 ()
    {
        final AtomicBoolean executed = new AtomicBoolean(false);

        final WebLogger logger = new NullWebLogger()
        {
            @Override
            public WebLogger extend ()
            {
                executed.set(true);
                throw new Error();
            }

        };

        final WebLogger wrapper = SafeWebLogger.create(logger);

        /**
         * This method should not throw an exception,
         * because the wrapper suppressed the exception.
         */
        final WebLogger extended = wrapper.extend();
        assertTrue(executed.get());

        /**
         * The extend() method is special.
         * If an exception occurs, the method cannot be allowed to fail.
         * Thus, the default null-logger is returned.
         */
        assertSame(NullWebLogger.create(), extended);
    }

    /**
     * Test: 20190316225041694992
     *
     * <p>
     * Method: <code>onStarted()</code>
     * </p>
     */
    @Test
    public void test20190316225041694992 ()
    {
        final AtomicBoolean executed = new AtomicBoolean(false);

        final WebLogger logger = new NullWebLogger()
        {
            @Override
            public void onStarted (WebServer server)
            {
                executed.set(true);
                throw new Error();
            }

        };

        final WebLogger wrapper = SafeWebLogger.create(logger);

        /**
         * This method should not throw an exception,
         * because the wrapper suppressed the exception.
         */
        wrapper.onStarted(null);
        assertTrue(executed.get());
    }

    /**
     * Test: 20190316225041695023
     *
     * <p>
     * Method: <code>onStopped()</code>
     * </p>
     */
    @Test
    public void test20190316225041695023 ()
    {
        final AtomicBoolean executed = new AtomicBoolean(false);

        final WebLogger logger = new NullWebLogger()
        {
            @Override
            public void onStopped ()
            {
                executed.set(true);
                throw new Error();
            }

        };

        final WebLogger wrapper = SafeWebLogger.create(logger);

        /**
         * This method should not throw an exception,
         * because the wrapper suppressed the exception.
         */
        wrapper.onStopped();
        assertTrue(executed.get());
    }

    /**
     * Test: 20190316225041695050
     *
     * <p>
     * Method: <code>onConnect()</code>
     * </p>
     */
    @Test
    public void test20190316225041695050 ()
    {
        final AtomicBoolean executed = new AtomicBoolean(false);

        final WebLogger logger = new NullWebLogger()
        {
            @Override
            public void onConnect (InetSocketAddress local,
                                   InetSocketAddress remote)
            {
                executed.set(true);
                throw new Error();
            }
        };

        final WebLogger wrapper = SafeWebLogger.create(logger);

        /**
         * This method should not throw an exception,
         * because the wrapper suppressed the exception.
         */
        wrapper.onConnect(null, null);
        assertTrue(executed.get());
    }

    /**
     * Test: 20190316225041695077
     *
     * <p>
     * Method: <code>onDisconnect()</code>
     * </p>
     */
    @Test
    public void test20190316225041695077 ()
    {
        final AtomicBoolean executed = new AtomicBoolean(false);

        final WebLogger logger = new NullWebLogger()
        {
            @Override
            public void onDisconnect (InetSocketAddress local,
                                      InetSocketAddress remote)
            {
                executed.set(true);
                throw new Error();
            }
        };

        final WebLogger wrapper = SafeWebLogger.create(logger);

        /**
         * This method should not throw an exception,
         * because the wrapper suppressed the exception.
         */
        wrapper.onDisconnect(null, null);
        assertTrue(executed.get());
    }

    /**
     * Test: 20190316225041695100
     *
     * <p>
     * Method: <code>onAccepted()</code>
     * </p>
     */
    @Test
    public void test20190316225041695100 ()
    {
        final AtomicBoolean executed = new AtomicBoolean(false);

        final WebLogger logger = new NullWebLogger()
        {
            @Override
            public void onAccepted (ServerSideHttpRequest request)
            {
                executed.set(true);
                throw new Error();
            }
        };

        final WebLogger wrapper = SafeWebLogger.create(logger);

        /**
         * This method should not throw an exception,
         * because the wrapper suppressed the exception.
         */
        wrapper.onAccepted(null);
        assertTrue(executed.get());
    }

    /**
     * Test: 20190316225041695123
     *
     * <p>
     * Method: <code>onRejected()</code>
     * </p>
     */
    @Test
    public void test20190316225041695123 ()
    {
        final AtomicBoolean executed = new AtomicBoolean(false);

        final WebLogger logger = new NullWebLogger()
        {
            @Override
            public void onRejected (ServerSideHttpRequest request,
                                    ServerSideHttpResponse response)
            {
                executed.set(true);
                throw new Error();
            }
        };

        final WebLogger wrapper = SafeWebLogger.create(logger);

        /**
         * This method should not throw an exception,
         * because the wrapper suppressed the exception.
         */
        wrapper.onRejected(null, null);
        assertTrue(executed.get());
    }

    /**
     * Test: 20190316225041695150
     *
     * <p>
     * Method: <code>onDenied()</code>
     * </p>
     */
    @Test
    public void test20190316225041695150 ()
    {
        final AtomicBoolean executed = new AtomicBoolean(false);

        final WebLogger logger = new NullWebLogger()
        {
            @Override
            public void onDenied (ServerSideHttpRequest request)
            {
                executed.set(true);
                throw new Error();
            }
        };

        final WebLogger wrapper = SafeWebLogger.create(logger);

        /**
         * This method should not throw an exception,
         * because the wrapper suppressed the exception.
         */
        wrapper.onDenied(null);
        assertTrue(executed.get());
    }

    /**
     * Test: 20190316225041695174
     *
     * <p>
     * Method: <code>onRequest()</code>
     * </p>
     */
    @Test
    public void test20190316225041695174 ()
    {
        final AtomicBoolean executed = new AtomicBoolean(false);

        final WebLogger logger = new NullWebLogger()
        {
            @Override
            public void onRequest (ServerSideHttpRequest request)
            {
                executed.set(true);
                throw new Error();
            }
        };

        final WebLogger wrapper = SafeWebLogger.create(logger);

        /**
         * This method should not throw an exception,
         * because the wrapper suppressed the exception.
         */
        wrapper.onRequest(null);
        assertTrue(executed.get());
    }

    /**
     * Test: 20190316225041695196
     *
     * <p>
     * Method: <code>onResponse()</code>
     * </p>
     */
    @Test
    public void test20190316225041695196 ()
    {
        final AtomicBoolean executed = new AtomicBoolean(false);

        final WebLogger logger = new NullWebLogger()
        {
            @Override
            public void onResponse (ServerSideHttpRequest request,
                                    ServerSideHttpResponse response)
            {
                executed.set(true);
                throw new Error();
            }
        };

        final WebLogger wrapper = SafeWebLogger.create(logger);

        /**
         * This method should not throw an exception,
         * because the wrapper suppressed the exception.
         */
        wrapper.onResponse(null, null);
        assertTrue(executed.get());
    }

    /**
     * Test: 20190316231039944835
     *
     * <p>
     * Method: <code>onUplinkTimeout()</code>
     * </p>
     */
    @Test
    public void test20190316231039944835 ()
    {
        final AtomicBoolean executed = new AtomicBoolean(false);

        final WebLogger logger = new NullWebLogger()
        {
            @Override
            public void onUplinkTimeout ()
            {
                executed.set(true);
                throw new Error();
            }
        };

        final WebLogger wrapper = SafeWebLogger.create(logger);

        /**
         * This method should not throw an exception,
         * because the wrapper suppressed the exception.
         */
        wrapper.onUplinkTimeout();
        assertTrue(executed.get());
    }

    /**
     * Test: 20190316231039944862
     *
     * <p>
     * Method: <code>onDownlinkTimeout()</code>
     * </p>
     */
    @Test
    public void test20190316231039944862 ()
    {
        final AtomicBoolean executed = new AtomicBoolean(false);

        final WebLogger logger = new NullWebLogger()
        {
            @Override
            public void onDownlinkTimeout ()
            {
                executed.set(true);
                throw new Error();
            }
        };

        final WebLogger wrapper = SafeWebLogger.create(logger);

        /**
         * This method should not throw an exception,
         * because the wrapper suppressed the exception.
         */
        wrapper.onDownlinkTimeout();
        assertTrue(executed.get());
    }

    /**
     * Test: 20190316231039944885
     *
     * <p>
     * Method: <code>onResponseTimeout</code>
     * </p>
     */
    @Test
    public void test20190316231039944885 ()
    {
        final AtomicBoolean executed = new AtomicBoolean(false);

        final WebLogger logger = new NullWebLogger()
        {
            @Override
            public void onResponseTimeout ()
            {
                executed.set(true);
                throw new Error();
            }
        };

        final WebLogger wrapper = SafeWebLogger.create(logger);

        /**
         * This method should not throw an exception,
         * because the wrapper suppressed the exception.
         */
        wrapper.onResponseTimeout();
        assertTrue(executed.get());
    }

    /**
     * Test: 20190316231039944909
     *
     * <p>
     * Method: <code>onConnectionTimeout</code>
     * </p>
     */
    @Test
    public void test20190316231039944909 ()
    {
        final AtomicBoolean executed = new AtomicBoolean(false);

        final WebLogger logger = new NullWebLogger()
        {
            @Override
            public void onConnectionTimeout ()
            {
                executed.set(true);
                throw new Error();
            }
        };

        final WebLogger wrapper = SafeWebLogger.create(logger);

        /**
         * This method should not throw an exception,
         * because the wrapper suppressed the exception.
         */
        wrapper.onConnectionTimeout();
        assertTrue(executed.get());
    }

    /**
     * Test: 20190316231039944930
     *
     * <p>
     * Method: <code>onTooManyConnections()</code>
     * </p>
     */
    @Test
    public void test20190316231039944930 ()
    {
        final AtomicBoolean executed = new AtomicBoolean(false);

        final WebLogger logger = new NullWebLogger()
        {
            @Override
            public void onTooManyConnections (int count)
            {
                executed.set(true);
                throw new Error();
            }
        };

        final WebLogger wrapper = SafeWebLogger.create(logger);

        /**
         * This method should not throw an exception,
         * because the wrapper suppressed the exception.
         */
        wrapper.onTooManyConnections(0);
        assertTrue(executed.get());
    }

    /**
     * Test: 20190316231039944949
     *
     * <p>
     * Method: <code>onException</code>
     * </p>
     */
    @Test
    public void test20190316231039944949 ()
    {
        final AtomicBoolean executed = new AtomicBoolean(false);

        final WebLogger logger = new NullWebLogger()
        {
            @Override
            public void onException (Throwable cause)
            {
                executed.set(true);
                throw new Error();
            }
        };

        final WebLogger wrapper = SafeWebLogger.create(logger);

        /**
         * This method should not throw an exception,
         * because the wrapper suppressed the exception.
         */
        wrapper.onException(null);
        assertTrue(executed.get());
    }

}
