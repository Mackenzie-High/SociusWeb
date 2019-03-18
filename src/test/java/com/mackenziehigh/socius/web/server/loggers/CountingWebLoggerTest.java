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

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class CountingWebLoggerTest
{
    private final CountingWebLogger logger = CountingWebLogger.create();

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
        assertEquals(0, logger.countExtend());
        assertSame(logger, logger.extend());
        assertEquals(1, logger.countExtend());
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
        assertEquals(0, logger.countOnStarted());
        logger.onStarted(null);
        assertEquals(1, logger.countOnStarted());
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
        assertEquals(0, logger.countOnStopped());
        logger.onStopped();
        assertEquals(1, logger.countOnStopped());
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
        assertEquals(0, logger.countOnConnect());
        logger.onConnect(null, null);
        assertEquals(1, logger.countOnConnect());
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
        assertEquals(0, logger.countOnDisconnect());
        logger.onDisconnect(null, null);
        assertEquals(1, logger.countOnDisconnect());
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
        assertEquals(0, logger.countOnAccepted());
        logger.onAccepted(null);
        assertEquals(1, logger.countOnAccepted());
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
        assertEquals(0, logger.countOnRejected());
        logger.onRejected(null, 0);
        assertEquals(1, logger.countOnRejected());
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
        assertEquals(0, logger.countOnDenied());
        logger.onDenied(null);
        assertEquals(1, logger.countOnDenied());
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
        assertEquals(0, logger.countOnRequest());
        logger.onRequest(null);
        assertEquals(1, logger.countOnRequest());
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
        assertEquals(0, logger.countOnResponse());
        logger.onResponse(null);
        assertEquals(1, logger.countOnResponse());
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
        assertEquals(0, logger.countOnUplinkTimeout());
        logger.onUplinkTimeout();
        assertEquals(1, logger.countOnUplinkTimeout());
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
        assertEquals(0, logger.countOnDownlinkTimeout());
        logger.onDownlinkTimeout();
        assertEquals(1, logger.countOnDownlinkTimeout());
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
        assertEquals(0, logger.countOnResponseTimeout());
        logger.onResponseTimeout();
        assertEquals(1, logger.countOnResponseTimeout());
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
        assertEquals(0, logger.countOnConnectionTimeout());
        logger.onConnectionTimeout();
        assertEquals(1, logger.countOnConnectionTimeout());
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
        assertEquals(0, logger.countOnTooManyConnections());
        logger.onTooManyConnections(0);
        assertEquals(1, logger.countOnTooManyConnections());
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
        assertEquals(0, logger.countOnException());
        logger.onException(null);
        assertEquals(1, logger.countOnException());
    }

}
