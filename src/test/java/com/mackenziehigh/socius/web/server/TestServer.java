package com.mackenziehigh.socius.web.server;

import com.google.common.io.ByteStreams;
import com.google.common.io.Resources;
import com.google.protobuf.ByteString;
import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor;
import com.mackenziehigh.socius.web.messages.web_m.ServerSideHttpRequest;
import com.mackenziehigh.socius.web.messages.web_m.ServerSideHttpResponse;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * An instance of this class is a web-application that is
 * tailored for use in the web-server integration-tests.
 */
public final class TestServer
{
    private final Stage stage = Cascade.newStage();

    private final WebServer server;

    public TestServer (final WebServer server)
    {
        this.server = server;
    }

    private ServerSideHttpResponse echoService (final ServerSideHttpRequest request)
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

    private void start ()
            throws InterruptedException
    {
        /**
         * Create the actors that are web-service end-points.
         */
        final Actor<ServerSideHttpRequest, ServerSideHttpResponse> echo = stage.newActor().withScript(this::echoService).create();

        /**
         * Connect the web-service end-points to the server.
         */
        server.requestsOut().connect(echo.input());
        server.responsesIn().connect(echo.output());

        /**
         * Bind the server to the server socket and begin serving.
         */
        server.start();
        Thread.sleep(2_000);
    }

    public int runTest (final String name)
            throws IOException,
                   InterruptedException
    {
        start();

        /**
         * Read-in the test-script.
         */
        final URL path = Resources.getResource(name);
        String script = Resources.toString(path, StandardCharsets.UTF_8);
        final String address = server.getBindAddress() + ":" + server.getPort();
        script = script.replace("${address}", address);

        /**
         * Write-out the test-script into a temporary BASH file.
         */
        final File temp = Files.createTempFile(name, ".sh").toFile();
        Files.write(temp.toPath(), script.getBytes(StandardCharsets.UTF_8));

        /**
         * Execute the test-script.
         */
        final String[] command = new String[2];
        command[0] = "bash";
        command[1] = temp.getAbsolutePath();
        final Process process = Runtime.getRuntime().exec(command);
        final int returnCode = process.waitFor();
        System.out.println(returnCode);
        System.out.println(new String(ByteStreams.toByteArray(process.getInputStream())));
        System.err.println(new String(ByteStreams.toByteArray(process.getErrorStream())));

        /**
         * Shutdown the server, etc.
         */
        server.stop();
        stage.close();

        return returnCode;
    }

    public static void main (String[] args)
            throws IOException,
                   InterruptedException
    {
        final WebServer server = WebServer
                .newWebServer()
                .withDefaultSettings()
                .withPredicateAccept()
                .build();

        final TestServer s = new TestServer(server);
        s.runTest("Test0001.txt");

    }
}
