package com.mackenziehigh.socius.web.server;

import com.google.protobuf.ByteString;
import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.socius.web.messages.web_m;

/**
 *
 * @author mackenzie
 */
final class Main01
{
    public static void main (String[] args)
            throws InterruptedException
    {
        final Cascade.Stage stage = Cascade.newStage();

        final WebServer server = WebServer
                .newWebServer()
                .withDefaultSettings()
                .withPredicateAccept()
                .build();

        final Cascade.Stage.Actor<web_m.ServerSideHttpRequest, web_m.ServerSideHttpResponse> website = stage.newActor().withScript(Main01::onRequest).create();
        server.requestsOut().connect(website.input());
        server.responsesIn().connect(website.output());

        server.start();

        Thread.sleep(200000);
    }

    private static web_m.ServerSideHttpResponse onRequest (final web_m.ServerSideHttpRequest request)
    {
        final byte[] bytes = request.toString().getBytes();

        if (request.getPath().contains("/sleep"))
        {
            try
            {
                Thread.sleep(10_000);
            }
            catch (InterruptedException ex)
            {
                // Pass
            }
        }

        final web_m.ServerSideHttpResponse response = web_m.ServerSideHttpResponse
                .newBuilder()
                .setRequest(request)
                .setContentType("text/martian")
                .setStatus(200)
                .setBody(ByteString.copyFrom(bytes))
                .build();

        return response;
    }
}
