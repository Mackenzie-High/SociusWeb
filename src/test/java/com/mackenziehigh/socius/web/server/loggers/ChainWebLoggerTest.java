package com.mackenziehigh.socius.web.server.loggers;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class ChainWebLoggerTest
{
    private final CountingWebLogger logger1 = CountingWebLogger.create();

    private final CountingWebLogger logger2 = CountingWebLogger.create();

    private final ChainWebLogger logger = ChainWebLogger.chain(logger1, logger2);

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
        assertEquals(0, logger1.countExtend());
        assertEquals(0, logger2.countExtend());
        assertNotSame(logger, logger.extend());
        assertEquals(1, logger1.countExtend());
        assertEquals(1, logger2.countExtend());
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
        assertEquals(0, logger1.countOnStarted());
        assertEquals(0, logger2.countOnStarted());
        logger.onStarted(null);
        assertEquals(1, logger1.countOnStarted());
        assertEquals(1, logger2.countOnStarted());
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
        assertEquals(0, logger1.countOnStopped());
        assertEquals(0, logger2.countOnStopped());
        logger.onStopped();
        assertEquals(1, logger1.countOnStopped());
        assertEquals(1, logger2.countOnStopped());
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
        assertEquals(0, logger1.countOnConnect());
        assertEquals(0, logger2.countOnConnect());
        logger.onConnect(null, null);
        assertEquals(1, logger1.countOnConnect());
        assertEquals(1, logger2.countOnConnect());
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
        assertEquals(0, logger1.countOnDisconnect());
        assertEquals(0, logger2.countOnDisconnect());
        logger.onDisconnect(null, null);
        assertEquals(1, logger1.countOnDisconnect());
        assertEquals(1, logger2.countOnDisconnect());
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
        assertEquals(0, logger1.countOnAccepted());
        assertEquals(0, logger2.countOnAccepted());
        logger.onAccepted(null);
        assertEquals(1, logger1.countOnAccepted());
        assertEquals(1, logger2.countOnAccepted());
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
        assertEquals(0, logger1.countOnRejected());
        assertEquals(0, logger2.countOnRejected());
        logger.onRejected(null, null);
        assertEquals(1, logger1.countOnRejected());
        assertEquals(1, logger2.countOnRejected());
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
        assertEquals(0, logger1.countOnDenied());
        assertEquals(0, logger2.countOnDenied());
        logger.onDenied(null);
        assertEquals(1, logger1.countOnDenied());
        assertEquals(1, logger2.countOnDenied());
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
        assertEquals(0, logger1.countOnRequest());
        assertEquals(0, logger2.countOnRequest());
        logger.onRequest(null);
        assertEquals(1, logger1.countOnRequest());
        assertEquals(1, logger2.countOnRequest());
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
        assertEquals(0, logger1.countOnResponse());
        assertEquals(0, logger2.countOnResponse());
        logger.onResponse(null, null);
        assertEquals(1, logger1.countOnResponse());
        assertEquals(1, logger2.countOnResponse());
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
        assertEquals(0, logger1.countOnUplinkTimeout());
        assertEquals(0, logger2.countOnUplinkTimeout());
        logger.onUplinkTimeout();
        assertEquals(1, logger1.countOnUplinkTimeout());
        assertEquals(1, logger2.countOnUplinkTimeout());
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
        assertEquals(0, logger1.countOnDownlinkTimeout());
        assertEquals(0, logger2.countOnDownlinkTimeout());
        logger.onDownlinkTimeout();
        assertEquals(1, logger1.countOnDownlinkTimeout());
        assertEquals(1, logger2.countOnDownlinkTimeout());
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
        assertEquals(0, logger1.countOnResponseTimeout());
        assertEquals(0, logger2.countOnResponseTimeout());
        logger.onResponseTimeout();
        assertEquals(1, logger1.countOnResponseTimeout());
        assertEquals(1, logger2.countOnResponseTimeout());
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
        assertEquals(0, logger1.countOnConnectionTimeout());
        assertEquals(0, logger2.countOnConnectionTimeout());
        logger.onConnectionTimeout();
        assertEquals(1, logger1.countOnConnectionTimeout());
        assertEquals(1, logger2.countOnConnectionTimeout());
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
        assertEquals(0, logger1.countOnTooManyConnections());
        assertEquals(0, logger2.countOnTooManyConnections());
        logger.onTooManyConnections(0);
        assertEquals(1, logger1.countOnTooManyConnections());
        assertEquals(1, logger2.countOnTooManyConnections());
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
        assertEquals(0, logger1.countOnException());
        assertEquals(0, logger2.countOnException());
        logger.onException(null);
        assertEquals(1, logger1.countOnException());
        assertEquals(1, logger2.countOnException());
    }
}
