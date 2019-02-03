package com.mackenziehigh.socius.web;

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

    public final ConnectorHTTP http;

    public final ConnectorWS socks;

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
        this.http = new ConnectorHTTP(this);
        this.socks = new ConnectorWS(this);
    }

}
