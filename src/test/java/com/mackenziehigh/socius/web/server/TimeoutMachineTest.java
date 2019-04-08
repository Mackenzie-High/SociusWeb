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

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class TimeoutMachineTest
{
    private final String READ_TIMEOUT = "READ TIMEOUT";

    private final String RESPONSE_TIMEOUT = "RESPONSE TIMEOUT";

    private final String WRITE_TIMEOUT = "WRITE TIMEOUT";

    private final EventLoopGroup executor = new NioEventLoopGroup();

    private TimeoutMachine tester;

    private final List<Object> callbacks = new CopyOnWriteArrayList<>();

    @After
    public void destroy ()
    {
        executor.shutdownGracefully();
    }

    private void setup (final int readTimeout,
                        final int responseTimeout,
                        final int writeTimeout)
    {
        tester = new TimeoutMachine(executor, Duration.ofMillis(readTimeout), Duration.ofMillis(responseTimeout), Duration.ofMillis(writeTimeout))
        {
            @Override
            public void onReadTimeout ()
            {
                callbacks.add(READ_TIMEOUT);
            }

            @Override
            public void onResponseTimeout ()
            {
                callbacks.add(RESPONSE_TIMEOUT);
            }

            @Override
            public void onWriteTimeout ()
            {
                callbacks.add(WRITE_TIMEOUT);
            }
        };
    }

    /**
     * Test Case: INITIAL_INIT
     */
    @Test
    public void test_INITIAL_INIT ()
    {
        setup(1000, 1000, 1000);

        assertEquals(tester.state(), TimeoutMachine.States.INITIAL);
        tester.reactTo(TimeoutMachine.Events.INIT);
        assertEquals(tester.state(), TimeoutMachine.States.READING);

        assertTrue(callbacks.isEmpty());
    }

    /**
     * Test Case: READING_INIT
     */
    @Test (expected = IllegalStateException.class)
    public void test_READING_INIT ()
    {
        setup(1000, 1000, 1000);

        assertEquals(tester.state(), TimeoutMachine.States.INITIAL);
        tester.reactTo(TimeoutMachine.Events.INIT);
        assertEquals(tester.state(), TimeoutMachine.States.READING);
        tester.reactTo(TimeoutMachine.Events.INIT);
    }

    /**
     * Test Case: RESPONDING_INIT
     */
    @Test (expected = IllegalStateException.class)
    public void test_RESPONDING_INIT ()
    {
        setup(1000, 1000, 1000);

        assertEquals(tester.state(), TimeoutMachine.States.INITIAL);
        tester.reactTo(TimeoutMachine.Events.INIT);
        assertEquals(tester.state(), TimeoutMachine.States.READING);
        tester.reactTo(TimeoutMachine.Events.READ_COMPLETE);
        assertEquals(tester.state(), TimeoutMachine.States.RESPONDING);
        tester.reactTo(TimeoutMachine.Events.INIT);
    }

    /**
     * Test Case: WRITING_INIT
     */
    @Test (expected = IllegalStateException.class)
    public void test_WRITING_INIT ()
    {
        setup(1000, 1000, 1000);

        assertEquals(tester.state(), TimeoutMachine.States.INITIAL);
        tester.reactTo(TimeoutMachine.Events.INIT);
        assertEquals(tester.state(), TimeoutMachine.States.READING);
        tester.reactTo(TimeoutMachine.Events.READ_COMPLETE);
        assertEquals(tester.state(), TimeoutMachine.States.RESPONDING);
        tester.reactTo(TimeoutMachine.Events.RESPONSE_COMPLETE);
        assertEquals(tester.state(), TimeoutMachine.States.WRITING);
        tester.reactTo(TimeoutMachine.Events.INIT);
    }

    /**
     * Test Case: CLOSED_INIT
     */
    @Test (expected = IllegalStateException.class)
    public void test_CLOSED_INIT ()
    {
        setup(1000, 1000, 1000);

        assertEquals(tester.state(), TimeoutMachine.States.INITIAL);
        tester.reactTo(TimeoutMachine.Events.INIT);
        assertEquals(tester.state(), TimeoutMachine.States.READING);
        tester.reactTo(TimeoutMachine.Events.READ_COMPLETE);
        assertEquals(tester.state(), TimeoutMachine.States.RESPONDING);
        tester.reactTo(TimeoutMachine.Events.RESPONSE_COMPLETE);
        assertEquals(tester.state(), TimeoutMachine.States.WRITING);
        tester.reactTo(TimeoutMachine.Events.WRITE_COMPLETE);
        assertEquals(tester.state(), TimeoutMachine.States.CLOSED);
        tester.reactTo(TimeoutMachine.Events.INIT);
    }

    /**
     * Test Case: INITIAL_READ_COMPLETE
     */
    @Test (expected = IllegalStateException.class)
    public void test_INITIAL_READ_COMPLETE ()
    {
        setup(1000, 1000, 1000);

        assertEquals(tester.state(), TimeoutMachine.States.INITIAL);
        tester.reactTo(TimeoutMachine.Events.READ_COMPLETE);
    }

    /**
     * Test Case: READING_READ_COMPLETE
     */
    @Test
    public void test_READING_READ_COMPLETE ()
    {
        setup(1000, 1000, 1000);

        assertEquals(tester.state(), TimeoutMachine.States.INITIAL);
        tester.reactTo(TimeoutMachine.Events.INIT);
        assertEquals(tester.state(), TimeoutMachine.States.READING);
        tester.reactTo(TimeoutMachine.Events.READ_COMPLETE);
        assertEquals(tester.state(), TimeoutMachine.States.RESPONDING);

        assertTrue(callbacks.isEmpty());
    }

    /**
     * Test Case: RESPONDING_READ_COMPLETE
     */
    @Test (expected = IllegalStateException.class)
    public void test_RESPONDING_READ_COMPLETE ()
    {
        setup(1000, 1000, 1000);

        assertEquals(tester.state(), TimeoutMachine.States.INITIAL);
        tester.reactTo(TimeoutMachine.Events.INIT);
        assertEquals(tester.state(), TimeoutMachine.States.READING);
        tester.reactTo(TimeoutMachine.Events.READ_COMPLETE);
        assertEquals(tester.state(), TimeoutMachine.States.RESPONDING);
        tester.reactTo(TimeoutMachine.Events.READ_COMPLETE);
    }

    /**
     * Test Case: WRITING_READ_COMPLETE
     */
    @Test (expected = IllegalStateException.class)
    public void test_WRITING_READ_COMPLETE ()
    {
        setup(1000, 1000, 1000);

        assertEquals(tester.state(), TimeoutMachine.States.INITIAL);
        tester.reactTo(TimeoutMachine.Events.INIT);
        assertEquals(tester.state(), TimeoutMachine.States.READING);
        tester.reactTo(TimeoutMachine.Events.READ_COMPLETE);
        assertEquals(tester.state(), TimeoutMachine.States.RESPONDING);
        tester.reactTo(TimeoutMachine.Events.RESPONSE_COMPLETE);
        assertEquals(tester.state(), TimeoutMachine.States.WRITING);
        tester.reactTo(TimeoutMachine.Events.READ_COMPLETE);
    }

    /**
     * Test Case: CLOSED_READ_COMPLETE
     */
    @Test (expected = IllegalStateException.class)
    public void test_CLOSED_READ_COMPLETE ()
    {
        setup(1000, 1000, 1000);

        assertEquals(tester.state(), TimeoutMachine.States.INITIAL);
        tester.reactTo(TimeoutMachine.Events.INIT);
        assertEquals(tester.state(), TimeoutMachine.States.READING);
        tester.reactTo(TimeoutMachine.Events.READ_COMPLETE);
        assertEquals(tester.state(), TimeoutMachine.States.RESPONDING);
        tester.reactTo(TimeoutMachine.Events.RESPONSE_COMPLETE);
        assertEquals(tester.state(), TimeoutMachine.States.WRITING);
        tester.reactTo(TimeoutMachine.Events.WRITE_COMPLETE);
        assertEquals(tester.state(), TimeoutMachine.States.CLOSED);
        tester.reactTo(TimeoutMachine.Events.READ_COMPLETE);
    }

    /**
     * Test Case: INITIAL_READ_TIMEOUT
     */
    @Test (expected = IllegalStateException.class)
    public void test_INITIAL_READ_TIMEOUT ()
    {
        setup(1000, 1000, 1000);

        assertEquals(tester.state(), TimeoutMachine.States.INITIAL);
        tester.reactTo(TimeoutMachine.Events.READ_TIMEOUT);
    }

    /**
     * Test Case: READING_READ_TIMEOUT
     */
    @Test
    public void test_READING_READ_TIMEOUT ()
    {
        setup(1000, 1000, 1000);

        assertEquals(tester.state(), TimeoutMachine.States.INITIAL);
        tester.reactTo(TimeoutMachine.Events.INIT);
        assertEquals(tester.state(), TimeoutMachine.States.READING);
        tester.reactTo(TimeoutMachine.Events.READ_TIMEOUT);
        assertEquals(1, callbacks.size());
        assertTrue(callbacks.contains(READ_TIMEOUT));
        assertEquals(tester.state(), TimeoutMachine.States.CLOSED);
    }

    /**
     * Test Case: RESPONDING_READ_TIMEOUT
     */
    @Test
    public void test_RESPONDING_READ_TIMEOUT ()
    {
        setup(1000, 1000, 1000);

        assertEquals(tester.state(), TimeoutMachine.States.INITIAL);
        tester.reactTo(TimeoutMachine.Events.INIT);
        assertEquals(tester.state(), TimeoutMachine.States.READING);
        tester.reactTo(TimeoutMachine.Events.READ_COMPLETE);
        assertEquals(tester.state(), TimeoutMachine.States.RESPONDING);
        tester.reactTo(TimeoutMachine.Events.READ_TIMEOUT);
        assertTrue(callbacks.isEmpty());
        assertEquals(tester.state(), TimeoutMachine.States.RESPONDING);
    }

    /**
     * Test Case: WRITING_READ_TIMEOUT
     */
    @Test
    public void test_WRITING_READ_TIMEOUT ()
    {
        setup(1000, 1000, 1000);

        assertEquals(tester.state(), TimeoutMachine.States.INITIAL);
        tester.reactTo(TimeoutMachine.Events.INIT);
        assertEquals(tester.state(), TimeoutMachine.States.READING);
        tester.reactTo(TimeoutMachine.Events.READ_COMPLETE);
        assertEquals(tester.state(), TimeoutMachine.States.RESPONDING);
        tester.reactTo(TimeoutMachine.Events.RESPONSE_COMPLETE);
        assertEquals(tester.state(), TimeoutMachine.States.WRITING);
        tester.reactTo(TimeoutMachine.Events.READ_TIMEOUT);
        assertTrue(callbacks.isEmpty());
        assertEquals(tester.state(), TimeoutMachine.States.WRITING);
    }

    /**
     * Test Case: CLOSED_READ_TIMEOUT
     */
    @Test
    public void test_CLOSED_READ_TIMEOUT ()
    {
        setup(1000, 1000, 1000);

        assertEquals(tester.state(), TimeoutMachine.States.INITIAL);
        tester.reactTo(TimeoutMachine.Events.INIT);
        assertEquals(tester.state(), TimeoutMachine.States.READING);
        tester.reactTo(TimeoutMachine.Events.READ_COMPLETE);
        assertEquals(tester.state(), TimeoutMachine.States.RESPONDING);
        tester.reactTo(TimeoutMachine.Events.RESPONSE_COMPLETE);
        assertEquals(tester.state(), TimeoutMachine.States.WRITING);
        tester.reactTo(TimeoutMachine.Events.WRITE_COMPLETE);
        assertEquals(tester.state(), TimeoutMachine.States.CLOSED);
        tester.reactTo(TimeoutMachine.Events.READ_TIMEOUT);
        assertTrue(callbacks.isEmpty());
        assertEquals(tester.state(), TimeoutMachine.States.CLOSED);
    }

    /**
     * Test Case: INITIAL_RESPONSE_COMPLETE
     */
    @Test (expected = IllegalStateException.class)
    public void test_INITIAL_RESPONSE_COMPLETE ()
    {
        setup(1000, 1000, 1000);

        assertEquals(tester.state(), TimeoutMachine.States.INITIAL);
        tester.reactTo(TimeoutMachine.Events.RESPONSE_COMPLETE);
    }

    /**
     * Test Case: READING_RESPONSE_COMPLETE
     */
    @Test (expected = IllegalStateException.class)
    public void test_READING_RESPONSE_COMPLETE ()
    {
        setup(1000, 1000, 1000);

        assertEquals(tester.state(), TimeoutMachine.States.INITIAL);
        tester.reactTo(TimeoutMachine.Events.INIT);
        assertEquals(tester.state(), TimeoutMachine.States.READING);
        tester.reactTo(TimeoutMachine.Events.RESPONSE_COMPLETE);
    }

    /**
     * Test Case: RESPONDING_RESPONSE_COMPLETE
     */
    @Test
    public void test_RESPONDING_RESPONSE_COMPLETE ()
    {
        setup(1000, 1000, 1000);

        assertEquals(tester.state(), TimeoutMachine.States.INITIAL);
        tester.reactTo(TimeoutMachine.Events.INIT);
        assertEquals(tester.state(), TimeoutMachine.States.READING);
        tester.reactTo(TimeoutMachine.Events.READ_COMPLETE);
        assertEquals(tester.state(), TimeoutMachine.States.RESPONDING);
        tester.reactTo(TimeoutMachine.Events.RESPONSE_COMPLETE);
        assertEquals(tester.state(), TimeoutMachine.States.WRITING);

        assertTrue(callbacks.isEmpty());
    }

    /**
     * Test Case: WRITING_RESPONSE_COMPLETE
     */
    @Test (expected = IllegalStateException.class)
    public void test_WRITING_RESPONSE_COMPLETE ()
    {
        setup(1000, 1000, 1000);

        assertEquals(tester.state(), TimeoutMachine.States.INITIAL);
        tester.reactTo(TimeoutMachine.Events.INIT);
        assertEquals(tester.state(), TimeoutMachine.States.READING);
        tester.reactTo(TimeoutMachine.Events.READ_COMPLETE);
        assertEquals(tester.state(), TimeoutMachine.States.RESPONDING);
        tester.reactTo(TimeoutMachine.Events.RESPONSE_COMPLETE);
        assertEquals(tester.state(), TimeoutMachine.States.WRITING);
        tester.reactTo(TimeoutMachine.Events.RESPONSE_COMPLETE);
    }

    /**
     * Test Case: CLOSED_RESPONSE_COMPLETE
     */
    @Test (expected = IllegalStateException.class)
    public void test_CLOSED_RESPONSE_COMPLETE ()
    {
        setup(1000, 1000, 1000);

        assertEquals(tester.state(), TimeoutMachine.States.INITIAL);
        tester.reactTo(TimeoutMachine.Events.INIT);
        assertEquals(tester.state(), TimeoutMachine.States.READING);
        tester.reactTo(TimeoutMachine.Events.READ_COMPLETE);
        assertEquals(tester.state(), TimeoutMachine.States.RESPONDING);
        tester.reactTo(TimeoutMachine.Events.RESPONSE_COMPLETE);
        assertEquals(tester.state(), TimeoutMachine.States.WRITING);
        tester.reactTo(TimeoutMachine.Events.WRITE_COMPLETE);
        assertEquals(tester.state(), TimeoutMachine.States.CLOSED);
        tester.reactTo(TimeoutMachine.Events.RESPONSE_COMPLETE);
    }

    /**
     * Test Case: INITIAL_RESPONSE_TIMEOUT
     */
    @Test (expected = IllegalStateException.class)
    public void test_INITIAL_RESPONSE_TIMEOUT ()
    {
        setup(1000, 1000, 1000);

        assertEquals(tester.state(), TimeoutMachine.States.INITIAL);
        tester.reactTo(TimeoutMachine.Events.RESPONSE_TIMEOUT);
    }

    /**
     * Test Case: READING_RESPONSE_TIMEOUT
     */
    @Test (expected = IllegalStateException.class)
    public void test_READING_RESPONSE_TIMEOUT ()
    {
        setup(1000, 1000, 1000);

        assertEquals(tester.state(), TimeoutMachine.States.INITIAL);
        tester.reactTo(TimeoutMachine.Events.INIT);
        assertEquals(tester.state(), TimeoutMachine.States.READING);
        tester.reactTo(TimeoutMachine.Events.RESPONSE_TIMEOUT);
    }

    /**
     * Test Case: RESPONDING_RESPONSE_TIMEOUT
     */
    @Test
    public void test_RESPONDING_RESPONSE_TIMEOUT ()
    {
        setup(1000, 1000, 1000);

        assertEquals(tester.state(), TimeoutMachine.States.INITIAL);
        tester.reactTo(TimeoutMachine.Events.INIT);
        assertEquals(tester.state(), TimeoutMachine.States.READING);
        tester.reactTo(TimeoutMachine.Events.READ_COMPLETE);
        assertEquals(tester.state(), TimeoutMachine.States.RESPONDING);
        tester.reactTo(TimeoutMachine.Events.RESPONSE_TIMEOUT);
        assertEquals(1, callbacks.size());
        assertTrue(callbacks.contains(RESPONSE_TIMEOUT));
        assertEquals(tester.state(), TimeoutMachine.States.WRITING);
    }

    /**
     * Test Case: WRITING_RESPONSE_TIMEOUT
     */
    @Test
    public void test_WRITING_RESPONSE_TIMEOUT ()
    {
        setup(1000, 1000, 1000);

        assertEquals(tester.state(), TimeoutMachine.States.INITIAL);
        tester.reactTo(TimeoutMachine.Events.INIT);
        assertEquals(tester.state(), TimeoutMachine.States.READING);
        tester.reactTo(TimeoutMachine.Events.READ_COMPLETE);
        assertEquals(tester.state(), TimeoutMachine.States.RESPONDING);
        tester.reactTo(TimeoutMachine.Events.RESPONSE_COMPLETE);
        assertEquals(tester.state(), TimeoutMachine.States.WRITING);
        tester.reactTo(TimeoutMachine.Events.RESPONSE_TIMEOUT);
        assertTrue(callbacks.isEmpty());
        assertEquals(tester.state(), TimeoutMachine.States.WRITING);
    }

    /**
     * Test Case: CLOSED_RESPONSE_TIMEOUT
     */
    @Test
    public void test_CLOSED_RESPONSE_TIMEOUT ()
    {
        setup(1000, 1000, 1000);

        assertEquals(tester.state(), TimeoutMachine.States.INITIAL);
        tester.reactTo(TimeoutMachine.Events.INIT);
        assertEquals(tester.state(), TimeoutMachine.States.READING);
        tester.reactTo(TimeoutMachine.Events.READ_COMPLETE);
        assertEquals(tester.state(), TimeoutMachine.States.RESPONDING);
        tester.reactTo(TimeoutMachine.Events.RESPONSE_COMPLETE);
        assertEquals(tester.state(), TimeoutMachine.States.WRITING);
        tester.reactTo(TimeoutMachine.Events.WRITE_COMPLETE);
        assertEquals(tester.state(), TimeoutMachine.States.CLOSED);
        tester.reactTo(TimeoutMachine.Events.RESPONSE_TIMEOUT);
        assertTrue(callbacks.isEmpty());
        assertEquals(tester.state(), TimeoutMachine.States.CLOSED);
    }

    /**
     * Test Case: INITIAL_WRITE_TIMEOUT
     */
    @Test (expected = IllegalStateException.class)
    public void test_INITIAL_WRITE_TIMEOUT ()
    {
        setup(1000, 1000, 1000);

        assertEquals(tester.state(), TimeoutMachine.States.INITIAL);
        tester.reactTo(TimeoutMachine.Events.WRITE_TIMEOUT);
    }

    /**
     * Test Case: READING_WRITE_TIMEOUT
     */
    @Test (expected = IllegalStateException.class)
    public void test_READING_WRITE_TIMEOUT ()
    {
        setup(1000, 1000, 1000);

        assertEquals(tester.state(), TimeoutMachine.States.INITIAL);
        tester.reactTo(TimeoutMachine.Events.INIT);
        assertEquals(tester.state(), TimeoutMachine.States.READING);
        tester.reactTo(TimeoutMachine.Events.WRITE_TIMEOUT);
    }

    /**
     * Test Case: RESPONDING_WRITE_TIMEOUT
     */
    @Test (expected = IllegalStateException.class)
    public void test_RESPONDING_WRITE_TIMEOUT ()
    {
        setup(1000, 1000, 1000);

        assertEquals(tester.state(), TimeoutMachine.States.INITIAL);
        tester.reactTo(TimeoutMachine.Events.INIT);
        assertEquals(tester.state(), TimeoutMachine.States.READING);
        tester.reactTo(TimeoutMachine.Events.READ_COMPLETE);
        assertEquals(tester.state(), TimeoutMachine.States.RESPONDING);
        tester.reactTo(TimeoutMachine.Events.WRITE_TIMEOUT);
    }

    /**
     * Test Case: WRITING_WRITE_TIMEOUT
     */
    @Test
    public void test_WRITING_WRITE_TIMEOUT ()
    {
        setup(1000, 1000, 1000);

        assertEquals(tester.state(), TimeoutMachine.States.INITIAL);
        tester.reactTo(TimeoutMachine.Events.INIT);
        assertEquals(tester.state(), TimeoutMachine.States.READING);
        tester.reactTo(TimeoutMachine.Events.READ_COMPLETE);
        assertEquals(tester.state(), TimeoutMachine.States.RESPONDING);
        tester.reactTo(TimeoutMachine.Events.RESPONSE_COMPLETE);
        assertEquals(tester.state(), TimeoutMachine.States.WRITING);
        tester.reactTo(TimeoutMachine.Events.WRITE_TIMEOUT);
        assertEquals(1, callbacks.size());
        assertTrue(callbacks.contains(WRITE_TIMEOUT));
        assertEquals(tester.state(), TimeoutMachine.States.CLOSED);
    }

    /**
     * Test Case: CLOSED_WRITE_TIMEOUT
     */
    @Test
    public void test_CLOSED_WRITE_TIMEOUT ()
    {
        setup(1000, 1000, 1000);

        assertEquals(tester.state(), TimeoutMachine.States.INITIAL);
        tester.reactTo(TimeoutMachine.Events.INIT);
        assertEquals(tester.state(), TimeoutMachine.States.READING);
        tester.reactTo(TimeoutMachine.Events.READ_COMPLETE);
        assertEquals(tester.state(), TimeoutMachine.States.RESPONDING);
        tester.reactTo(TimeoutMachine.Events.RESPONSE_COMPLETE);
        assertEquals(tester.state(), TimeoutMachine.States.WRITING);
        tester.reactTo(TimeoutMachine.Events.WRITE_COMPLETE);
        assertEquals(tester.state(), TimeoutMachine.States.CLOSED);
        tester.reactTo(TimeoutMachine.Events.WRITE_TIMEOUT);
        assertTrue(callbacks.isEmpty());
        assertEquals(tester.state(), TimeoutMachine.States.CLOSED);
    }

    /**
     * Test Case: INITIAL_WRITE_COMPLETE
     */
    @Test
    public void test_INITIAL_WRITE_COMPLETE ()
    {
        setup(1000, 1000, 1000);

        assertEquals(tester.state(), TimeoutMachine.States.INITIAL);
        tester.reactTo(TimeoutMachine.Events.WRITE_COMPLETE);
        assertEquals(tester.state(), TimeoutMachine.States.CLOSED);
        assertTrue(callbacks.isEmpty());
    }

    /**
     * Test Case: READING_WRITE_COMPLETE
     */
    @Test
    public void test_READING_WRITE_COMPLETE ()
    {
        setup(1000, 1000, 1000);

        assertEquals(tester.state(), TimeoutMachine.States.INITIAL);
        tester.reactTo(TimeoutMachine.Events.INIT);
        assertEquals(tester.state(), TimeoutMachine.States.READING);
        tester.reactTo(TimeoutMachine.Events.WRITE_COMPLETE);
        assertEquals(tester.state(), TimeoutMachine.States.CLOSED);
        assertTrue(callbacks.isEmpty());
    }

    /**
     * Test Case: RESPONDING_WRITE_COMPLETE
     */
    @Test
    public void test_RESPONDING_WRITE_COMPLETE ()
    {
        setup(1000, 1000, 1000);

        assertEquals(tester.state(), TimeoutMachine.States.INITIAL);
        tester.reactTo(TimeoutMachine.Events.INIT);
        assertEquals(tester.state(), TimeoutMachine.States.READING);
        tester.reactTo(TimeoutMachine.Events.READ_COMPLETE);
        assertEquals(tester.state(), TimeoutMachine.States.RESPONDING);
        tester.reactTo(TimeoutMachine.Events.WRITE_COMPLETE);
        assertEquals(tester.state(), TimeoutMachine.States.CLOSED);
        assertTrue(callbacks.isEmpty());
    }

    /**
     * Test Case: WRITING_WRITE_COMPLETE
     */
    @Test
    public void test_WRITING_WRITE_COMPLETE ()
    {
        setup(1000, 1000, 1000);

        assertEquals(tester.state(), TimeoutMachine.States.INITIAL);
        tester.reactTo(TimeoutMachine.Events.INIT);
        assertEquals(tester.state(), TimeoutMachine.States.READING);
        tester.reactTo(TimeoutMachine.Events.READ_COMPLETE);
        assertEquals(tester.state(), TimeoutMachine.States.RESPONDING);
        tester.reactTo(TimeoutMachine.Events.RESPONSE_COMPLETE);
        assertEquals(tester.state(), TimeoutMachine.States.WRITING);
        tester.reactTo(TimeoutMachine.Events.WRITE_COMPLETE);
        assertEquals(tester.state(), TimeoutMachine.States.CLOSED);
        assertTrue(callbacks.isEmpty());
    }

    /**
     * Test Case: CLOSED_WRITE_COMPLETE
     */
    @Test
    public void test_CLOSED_WRITE_COMPLETE ()
    {
        setup(1000, 1000, 1000);

        assertEquals(tester.state(), TimeoutMachine.States.INITIAL);
        tester.reactTo(TimeoutMachine.Events.INIT);
        assertEquals(tester.state(), TimeoutMachine.States.READING);
        tester.reactTo(TimeoutMachine.Events.READ_COMPLETE);
        assertEquals(tester.state(), TimeoutMachine.States.RESPONDING);
        tester.reactTo(TimeoutMachine.Events.RESPONSE_COMPLETE);
        assertEquals(tester.state(), TimeoutMachine.States.WRITING);
        tester.reactTo(TimeoutMachine.Events.WRITE_COMPLETE);
        assertEquals(tester.state(), TimeoutMachine.States.CLOSED);
        tester.reactTo(TimeoutMachine.Events.WRITE_COMPLETE);
        assertEquals(tester.state(), TimeoutMachine.States.CLOSED);
        assertTrue(callbacks.isEmpty());
    }

}
