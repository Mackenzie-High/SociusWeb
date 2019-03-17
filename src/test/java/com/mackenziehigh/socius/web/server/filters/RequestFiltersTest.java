package com.mackenziehigh.socius.web.server.filters;

import com.mackenziehigh.socius.web.messages.web_m;
import com.mackenziehigh.socius.web.messages.web_m.HttpProtocol;
import com.mackenziehigh.socius.web.messages.web_m.ServerSideHttpRequest;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class RequestFiltersTest
{
    private final HttpProtocol PROTOCOL = web_m.HttpProtocol.newBuilder()
            .setMajorVersion(1)
            .setMinorVersion(0)
            .setName("HTTP")
            .setText("HTTP/1.0")
            .build();

    private final ServerSideHttpRequest GET = ServerSideHttpRequest
            .newBuilder()
            .setTimestamp(System.currentTimeMillis())
            .setMethod("GET")
            .setUri("/index.html")
            .setProtocol(PROTOCOL)
            .build();

    /**
     * Test: 20190317163724410282
     *
     * <p>
     * Case: Verify that the singletons are in-fact singletons.
     * </p>
     */
    @Test
    public void test20190317163724410282 ()
    {
        assertSame(RequestFilters.accept(), RequestFilters.accept());
        assertSame(RequestFilters.deny(), RequestFilters.deny());
        assertSame(RequestFilters.forward(), RequestFilters.forward());
    }

    /**
     * Test: 20190317154137023834
     *
     * <p>
     * Method: <code>accept()</code>
     * </p>
     *
     * <p>
     * Case: Always.
     * </p>
     */
    @Test
    public void test20190317154137023834 ()
    {
        final RequestFilter.Action action = RequestFilters.accept().apply(GET);

        assertEquals(RequestFilter.ActionType.ACCEPT, action.action());
        assertFalse(action.status().isPresent());
    }

    /**
     * Test: 20190317154137023905
     *
     * <p>
     * Method: <code>accept(Predicate)</code>
     * </p>
     *
     * <p>
     * Case: Accept.
     * </p>
     */
    @Test
    public void test20190317154137023905 ()
    {
        final RequestFilter.Action action = RequestFilters.accept(x -> x.getMethod().equalsIgnoreCase("GET")).apply(GET);

        assertEquals(RequestFilter.ActionType.ACCEPT, action.action());
        assertFalse(action.status().isPresent());
    }

    /**
     * Test: 20190317154137023930
     *
     * <p>
     * Method: <code>accept(Predicate)</code>
     * </p>
     *
     * <p>
     * Case: Forward.
     * </p>
     */
    @Test
    public void test20190317154137023930 ()
    {
        final RequestFilter.Action action = RequestFilters.accept(x -> x.getMethod().equalsIgnoreCase("POST")).apply(GET);

        assertEquals(RequestFilter.ActionType.FORWARD, action.action());
        assertFalse(action.status().isPresent());
    }

    /**
     * Test: 20190317154137023972
     *
     * <p>
     * Method: <code>deny()</code>
     * </p>
     *
     * <p>
     * Case: Always.
     * </p>
     */
    @Test
    public void test20190317154137023972 ()
    {
        final RequestFilter.Action action = RequestFilters.deny().apply(GET);

        assertEquals(RequestFilter.ActionType.DENY, action.action());
        assertFalse(action.status().isPresent());
    }

    /**
     * Test: 20190317154137024012
     *
     * <p>
     * Method: <code>deny(Predicate)</code>
     * </p>
     *
     * <p>
     * Case: Deny.
     * </p>
     */
    @Test
    public void test20190317154137024012 ()
    {
        final RequestFilter.Action action = RequestFilters.deny(x -> x.getMethod().equalsIgnoreCase("GET")).apply(GET);

        assertEquals(RequestFilter.ActionType.DENY, action.action());
        assertFalse(action.status().isPresent());
    }

    /**
     * Test: 20190317154137024031
     *
     * <p>
     * Method: <code>deny(Predicate)</code>
     * </p>
     *
     * <p>
     * Case: Forward.
     * </p>
     */
    @Test
    public void test20190317154137024031 ()
    {
        final RequestFilter.Action action = RequestFilters.deny(x -> x.getMethod().equalsIgnoreCase("POST")).apply(GET);

        assertEquals(RequestFilter.ActionType.FORWARD, action.action());
        assertFalse(action.status().isPresent());
    }

    /**
     * Test: 20190317161709644308A
     *
     * <p>
     * Method: <code>reject(int)</code>
     * </p>
     *
     * <p>
     * Case: The status-code is above (599).
     * </p>
     */
    @Test (expected = IllegalArgumentException.class)
    public void test20190317161709644308A ()
    {
        RequestFilters.reject(600);
    }

    /**
     * Test: 20190317161709644308B
     *
     * <p>
     * Method: <code>reject(int)</code>
     * </p>
     *
     * <p>
     * Case: The status-code is below (400).
     * </p>
     */
    @Test (expected = IllegalArgumentException.class)
    public void test20190317161709644308B ()
    {
        RequestFilters.reject(399);
    }

    /**
     * Test: 20190317154137024050
     *
     * <p>
     * Method: <code>reject(int)</code>
     * </p>
     *
     * <p>
     * Case: Always.
     * </p>
     */
    @Test
    public void test20190317154137024050 ()
    {
        final RequestFilter.Action action = RequestFilters.reject(413).apply(GET);

        assertEquals(RequestFilter.ActionType.REJECT, action.action());
        assertEquals(413, action.status().getAsInt());
    }

    /**
     * Test: 20190317154137024068
     *
     * <p>
     * Method: <code>reject(int, Predicate)</code>
     * </p>
     *
     * <p>
     * Case: Reject.
     * </p>
     */
    @Test
    public void test20190317154137024068 ()
    {
        final RequestFilter.Action action = RequestFilters.reject(405, x -> x.getMethod().equalsIgnoreCase("GET")).apply(GET);

        assertEquals(RequestFilter.ActionType.REJECT, action.action());
        assertEquals(405, action.status().getAsInt());
    }

    /**
     * Test: 20190317154137024086
     *
     * <p>
     * Method: <code>reject(int, Predicate)</code>
     * </p>
     *
     * <p>
     * Case: Forward.
     * </p>
     */
    @Test
    public void test20190317154137024086 ()
    {
        final RequestFilter.Action action = RequestFilters.reject(405, x -> x.getMethod().equalsIgnoreCase("POST")).apply(GET);

        assertEquals(RequestFilter.ActionType.FORWARD, action.action());
        assertFalse(action.status().isPresent());
    }

    /**
     * Test: 20190317160930488269
     *
     * <p>
     * Method: <code>forward()</code>
     * </p>
     *
     * <p>
     * Case: Always.
     * </p>
     */
    @Test
    public void test20190317160930488269 ()
    {
        final RequestFilter.Action action = RequestFilters.forward().apply(GET);

        assertEquals(RequestFilter.ActionType.FORWARD, action.action());
        assertFalse(action.status().isPresent());
    }

    /**
     * Test: 20190317160930488353
     *
     * <p>
     * Method: <code>chain(RequestFilter, RequestFilter)</code>
     * </p>
     *
     * <p>
     * Case: The left-operand specifies an action to perform.
     * </p>
     */
    @Test
    public void test20190317160930488353 ()
    {
        final RequestFilter left = RequestFilters.accept();
        final RequestFilter right = RequestFilters.deny();
        final RequestFilter chain = RequestFilters.chain(left, right);

        final RequestFilter.Action action = chain.apply(GET);

        assertEquals(RequestFilter.ActionType.ACCEPT, action.action());
        assertFalse(action.status().isPresent());
    }

    /**
     * Test: 20190317160930488382
     *
     * <p>
     * Method: <code>chain(RequestFilter, RequestFilter)</code>
     * </p>
     *
     * <p>
     * Case: The right-operand specifies a non-forwarding action to perform.
     * </p>
     */
    @Test
    public void test20190317160930488382 ()
    {
        final RequestFilter left = RequestFilters.forward();
        final RequestFilter right = RequestFilters.deny();
        final RequestFilter chain = RequestFilters.chain(left, right);

        final RequestFilter.Action action = chain.apply(GET);

        assertEquals(RequestFilter.ActionType.DENY, action.action());
        assertFalse(action.status().isPresent());
    }

    /**
     * Test: 20190317161345101320
     *
     * <p>
     * Method: <code>chain(RequestFilter, RequestFilter)</code>
     * </p>
     *
     * <p>
     * Case: The right-operand specifies forwarding should occur.
     * </p>
     */
    @Test
    public void test20190317161345101320 ()
    {
        final RequestFilter left = RequestFilters.forward();
        final RequestFilter right = RequestFilters.forward();
        final RequestFilter chain = RequestFilters.chain(left, right);

        final RequestFilter.Action action = chain.apply(GET);

        assertEquals(RequestFilter.ActionType.FORWARD, action.action());
        assertFalse(action.status().isPresent());
    }
}
