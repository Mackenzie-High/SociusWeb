package com.mackenziehigh.socius.web;

import com.google.protobuf.ByteString;
import com.mackenziehigh.socius.web.web_m.HttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class HttpRequestEncoder
{

    public static HttpRequest encode (final FullHttpRequest request,
                                      final String serverName,
                                      final String serverId,
                                      final String replyTo,
                                      final long seqnum)
            throws URISyntaxException
    {
        final String correlationId = UUID.randomUUID().toString();

        final web_m.HttpRequest.Builder builder = web_m.HttpRequest.newBuilder();

        builder.setServerName(serverName);
        builder.setServerId(serverId);
        builder.setSequenceNumber(seqnum);
        builder.setTimestamp(System.currentTimeMillis());
        builder.setCorrelationId(correlationId);
        builder.setReplyTo(replyTo);
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
}
