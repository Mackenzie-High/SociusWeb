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

import com.google.common.base.Strings;
import com.google.protobuf.ByteString;
import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor;
import com.mackenziehigh.socius.web.messages.web_m.ServerSideHttpRequest;
import com.mackenziehigh.socius.web.messages.web_m.ServerSideHttpResponse;
import com.mackenziehigh.socius.web.server.loggers.DefaultWebLogger;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.time.Duration;

/**
 * An instance of this class is a web-application that is
 * tailored for use in the web-server integration-tests.
 */
public final class TestServer
{
    private final Stage stage = Cascade.newStage(4);

    private final WebServer server;

    public TestServer (final WebServer server)
    {
        this.server = server;
        this.stage.addErrorHandler(ex -> System.out.println(ex));
    }

    private ServerSideHttpResponse slashEcho (final ServerSideHttpRequest request)
    {
        if (request.getPath().equals("/echo") == false)
        {
            return null;
        }

        final byte[] bytes = request.toString().getBytes();

        final ServerSideHttpResponse response = ServerSideHttpResponse
                .newBuilder()
                .setRequest(request)
                .setContentType("text/plain")
                .setStatus(200)
                .setBody(ByteString.copyFrom(bytes))
                .build();

        return response;
    }

    private ServerSideHttpResponse slashZero (final ServerSideHttpRequest request)
    {
        if (request.getPath().equals("/zero") == false)
        {
            return null;
        }

        final int count = Integer.parseInt(request.getParametersMap().get("count").getValues(0));
        final byte[] bytes = Strings.repeat("0", count).getBytes(StandardCharsets.US_ASCII);

        final ServerSideHttpResponse response = ServerSideHttpResponse
                .newBuilder()
                .setRequest(request)
                .setContentType("text/plain")
                .setStatus(200)
                .setBody(ByteString.copyFrom(bytes))
                .build();

        return response;
    }

    private ServerSideHttpResponse slashSleep (final ServerSideHttpRequest request)
            throws InterruptedException
    {
        if (request.getPath().equals("/sleep") == false)
        {
            return null;
        }

        final long millis = Integer.parseInt(request.getParametersMap().get("period").getValues(0));
        Thread.sleep(millis);

        final ServerSideHttpResponse response = ServerSideHttpResponse
                .newBuilder()
                .setRequest(request)
                .setContentType("text/plain")
                .setStatus(200)
                .setBody(ByteString.copyFromUtf8("SleepComplete\n"))
                .build();

        return response;
    }

    private ServerSideHttpResponse slashAdd (final ServerSideHttpRequest request)
    {
        if (request.getPath().equals("/add") == false)
        {
            return null;
        }

        final long X = Integer.parseInt(request.getParametersMap().get("x").getValues(0));
        final long Y = Integer.parseInt(request.getParametersMap().get("y").getValues(0));
        final long Z = X + Y;

        final ServerSideHttpResponse response = ServerSideHttpResponse
                .newBuilder()
                .setRequest(request)
                .setContentType("text/plain")
                .setStatus(200)
                .setBody(ByteString.copyFromUtf8(Long.toString(Z)))
                .build();

        return response;
    }

    public void start ()
            throws InterruptedException
    {
        /**
         * Create the actors that are web-service end-points.
         */
        final Actor<ServerSideHttpRequest, ServerSideHttpResponse> echo = stage.newActor().withScript(this::slashEcho).create();
        final Actor<ServerSideHttpRequest, ServerSideHttpResponse> sleep = stage.newActor().withScript(this::slashSleep).create();
        final Actor<ServerSideHttpRequest, ServerSideHttpResponse> add = stage.newActor().withScript(this::slashAdd).create();
        final Actor<ServerSideHttpRequest, ServerSideHttpResponse> zero = stage.newActor().withScript(this::slashZero).create();

        /**
         * Connect the web-service end-points to the server.
         */
        server.requestsOut().connect(echo.input());
        server.responsesIn().connect(echo.output());
        //
        server.requestsOut().connect(sleep.input());
        server.responsesIn().connect(sleep.output());
        //
        server.requestsOut().connect(add.input());
        server.responsesIn().connect(add.output());
        //
        server.requestsOut().connect(zero.input());
        server.responsesIn().connect(zero.output());

        /**
         * Bind the server to the server socket and begin serving.
         */
        server.start();
        Thread.sleep(2_000);
    }

    public static void main (String[] args)
            throws IOException,
                   InterruptedException,
                   KeyStoreException,
                   NoSuchAlgorithmException,
                   CertificateException,
                   KeyManagementException,
                   UnrecoverableKeyException
    {
        final WebServer server = WebServer
                .newWebServer()
                .withDefaultSettings()
                .withSoftConnectionLimit(100_000)
                .withHardConnectionLimit(100_000)
                .withAcceptFilter()
                .withResponseTimeout(Duration.ofSeconds(5))
                .withUplinkTimeout(Duration.ofSeconds(1))
                .withLogger(new DefaultWebLogger())
                .withUnsecureSockets()
                .build();

        final TestServer s = new TestServer(server);
        s.start();

    }
}
