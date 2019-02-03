package com.mackenziehigh.socius.web;

import com.mackenziehigh.socius.web.ConnectorWS.WebSocket;
import com.mackenziehigh.socius.web.web_m.HttpRequest;
import com.mackenziehigh.socius.web.web_m.HttpResponse;
import com.mackenziehigh.socius.web.web_m.WebSocketResponse;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import java.net.URISyntaxException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * An instance of this class will be used to translate
 * an incoming HTTP Request into a Protocol Buffer
 * and then send the Protocol Buffer to external handlers.
 *
 * <p>
 * A new instance of this class will be created per connection!
 * </p>
 */
final class ServerHandler
        extends SimpleChannelInboundHandler<Object>
{
    private final SharedState shared;

    private final UUID connectionId = UUID.randomUUID();

    private final AtomicLong httpCount = new AtomicLong();

    private volatile WebSocket websocket;

    public ServerHandler (final SharedState shared)
    {
        this.shared = shared;
    }

    @Override
    public void channelReadComplete (final ChannelHandlerContext ctx)
            throws Exception
    {
        ctx.flush();
    }

    @Override
    protected void channelRead0 (final ChannelHandlerContext ctx,
                                 final Object msg)
            throws URISyntaxException
    {
        if (msg instanceof FullHttpRequest)
        {
            onRecvFullMessage(ctx, (FullHttpRequest) msg);
        }
        else if (msg instanceof WebSocketFrame)
        {
            onRecvSocketMessage((WebSocketFrame) msg);
        }
    }

    private void onRecvFullMessage (final ChannelHandlerContext ctx,
                                    final FullHttpRequest request)
            throws URISyntaxException
    {
        final HttpHeaders headers = request.headers();
        final boolean upgrade = "Upgrade".equalsIgnoreCase(headers.get(HttpHeaderNames.CONNECTION));
        final boolean socket = "WebSocket".equalsIgnoreCase(headers.get(HttpHeaderNames.UPGRADE));
        final boolean upgradeToWebSocket = upgrade && socket;

        if (httpCount.incrementAndGet() != 1)
        {
            // Ignore, pipelined HTTP messages, except the first.
        }
        else if (upgradeToWebSocket)
        {
            final WebSocketOutputConnection output = new WebSocketOutputConnection(connectionId, ctx);
            final HttpRequest httpRequest = Translator.requestToGPB(shared, connectionId, request);
            websocket = shared.socks.open(httpRequest, output);
            handleHandshake(ctx, request);
        }
        else
        {
            final HttpOutputConnection output = new HttpOutputConnection(connectionId, ctx);
            final HttpRequest httpRequest = Translator.requestToGPB(shared, connectionId, request);
            shared.http.open(httpRequest, output);
        }
    }

    private void onRecvSocketMessage (final WebSocketFrame frame)
    {
        if (frame instanceof BinaryWebSocketFrame)
        {
            websocket.onBlobFromClient((BinaryWebSocketFrame) frame);
        }
        else if (frame instanceof TextWebSocketFrame)
        {
            websocket.onTextFromClient((TextWebSocketFrame) frame);
        }
        else if (frame instanceof PingWebSocketFrame)
        {
            websocket.onPing((PingWebSocketFrame) frame);
        }
        else if (frame instanceof PongWebSocketFrame)
        {
            websocket.onPong((PongWebSocketFrame) frame);
        }
        else if (frame instanceof CloseWebSocketFrame)
        {
            websocket.onCloseFromClient((CloseWebSocketFrame) frame);
        }
    }

    @Override
    public void exceptionCaught (final ChannelHandlerContext ctx,
                                 final Throwable cause)
            throws Exception
    {
        /**
         * Log the exception.
         */
//        logger.warn(cause);

        /**
         * Notify the client of the error, but do not tell them exactly why (for security).
         */
        final FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, INTERNAL_SERVER_ERROR);

        /**
         * Send the response to the client.
         */
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    /* Do the handshaking for WebSocket request */
    private void handleHandshake (final ChannelHandlerContext ctx,
                                  final FullHttpRequest req)
    {
        // TODO: Args
        final WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(getWebSocketURL(req), null, true);
        final WebSocketServerHandshaker handshaker = wsFactory.newHandshaker(req);

        if (handshaker == null)
        {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        }
        else
        {
            handshaker.handshake(ctx.channel(), req, req.headers(), null);
        }
    }

    private String getWebSocketURL (final FullHttpRequest req)
    {
        System.out.println("Req URI : " + req.getUri());
        String url = "ws://" + req.headers().get("Host") + req.getUri();
        System.out.println("Constructed URL : " + url);
        return url;
    }

    private static final class HttpOutputConnection
            implements OutputConnection<HttpResponse>
    {
        private final UUID connectionId;

        private final ChannelHandlerContext context;

        public HttpOutputConnection (final UUID connectionId,
                                     final ChannelHandlerContext context)
        {
            this.connectionId = connectionId;
            this.context = context;
        }

        @Override
        public UUID connectionId ()
        {
            return connectionId;
        }

        @Override
        public void write (final HttpResponse message)
        {
            final FullHttpResponse translated = Translator.responseFromGPB(message);
            context.writeAndFlush(translated);
        }

        @Override
        public void close ()
        {
            context.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }

    private static final class WebSocketOutputConnection
            implements OutputConnection<WebSocketResponse>
    {
        private final UUID connectionId;

        private final ChannelHandlerContext context;

        public WebSocketOutputConnection (final UUID connectionId,
                                          final ChannelHandlerContext context)
        {
            this.connectionId = connectionId;
            this.context = context;
        }

        @Override
        public UUID connectionId ()
        {
            return connectionId;
        }

        @Override
        public void write (final WebSocketResponse message)
        {
            if (message.getData().hasBlob())
            {
                final ByteBuf blob = Unpooled.copiedBuffer(message.getData().getBlob().toByteArray());
                final BinaryWebSocketFrame frame = new BinaryWebSocketFrame(blob);
                context.writeAndFlush(frame);
            }
            else if (message.getData().hasText())
            {
                final TextWebSocketFrame frame = new TextWebSocketFrame(message.getData().getText());
                context.writeAndFlush(frame);
            }
        }

        @Override
        public void close ()
        {
            context.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }
}
