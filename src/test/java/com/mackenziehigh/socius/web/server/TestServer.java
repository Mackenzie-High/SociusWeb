package com.mackenziehigh.socius.web.server;

import com.google.common.base.Strings;
import com.google.protobuf.ByteString;
import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor;
import com.mackenziehigh.socius.flow.RoundRobin;
import com.mackenziehigh.socius.web.messages.web_m.ServerSideHttpRequest;
import com.mackenziehigh.socius.web.messages.web_m.ServerSideHttpResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
        final Actor<ServerSideHttpRequest, ServerSideHttpResponse> zero0 = stage.newActor().withScript(this::slashZero).create();
        final Actor<ServerSideHttpRequest, ServerSideHttpResponse> zero1 = stage.newActor().withScript(this::slashZero).create();
        final Actor<ServerSideHttpRequest, ServerSideHttpResponse> zero2 = stage.newActor().withScript(this::slashZero).create();
        final Actor<ServerSideHttpRequest, ServerSideHttpResponse> zero3 = stage.newActor().withScript(this::slashZero).create();

        final RoundRobin<ServerSideHttpRequest> rr = RoundRobin.newRoundRobin(stage, 4);
        rr.dataOut(0).connect(zero0.input());
        rr.dataOut(1).connect(zero1.input());
        rr.dataOut(2).connect(zero2.input());
        rr.dataOut(3).connect(zero3.input());

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
        //server.requestsOut().connect(zero.input());
        //server.responsesIn().connect(zero.output());
        server.requestsOut().connect(rr.dataIn());
        server.responsesIn().connect(zero0.output());
        server.responsesIn().connect(zero1.output());
        server.responsesIn().connect(zero2.output());
        server.responsesIn().connect(zero3.output());

        /**
         * Bind the server to the server socket and begin serving.
         */
        server.start();
        Thread.sleep(2_000);
    }

    public static void main (String[] args)
            throws IOException,
                   InterruptedException
    {
        final WebServer server = WebServer
                .newWebServer()
                .withDefaultSettings()
                .withPrecheckAccept()
                .withConnectionTimeout(Duration.ofSeconds(60))
                .withResponseTimeout(Duration.ofSeconds(30))
                .withSlowUplinkTimeout(Duration.ofSeconds(1))
                .build();

        final TestServer s = new TestServer(server);
        s.start();
    }
}
