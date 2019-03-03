package com.mackenziehigh.socius.web.server;

import com.google.protobuf.ByteString;
import com.mackenziehigh.socius.web.messages.web_m;

/**
 *
 * @author mackenzie
 */
public class Main01
{
    public static void main (String[] args)
            throws InterruptedException
    {
//        final Cascade.Stage stage = Cascade.newStage();
//
//        final WebServer server = WebServer
//                .newWebServer()
//                .withSoftConnectionLimit(10)
//                .withHardConnectionLimit(20)
//                .withMaxMessagesPerRead(1)
//                .withRecvBufferAllocator(64, 1024, 32000)
//                //                .withSlowReadTimeout(Duration.ofSeconds(1))
//                .withResponseTimeout(Duration.ofSeconds(1))
//                .withConnectionTimeout(Duration.ofSeconds(2))
//                .withBindAddress("127.0.0.1")
//                .withPort(8089)
//                .withReplyTo("Mars")
//                .withServerName("Alien")
//                .withMaxRequestSize(1 * 1024 * 1024)
//                .withMaxInitialLineSize(1024)
//                .withMaxHeaderSize(8092)
//                .withPredicateReject(x -> x.getMethod().equalsIgnoreCase("GET"))
//                .withPredicateAccept()
//                .build();
//
//        final Cascade.Stage.Actor<web_m.HttpRequest, web_m.HttpResponse> website = stage.newActor().withScript(Main01::onRequest).create();
//        server.requestsOut().connect(website.input());
//        server.responsesIn().connect(website.output());
//
//        server.start();
//
//        Thread.sleep(2000);
    }

    private static web_m.HttpResponse onRequest (final web_m.HttpRequest request)
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

        final web_m.HttpResponse response = web_m.HttpResponse
                .newBuilder()
                .setRequest(request)
                .setContentType("text/martian")
                .setStatus(200)
                .setBody(ByteString.copyFrom(bytes))
                .build();

        return response;
    }
}
