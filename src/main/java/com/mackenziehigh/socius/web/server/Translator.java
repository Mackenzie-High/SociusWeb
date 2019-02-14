package com.mackenziehigh.socius.web.server;

import com.google.common.net.MediaType;
import com.google.protobuf.ByteString;
import com.mackenziehigh.socius.web.messages.web_m;
import com.mackenziehigh.socius.web.messages.web_m.HttpPrefix;
import com.mackenziehigh.socius.web.messages.web_m.HttpRequest;
import com.mackenziehigh.socius.web.messages.web_m.HttpResponse;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Provides static utility methods for translating between Netty-related objects and GPBs.
 */
final class Translator
{
    private final AtomicLong sequenceCount = new AtomicLong();

    private final String serverName;

    private final String serverId;

    private final String replyTo;

    public Translator (final String serverName,
                       final String serverId,
                       final String replyTo)
    {
        this.serverName = serverName;
        this.serverId = serverId;
        this.replyTo = replyTo;
    }

    public HttpPrefix prefixOf (io.netty.handler.codec.http.HttpRequest request)
    {
        final HttpPrefix.Builder builder = HttpPrefix.newBuilder();
        final HttpRequest.Builder partial = encodePrefix(request);

        if (partial.hasProtocol())
        {
            builder.setProtocol(builder.getProtocol());
        }

        if (partial.hasMethod())
        {
            builder.setMethod(partial.getMethod());
        }

        if (partial.hasUri())
        {
            builder.setUri(partial.getUri());
        }

        if (partial.hasPath())
        {
            builder.setPath(partial.getPath());
        }

        if (partial.hasRawPath())
        {
            builder.setRawPath(partial.getRawPath());
        }

        if (partial.hasRawQuery())
        {
            builder.setRawQuery(partial.getRawQuery());
        }

        builder.putAllParameters(partial.getParametersMap());

        builder.putAllHeaders(partial.getHeadersMap());

        if (partial.hasHost())
        {
            builder.setHost(partial.getHost());
        }

        builder.addAllCookies(partial.getCookiesList());

        if (partial.hasContentType())
        {
            builder.setContentType(partial.getContentType());
        }

        if (partial.hasContentLength())
        {
            builder.setContentLength(partial.getContentLength());
        }

        return builder.build();
    }

    public HttpRequest requestToGPB (final FullHttpRequest request)
    {
        final long seqnum = sequenceCount.incrementAndGet();

        final String correlationId = UUID.randomUUID().toString();

        final web_m.HttpRequest.Builder builder = encodePrefix(request);

        builder.setServerName(serverName);
        builder.setServerId(serverId);
        builder.setSequenceNumber(seqnum);
        builder.setTimestamp(System.currentTimeMillis());
        builder.setCorrelationId(correlationId);
        builder.setReplyTo(replyTo);

        /**
         * Encode the body.
         */
        if (request.content().isReadable())
        {
            // TODO: Optimize? Use shared temp buffer?
            final byte[] bytes = new byte[request.content().readableBytes()];
            request.content().readBytes(bytes);
            final ByteString byteString = ByteString.copyFrom(bytes);
            builder.setContentLength(byteString.size());
            builder.setBody(byteString);
        }
        else
        {
            builder.setContentLength(0);
            builder.setBody(ByteString.EMPTY);
        }

        return builder.build();
    }

    private HttpRequest.Builder encodePrefix (final io.netty.handler.codec.http.HttpRequest request)
    {
        final web_m.HttpRequest.Builder builder = web_m.HttpRequest.newBuilder();

        builder.setProtocol(web_m.HttpProtocol.newBuilder()
                .setText(request.protocolVersion().text())
                .setName(request.protocolVersion().protocolName())
                .setMajorVersion(request.protocolVersion().majorVersion())
                .setMinorVersion(request.protocolVersion().minorVersion()));

        builder.setMethod(request.method().name());

        /**
         * Encode the URL.
         */
        final QueryStringDecoder qsDecoder = new QueryStringDecoder(request.uri());
        builder.setUri(qsDecoder.uri());
        builder.setPath(qsDecoder.path());
        builder.setRawPath(qsDecoder.rawPath());
        builder.setRawQuery(qsDecoder.rawQuery());

        /**
         * Encode Query Parameters.
         */
        for (Map.Entry<String, List<String>> params : qsDecoder.parameters().entrySet())
        {
            final web_m.HttpQueryParameter.Builder param = web_m.HttpQueryParameter.newBuilder();
            param.setKey(params.getKey());
            param.addAllValues(params.getValue());

            builder.putParameters(param.getKey(), param.build());
        }

        /**
         * Encode Host.
         */
        if (request.headers().contains(HttpHeaders.Names.HOST))
        {
            builder.setHost(request.headers().get(HttpHeaders.Names.HOST));
        }

        /**
         * Encode Content Type.
         */
        if (request.headers().contains(HttpHeaders.Names.CONTENT_TYPE))
        {
            builder.setContentType(request.headers().get(HttpHeaders.Names.CONTENT_TYPE));
        }

        /**
         * Encode the headers.
         */
        for (String name : request.headers().names())
        {
            builder.putHeaders(name,
                               web_m.HttpHeader.newBuilder()
                                       .setKey(name)
                                       .addAllValues(request.headers().getAll(name))
                                       .build());
        }

        /**
         * Encode the cookies.
         */
        for (String header : request.headers().getAll(HttpHeaders.Names.COOKIE))
        {
            for (Cookie cookie : ServerCookieDecoder.STRICT.decode(header))
            {
                final web_m.HttpCookie.Builder cookieBuilder = web_m.HttpCookie.newBuilder();

                cookieBuilder.setDomain(cookie.domain());
                cookieBuilder.setHttpOnly(cookie.isHttpOnly());
                cookieBuilder.setSecure(cookie.isSecure());
                cookieBuilder.setPath(cookie.path());
                cookieBuilder.setMaxAge(cookie.maxAge());

                builder.addCookies(cookieBuilder);
            }
        }

        return builder;
    }

    public FullHttpResponse responseFromGPB (final HttpResponse encodedResponse)
    {
        /**
         * Decode the body.
         */
        final ByteBuf body = Unpooled.copiedBuffer(encodedResponse.getBody().asReadOnlyByteBuffer());

        /**
         * Decode the HTTP status code.
         */
        final HttpResponseStatus status = encodedResponse.hasStatus()
                ? HttpResponseStatus.valueOf(encodedResponse.getStatus())
                : HttpResponseStatus.OK;

        /**
         * Create the response.
         */
        final FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, status, body);

        /**
         * Decode the headers.
         */
        for (Map.Entry<String, web_m.HttpHeader> header : encodedResponse.getHeadersMap().entrySet())
        {
            response.headers().add(header.getKey(), header.getValue().getValuesList());
        }

        /**
         * Always close the connection.
         */
        response.headers().add("connection", "close");

        /**
         * Decode the content-type.
         */
        if (encodedResponse.hasContentType())
        {
            response.headers().add(HttpHeaders.Names.CONTENT_TYPE, encodedResponse.getContentType());
        }
        else
        {
            response.headers().add(HttpHeaders.Names.CONTENT_TYPE, MediaType.OCTET_STREAM.toString());
        }

        /**
         * Decode the content-length.
         */
        response.headers().add(HttpHeaders.Names.CONTENT_LENGTH, encodedResponse.getBody().size());

        return response;
    }
}