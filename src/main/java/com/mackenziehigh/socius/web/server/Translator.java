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

import com.google.common.net.MediaType;
import com.google.protobuf.ByteString;
import com.mackenziehigh.socius.web.messages.web_m;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_0;
import io.netty.handler.codec.http.QueryStringDecoder;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Provides methods for translating between Netty-related objects and GPBs.
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
        this.serverName = Objects.requireNonNull(serverName, "serverName");
        this.serverId = Objects.requireNonNull(serverId, "serverId");
        this.replyTo = Objects.requireNonNull(replyTo, "replyTo");
    }

    /**
     * Getter.
     *
     * @return the current value of the sequence-counter.
     */
    public long sequenceCount ()
    {
        return sequenceCount.get();
    }

    /**
     * Translate a Netty-based request to a GPB-based request,
     * for use in the pre-check handlers, with certain parts
     * of the request are omitted, such as the entity.
     *
     * @param clientAddress is the remote-address of the client.
     * @param serverAddress is the local-address of the server.
     * @param request is a Netty-based request.
     * @return the partial GPB-based request.
     */
    public web_m.ServerSideHttpRequest prefixOf (final InetSocketAddress clientAddress,
                                                 final InetSocketAddress serverAddress,
                                                 final HttpRequest request)
    {
        Objects.requireNonNull(request, "request");
        return commonPrefix(clientAddress, serverAddress, request).build();
    }

    /**
     * Translate a Netty-based request to a GPB-based request.
     *
     * @param clientAddress is the remote-address of the client.
     * @param serverAddress is the local-address of the server.
     * @param request is a Netty-based request.
     * @return the complete GPB-based request.
     */
    public web_m.ServerSideHttpRequest requestToGPB (final InetSocketAddress clientAddress,
                                                     final InetSocketAddress serverAddress,
                                                     final FullHttpRequest request)
    {
        Objects.requireNonNull(request, "request");

        final web_m.ServerSideHttpRequest.Builder builder = commonPrefix(clientAddress, serverAddress, request);

        /**
         * Sequence Number.
         */
        final long seqnum = sequenceCount.incrementAndGet();
        builder.setSequenceNumber(seqnum);

        /**
         * Correlation-ID.
         */
        final String correlationId = UUID.randomUUID().toString();
        builder.setCorrelationId(correlationId);

        /**
         * Reply-To.
         */
        builder.setReplyTo(replyTo);

        /**
         * Entity.
         */
        if (request.content().isReadable())
        {
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

        /**
         * Trailing Headers.
         */
        final List<String> trailerNames = request.trailingHeaders().names().stream().map(x -> x.toLowerCase()).collect(Collectors.toList());
        for (String name : trailerNames)
        {
            builder.putTrailers(name,
                                web_m.HttpHeader.newBuilder()
                                        .setKey(name)
                                        .addAllValues(request.trailingHeaders().getAll(name))
                                        .build());
        }

        return builder.build();
    }

    private web_m.ServerSideHttpRequest.Builder commonPrefix (final InetSocketAddress clientAddress,
                                                              final InetSocketAddress serverAddress,
                                                              final HttpRequest request)
    {
        final web_m.ServerSideHttpRequest.Builder builder = web_m.ServerSideHttpRequest.newBuilder();

        /**
         * Server Name.
         */
        builder.setServerName(serverName);

        /**
         * Server-ID.
         */
        builder.setServerId(serverId);

        /**
         * Timestamp.
         */
        builder.setTimestamp(System.currentTimeMillis());

        /**
         * Remote Address.
         */
        final web_m.RemoteAddress remoteAddress = web_m.RemoteAddress.newBuilder()
                .setHost(clientAddress.getHostString())
                .setPort(clientAddress.getPort())
                .build();

        builder.setRemoteAddress(remoteAddress);

        /**
         * Remote Address.
         */
        final web_m.LocalAddress localAddress = web_m.LocalAddress.newBuilder()
                .setHost(serverAddress.getHostString())
                .setPort(serverAddress.getPort())
                .build();

        builder.setLocalAddress(localAddress);

        /**
         * HTTP Protocol.
         */
        builder.setProtocol(web_m.HttpProtocol.newBuilder()
                .setText(request.protocolVersion().text())
                .setName(request.protocolVersion().protocolName())
                .setMajorVersion(request.protocolVersion().majorVersion())
                .setMinorVersion(request.protocolVersion().minorVersion()));

        /**
         * HTTP Method.
         */
        builder.setMethod(request.method().name().toUpperCase());

        /**
         * URI, etc.
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
         * Header: Host.
         */
        if (request.headers().contains(HttpHeaders.Names.HOST))
        {
            builder.setHost(request.headers().get(HttpHeaders.Names.HOST));
        }

        /**
         * Header: Content-Type.
         */
        if (request.headers().contains(HttpHeaders.Names.CONTENT_TYPE))
        {
            builder.setContentType(request.headers().get(HttpHeaders.Names.CONTENT_TYPE));
        }

        /**
         * Header: Content-Length.
         */
        if (request.headers().contains(HttpHeaders.Names.CONTENT_LENGTH))
        {
            final int length = Integer.parseInt(request.headers().get(HttpHeaders.Names.CONTENT_LENGTH));
            builder.setContentLength(length);
        }

        /**
         * Other HTTP Headers.
         */
        final List<String> headerNames = request.headers().names().stream().map(x -> x.toLowerCase()).collect(Collectors.toList());
        for (String name : headerNames)
        {
            builder.putHeaders(name,
                               web_m.HttpHeader.newBuilder()
                                       .setKey(name)
                                       .addAllValues(request.headers().getAll(name))
                                       .build());
        }

        return builder;
    }

    /**
     * Translate a GPB-based response to a Netty-based response.
     *
     * @param response is the GPB-based response to translate.
     * @return the response translated to a Netty-based equivalent.
     */
    public FullHttpResponse responseFromGPB (final web_m.ServerSideHttpResponse response)
    {
        //Objects.requireNonNull(request, "request");

        /**
         * HTTP Entity.
         */
        final ByteBuf body = Unpooled.copiedBuffer(response.getBody().asReadOnlyByteBuffer());

        /**
         * HTTP Status Code.
         */
        final HttpResponseStatus status = response.hasStatus()
                ? HttpResponseStatus.valueOf(response.getStatus())
                : HttpResponseStatus.INTERNAL_SERVER_ERROR;

        /**
         * Create the response object.
         */
        final FullHttpResponse netty = new DefaultFullHttpResponse(HTTP_1_0, status, body);

        /**
         * HTTP Headers.
         */
        for (web_m.HttpHeader header : response.getHeadersList())
        {
            if (header.hasKey() == false)
            {
                throw new IllegalArgumentException("Unnamed Header in Response");
            }

            /**
             * Normalize header names to lower-case.
             */
            final String key = header.getKey().toLowerCase();

            /**
             * If a header is specified with a name, but no value,
             * then assume that the value is an empty string.
             */
            if (header.getValuesList().isEmpty())
            {
                netty.headers().add(key, "");
            }

            /**
             * If a header is specified with one or more values,
             * then add each key-value pair to the response.
             */
            for (String value : header.getValuesList())
            {
                netty.headers().add(key, value);
            }
        }

        /**
         * Always include an HTTP 1.1 compatible header to close the connection.
         * Since the response is HTTP 1.0, this is strictly optional.
         */
        netty.headers().set("connection", "close");

        /**
         * Header: Content-Type.response
         */
        if (response.hasContentType())
        {
            netty.headers().set(HttpHeaders.Names.CONTENT_TYPE, response.getContentType());
        }
        else if (netty.headers().contains(HttpHeaders.Names.CONTENT_TYPE))
        {
            // Pass, because the Content-Type is already set.
        }
        else
        {
            netty.headers().set(HttpHeaders.Names.CONTENT_TYPE, MediaType.OCTET_STREAM.toString());
        }

        /**
         * Header: ConteresponseLength.
         */
        if (response.hasBody())
        {
            netty.headers().set(HttpHeaders.Names.CONTENT_LENGTH, response.getBody().size());
        }
        else
        {
            netty.headers().set(HttpHeaders.Names.CONTENT_LENGTH, 0);
        }

        return netty;
    }

    /**
     * Static utility method for creating common error-responses.
     *
     * <p>
     * The response will redirect the client to an HTML page
     * under the web-root, whose name is the numeric error-code.
     * </p>
     *
     * @param status is the HTTP error-code.
     * @return the Netty-based response object.
     */
    public static FullHttpResponse newErrorResponse (final HttpResponseStatus status)
    {
        final String message = "<head> <meta http-equiv=\"refresh\" content=\"5; URL=\"/" + status.code() + ".html\" /> </head>\r\n";
        final ByteBuf content = Unpooled.copiedBuffer(message.getBytes(StandardCharsets.US_ASCII));
        final FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_0, status, content);
        response.headers().set("connection", "close");
        response.headers().set("content-type", "text/html");
        response.headers().set("content-length", message.length());
        return response;
    }

    /**
     * Static utility method for creating common error-responses.
     *
     * <p>
     * The response will redirect the client to an HTML page
     * under the web-root, whose name is the numeric error-code.
     * </p>
     *
     * @param status is the HTTP error-code.
     * @return the Netty-based response object.
     */
    public static web_m.ServerSideHttpResponse newErrorResponseGPB (final int status)
    {
        final String message = "<head> <meta http-equiv=\"refresh\" content=\"5; URL=\"/" + status + ".html\" /> </head>\r\n";

        final web_m.ServerSideHttpResponse response = web_m.ServerSideHttpResponse.newBuilder()
                .addHeaders(web_m.HttpHeader.newBuilder().setKey("connection").addValues("close"))
                .setContentType("text/html")
                .setTimestamp(System.currentTimeMillis())
                .setStatus(status)
                .setBody(ByteString.copyFromUtf8(message))
                .build();

        return response;
    }
}
