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
    private final CountingWebLogger counter = CountingWebLogger.create();

    private final SafeWebLogger safeCounter = new SafeWebLogger(counter);

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

        final WebLogger logger = new BaseWebLogger()
        {
            @Override
            public WebLogger extend ()
            {
                executed.set(true);
                throw new Error();
            }

        };

        final WebLogger wrapper = new SafeWebLogger(logger);

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
        assertSame(SafeWebLogger.NULL_LOGGER, extended);
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

        final WebLogger logger = new BaseWebLogger()
        {
            @Override
            public void onStarted (WebServer server)
            {
                executed.set(true);
                throw new Error();
            }

        };

        final WebLogger wrapper = new SafeWebLogger(logger);

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

        final WebLogger logger = new BaseWebLogger()
        {
            @Override
            public void onStopped ()
            {
                executed.set(true);
                throw new Error();
            }

        };

        final WebLogger wrapper = new SafeWebLogger(logger);

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

        final WebLogger logger = new BaseWebLogger()
        {
            @Override
            public void onConnect (InetSocketAddress local,
                                   InetSocketAddress remote)
            {
                executed.set(true);
                throw new Error();
            }
        };

        final WebLogger wrapper = new SafeWebLogger(logger);

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

        final WebLogger logger = new BaseWebLogger()
        {
            @Override
            public void onDisconnect (InetSocketAddress local,
                                      InetSocketAddress remote)
            {
                executed.set(true);
                throw new Error();
            }
        };

        final WebLogger wrapper = new SafeWebLogger(logger);

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

        final WebLogger logger = new BaseWebLogger()
        {
            @Override
            public void onAccepted (ServerSideHttpRequest request)
            {
                executed.set(true);
                throw new Error();
            }
        };

        final WebLogger wrapper = new SafeWebLogger(logger);

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

        final WebLogger logger = new BaseWebLogger()
        {
            @Override
            public void onRejected (ServerSideHttpRequest request,
                                    final int status)
            {
                executed.set(true);
                throw new Error();
            }
        };

        final WebLogger wrapper = new SafeWebLogger(logger);

        /**
         * This method should not throw an exception,
         * because the wrapper suppressed the exception.
         */
        wrapper.onRejected(null, 0);
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

        final WebLogger logger = new BaseWebLogger()
        {
            @Override
            public void onDenied (ServerSideHttpRequest request)
            {
                executed.set(true);
                throw new Error();
            }
        };

        final WebLogger wrapper = new SafeWebLogger(logger);

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

        final WebLogger logger = new BaseWebLogger()
        {
            @Override
            public void onRequest (ServerSideHttpRequest request)
            {
                executed.set(true);
                throw new Error();
            }
        };

        final WebLogger wrapper = new SafeWebLogger(logger);

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

        final WebLogger logger = new BaseWebLogger()
        {
            @Override
            public void onResponse (ServerSideHttpResponse response)
            {
                executed.set(true);
                throw new Error();
            }
        };

        final WebLogger wrapper = new SafeWebLogger(logger);

        /**
         * This method should not throw an exception,
         * because the wrapper suppressed the exception.
         */
        wrapper.onResponse(null);
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

        final WebLogger logger = new BaseWebLogger()
        {
            @Override
            public void onUplinkTimeout ()
            {
                executed.set(true);
                throw new Error();
            }
        };

        final WebLogger wrapper = new SafeWebLogger(logger);

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

        final WebLogger logger = new BaseWebLogger()
        {
            @Override
            public void onDownlinkTimeout ()
            {
                executed.set(true);
                throw new Error();
            }
        };

        final WebLogger wrapper = new SafeWebLogger(logger);

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

        final WebLogger logger = new BaseWebLogger()
        {
            @Override
            public void onResponseTimeout ()
            {
                executed.set(true);
                throw new Error();
            }
        };

        final WebLogger wrapper = new SafeWebLogger(logger);

        /**
         * This method should not throw an exception,
         * because the wrapper suppressed the exception.
         */
        wrapper.onResponseTimeout();
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

        final WebLogger logger = new BaseWebLogger()
        {
            @Override
            public void onTooManyConnections (int count)
            {
                executed.set(true);
                throw new Error();
            }
        };

        final WebLogger wrapper = new SafeWebLogger(logger);

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

        final WebLogger logger = new BaseWebLogger()
        {
            @Override
            public void onException (Throwable cause)
            {
                executed.set(true);
                throw new Error();
            }
        };

        final WebLogger wrapper = new SafeWebLogger(logger);

        /**
         * This method should not throw an exception,
         * because the wrapper suppressed the exception.
         */
        wrapper.onException(null);
        assertTrue(executed.get());
    }

    /**
     * Test: 20190316225041694886C
     *
     * <p>
     * Method: <code>extend()</code>
     * </p>
     */
    @Test
    public void test20190316225041694886C ()
    {
        assertEquals(0, counter.countExtend());
        final WebLogger extended = safeCounter.extend();
        assertNotSame(safeCounter, extended);
        assertNotNull(extended);
        assertTrue(extended instanceof SafeWebLogger);
        assertEquals(1, counter.countExtend());
    }

    /**
     * Test: 20190316225041694992C
     *
     * <p>
     * Method: <code>onStarted()</code>
     * </p>
     */
    @Test
    public void test20190316225041694992C ()
    {
        assertEquals(0, counter.countOnStarted());
        safeCounter.onStarted(null);
        assertEquals(1, counter.countOnStarted());
    }

    /**
     * Test: 20190316225041695023C
     *
     * <p>
     * Method: <code>onStopped()</code>
     * </p>
     */
    @Test
    public void test20190316225041695023C ()
    {
        assertEquals(0, counter.countOnStopped());
        safeCounter.onStopped();
        assertEquals(1, counter.countOnStopped());
    }

    /**
     * Test: 20190316225041695050C
     *
     * <p>
     * Method: <code>onConnect()</code>
     * </p>
     */
    @Test
    public void test20190316225041695050C ()
    {
        assertEquals(0, counter.countOnConnect());
        safeCounter.onConnect(null, null);
        assertEquals(1, counter.countOnConnect());
    }

    /**
     * Test: 20190316225041695077C
     *
     * <p>
     * Method: <code>onDisconnect()</code>
     * </p>
     */
    @Test
    public void test20190316225041695077C ()
    {
        assertEquals(0, counter.countOnDisconnect());
        safeCounter.onDisconnect(null, null);
        assertEquals(1, counter.countOnDisconnect());
    }

    /**
     * Test: 20190316225041695100C
     *
     * <p>
     * Method: <code>onAccepted()</code>
     * </p>
     */
    @Test
    public void test20190316225041695100C ()
    {
        assertEquals(0, counter.countOnAccepted());
        safeCounter.onAccepted(null);
        assertEquals(1, counter.countOnAccepted());
    }

    /**
     * Test: 20190316225041695123C
     *
     * <p>
     * Method: <code>onRejected()</code>
     * </p>
     */
    @Test
    public void test20190316225041695123C ()
    {
        assertEquals(0, counter.countOnRejected());
        safeCounter.onRejected(null, 0);
        assertEquals(1, counter.countOnRejected());
    }

    /**
     * Test: 20190316225041695150C
     *
     * <p>
     * Method: <code>onDenied()</code>
     * </p>
     */
    @Test
    public void test20190316225041695150C ()
    {
        assertEquals(0, counter.countOnDenied());
        safeCounter.onDenied(null);
        assertEquals(1, counter.countOnDenied());
    }

    /**
     * Test: 20190316225041695174C
     *
     * <p>
     * Method: <code>onRequest()</code>
     * </p>
     */
    @Test
    public void test20190316225041695174C ()
    {
        assertEquals(0, counter.countOnRequest());
        safeCounter.onRequest(null);
        assertEquals(1, counter.countOnRequest());
    }

    /**
     * Test: 20190316225041695196C
     *
     * <p>
     * Method: <code>onResponse()</code>
     * </p>
     */
    @Test
    public void test20190316225041695196C ()
    {
        assertEquals(0, counter.countOnResponse());
        safeCounter.onResponse(null);
        assertEquals(1, counter.countOnResponse());
    }

    /**
     * Test: 20190316231039944835C
     *
     * <p>
     * Method: <code>onUplinkTimeout()</code>
     * </p>
     */
    @Test
    public void test20190316231039944835C ()
    {
        assertEquals(0, counter.countOnUplinkTimeout());
        safeCounter.onUplinkTimeout();
        assertEquals(1, counter.countOnUplinkTimeout());
    }

    /**
     * Test: 20190316231039944862C
     *
     * <p>
     * Method: <code>onDownlinkTimeout()</code>
     * </p>
     */
    @Test
    public void test20190316231039944862C ()
    {
        assertEquals(0, counter.countOnDownlinkTimeout());
        safeCounter.onDownlinkTimeout();
        assertEquals(1, counter.countOnDownlinkTimeout());
    }

    /**
     * Test: 20190316231039944885C
     *
     * <p>
     * Method: <code>onResponseTimeout</code>
     * </p>
     */
    @Test
    public void test20190316231039944885C ()
    {
        assertEquals(0, counter.countOnResponseTimeout());
        safeCounter.onResponseTimeout();
        assertEquals(1, counter.countOnResponseTimeout());
    }

    /**
     * Test: 20190316231039944930C
     *
     * <p>
     * Method: <code>onTooManyConnections()</code>
     * </p>
     */
    @Test
    public void test20190316231039944930C ()
    {
        assertEquals(0, counter.countOnTooManyConnections());
        safeCounter.onTooManyConnections(0);
        assertEquals(1, counter.countOnTooManyConnections());
    }

    /**
     * Test: 20190316231039944949C
     *
     * <p>
     * Method: <code>onException</code>
     * </p>
     */
    @Test
    public void test20190316231039944949C ()
    {
        assertEquals(0, counter.countOnException());
        safeCounter.onException(null);
        assertEquals(1, counter.countOnException());
    }

}
