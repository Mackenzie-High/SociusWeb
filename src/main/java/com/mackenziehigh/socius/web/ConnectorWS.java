package com.mackenziehigh.socius.web;

import com.google.common.base.Verify;
import com.google.common.collect.Maps;
import com.google.protobuf.ByteString;
import com.mackenziehigh.cascade.Cascade.Stage.Actor;
import com.mackenziehigh.socius.web.web_m.HttpRequest;
import com.mackenziehigh.socius.web.web_m.WebSocketRequest;
import com.mackenziehigh.socius.web.web_m.WebSocketResponse;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.UUID;

final class ConnectorWS
{
    private final SharedState shared;

    /**
     * This map maps correlation UUIDs of requests to consumer functions
     * that will be used to process the corresponding responses.
     *
     * <p>
     * Entries are added to this map whenever an HTTP request is processed.
     * Entries are lazily removed from this map, if they have been there too long.
     * </p>
     */
    private final Map<String, WebSocket> connections = Maps.newConcurrentMap();

    /**
     * This processor will be used to send HTTP requests out of the server,
     * so that external handler actors can process the requests.
     */
    public final Actor<WebSocketRequest, WebSocketRequest> requestsOut;

    /**
     * This processor will receive the HTTP responses from the external actors
     * and then will route those responses to the originating connection.
     */
    public final Actor<WebSocketResponse, WebSocketResponse> responsesIn;

    public ConnectorWS (final SharedState shared)
    {
        this.shared = shared;
        this.requestsOut = shared.stage.newActor().withScript(this::onRequest).create();
        this.responsesIn = shared.stage.newActor().withScript(this::onResponse).create();
    }

    public WebSocket open (final HttpRequest request,
                           final OutputConnection<WebSocketResponse> output)
            throws URISyntaxException
    {

        final String correlationId = request.getCorrelationId();

        /**
         * Create the response handler that will be used to route
         * the corresponding HTTP Response, if and when it occurs.
         */
        Verify.verify(connections.containsKey(correlationId) == false);
        final WebSocket connection = new WebSocket(output, request);
        connections.put(correlationId, connection);

        return connection;
    }

    private WebSocketRequest onRequest (final WebSocketRequest message)
    {
        return message;
    }

    private void onResponse (final WebSocketResponse message)
    {
        /**
         * The message is supposed to contain the Correlation-ID of the connection.
         * Use the Correlation-ID in order to lookup the corresponding collection.
         * If no Correlation-ID is present, then simply ignore the message,
         * because there is nothing that we can really do in this situation.
         * If no connection is present with the given Correlation-ID,
         * then ignore the message, because the connection has already closed.
         */
        final WebSocket connection = message.hasCorrelationId() ? connections.get(message.getCorrelationId()) : null;

        if (connection == null)
        {
            return;
        }

        /**
         * If the message contains binary data to send to the client,
         * then send that data to the client immediately.
         */
        if (message.hasData() && message.getData().hasBlob())
        {
            connection.onBlobFromServer(message);
        }

        /**
         * If the message contains textual data to send to the client,
         * then send that data to the client immediately.
         */
        if (message.hasData() && message.getData().hasText())
        {
            connection.onTextFromServer(message);
        }

        /**
         * If the server-side wants the web-socket to be closed now,
         * then go ahead and close the web-socket immediately.
         */
        if (message.hasClose())
        {
            connection.onCloseFromServer();
        }
    }

    public final class WebSocket
    {
        public final UUID correlationId = UUID.randomUUID();

        public final String correlationIdAsString = correlationId.toString();

        private final OutputConnection<WebSocketResponse> context;

        private final HttpRequest httpRequest;

        public WebSocket (final OutputConnection context,
                          final HttpRequest request)
                throws URISyntaxException
        {
            this.context = context;
            this.httpRequest = request;
        }

        public void open ()
        {
            connections.put(correlationIdAsString, this);
        }

        public void onTextFromServer (final WebSocketResponse message)
        {
            context.write(message);
        }

        public void onBlobFromServer (final WebSocketResponse message)
        {
            context.write(message);
        }

        public void onTextFromClient (final TextWebSocketFrame message)
        {
            final web_m.WebSocketRequest.Builder builder = web_m.WebSocketRequest.newBuilder();
            builder.setCorrelationId(correlationIdAsString);
            builder.setSequenceNumber(shared.sequenceNumber.incrementAndGet());
            builder.setServerId(shared.serverId);
            builder.setServerName(shared.serverName);
            builder.setReplyTo(shared.replyTo);
            builder.setTimestamp(System.currentTimeMillis());
            builder.setRequest(httpRequest);
            builder.setData(web_m.DataFrame.newBuilder().setText(message.text()));
            requestsOut.accept(builder.build());
        }

        public void onBlobFromClient (final BinaryWebSocketFrame message)
        {
            final web_m.WebSocketRequest.Builder builder = web_m.WebSocketRequest.newBuilder();
            builder.setCorrelationId(correlationIdAsString);
            builder.setSequenceNumber(shared.sequenceNumber.incrementAndGet());
            builder.setServerId(shared.serverId);
            builder.setServerName(shared.serverName);
            builder.setReplyTo(shared.replyTo);
            builder.setTimestamp(System.currentTimeMillis());
            builder.setRequest(httpRequest);
            final ByteString data = ByteString.copyFrom(message.content().nioBuffer());
            builder.setData(web_m.DataFrame.newBuilder().setBlob(data));
            requestsOut.accept(builder.build());
        }

        public void onPing (final PingWebSocketFrame message)
        {
        }

        public void onPong (final PongWebSocketFrame message)
        {
        }

        public void onCloseFromClient (final CloseWebSocketFrame message)
        {
            // reason and status code
            connections.remove(correlationId);
        }

        public void onCloseFromServer ()
        {
            connections.remove(correlationId);
        }
    }

}
