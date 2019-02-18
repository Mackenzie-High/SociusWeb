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

import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;

final class SharedState
{
    public final ScheduledExecutorService service;

    public final Stage stage;

    /**
     * This is the human-readable name of this server to embed in requests.
     */
    public final String serverName;

    /**
     * This is the UUID of this server lifetime to embed in requests.
     */
    public final String serverId = UUID.randomUUID().toString();

    /**
     * This user-defined value will be embedded in requests.
     */
    public final String replyTo;

    /**
     * This counter is used to assign sequence-numbers to requests.
     */
    public final AtomicLong sequenceNumber = new AtomicLong();

    /**
     * This is the host that the server will listen on.
     */
    public final String host;

    /**
     * This is the port that the server will listen on.
     */
    public final int port;

    /**
     * Connections will be closed, if a response is not received within this timeout.
     */
    public final Duration responseTimeout;

    /**
     * HTTP Chunk Aggregation Limit.
     */
    public final int aggregationCapacity;

    public final int maxInitialLineLength = 4096;

    public final int maxHeaderSize = 256 * 256;

    public final int maxChunkSize = 256 * 256;

    public final boolean validateHeaders = true;

    public SharedState (final ScheduledExecutorService service,
                        final String serverName,
                        final String replyTo,
                        final String host,
                        final int port,
                        final Duration responseTimeout,
                        final int aggregationCapacity)
    {
        this.service = service;
        this.stage = Cascade.newExecutorStage(service);
        this.serverName = serverName;
        this.replyTo = replyTo;
        this.host = host;
        this.port = port;
        this.responseTimeout = responseTimeout;
        this.aggregationCapacity = aggregationCapacity;
    }

}
