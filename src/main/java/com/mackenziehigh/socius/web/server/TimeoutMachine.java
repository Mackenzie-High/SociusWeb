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

import static com.mackenziehigh.socius.web.server.TimeoutMachine.Events.*;
import static com.mackenziehigh.socius.web.server.TimeoutMachine.States.*;
import io.netty.channel.EventLoopGroup;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 *
 */
abstract class TimeoutMachine
{
    public static enum Events
    {
        INIT,
        READ_COMPLETE,
        READ_TIMEOUT,
        RESPONSE_COMPLETE,
        RESPONSE_TIMEOUT,
        WRITE_COMPLETE,
        WRITE_TIMEOUT
    }

    public static enum States
    {
        INITIAL,
        READING,
        RESPONDING,
        WRITING,
        CLOSED
    }

    private final EventLoopGroup executor;

    private final Duration readTimeout;

    private final Duration responseTimeout;

    private final Duration writeTimeout;

    private States state = States.INITIAL;

    public TimeoutMachine (final EventLoopGroup executor,
                           final Duration readTimeout,
                           final Duration responseTimeout,
                           final Duration writeTimeout)
    {
        this.executor = executor;
        this.readTimeout = readTimeout;
        this.responseTimeout = responseTimeout;
        this.writeTimeout = writeTimeout;
    }

    public abstract void onReadTimeout ();

    public abstract void onResponseTimeout ();

    public abstract void onWriteTimeout ();

    public States state ()
    {
        return state;
    }

    public void reactTo (final Events event)
    {
        if (event == WRITE_COMPLETE)
        {
            state = CLOSED;
        }
        else if (state == INITIAL && event == INIT)
        {
            state = READING;
            executor.schedule(() -> reactTo(READ_TIMEOUT), readTimeout.toNanos(), TimeUnit.NANOSECONDS);
        }
        else if (state != INITIAL && state != READING && event == READ_TIMEOUT)
        {
            // Pass, because this is normal.
            // If (READ_COMPLETE) occurs before the timer fires,
            // then the timer will wake up in a non (READING) state.
        }
        else if (state != INITIAL && state != READING && state != RESPONDING && event == RESPONSE_TIMEOUT)
        {
            // Pass, because this is normal.
            // If (RESPONSE_COMPLETE) occurs before the timer fires,
            // then the timer will wake up in a non (RESPONDING) state.
        }
        else if (state != INITIAL && state != READING && state != RESPONDING && state != WRITING && event == WRITE_TIMEOUT)
        {
            // Pass, because this is normal.
            // If (WRITE_COMPLETE) occurs before the timer fires,
            // then the timer will wake up in a non (WRITING) state.
        }
        else if (state == READING && event == READ_COMPLETE)
        {
            state = RESPONDING;
            executor.schedule(() -> reactTo(RESPONSE_TIMEOUT), responseTimeout.toNanos(), TimeUnit.NANOSECONDS);
        }
        else if (state == READING && event == READ_TIMEOUT)
        {
            state = CLOSED;
            onReadTimeout();
        }
        else if (state == RESPONDING && event == RESPONSE_COMPLETE)
        {
            state = WRITING;
            executor.schedule(() -> reactTo(WRITE_TIMEOUT), writeTimeout.toNanos(), TimeUnit.NANOSECONDS);
        }
        else if (state == RESPONDING && event == RESPONSE_TIMEOUT)
        {
            state = WRITING;
            executor.schedule(() -> reactTo(WRITE_TIMEOUT), writeTimeout.toNanos(), TimeUnit.NANOSECONDS);
            onResponseTimeout();
        }
        else if (state == WRITING && event == WRITE_TIMEOUT)
        {
            state = CLOSED;
            onWriteTimeout();
        }
        else
        {
            throw new IllegalStateException(String.format("Illegal Reaction (state = %s, event = %s)", state, event));
        }
    }
}
