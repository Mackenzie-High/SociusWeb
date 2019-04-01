package com.mackenziehigh.socius.web.examples;

import com.google.protobuf.ByteString;
import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor;
import com.mackenziehigh.socius.web.messages.web_m.ServerSideHttpRequest;
import com.mackenziehigh.socius.web.messages.web_m.ServerSideHttpResponse;
import com.mackenziehigh.socius.web.server.WebServer;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.time.Instant;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

/**
 * Trivial Server Example.
 */
public final class Example
{
    /**
     * Create and start the server.
     *
     * @throws Exception for brevity in the example.
     */
    private void start ()
            throws Exception
    {
        /**
         * Create the SSL Context that provides the SSL certificate, etc.
         */
        final SSLContext context = createSSLContext();

        /**
         * Create the secured server.
         */
        final WebServer server = WebServer.newWebServer()
                .withDefaultSettings()
                .withPort(8080)
                .withAcceptFilter()
                .withSecureSockets(() -> secure(context))
                .build()
                .start();

        /**
         * Create an actor that will asynchronously handle the HTTP requests.
         */
        final Stage stage = Cascade.newStage();
        final Actor<ServerSideHttpRequest, ServerSideHttpResponse> webapp = stage
                .newActor()
                .withScript(this::onHttpRequest)
                .create();

        /**
         * Connect the actor to the server.
         * The server will send request to the actor.
         * The actor will then send responses to the server.
         */
        server.requestsOut().connect(webapp.input());
        server.responsesIn().connect(webapp.output());
    }

    private SSLContext createSSLContext ()
            throws Exception
    {
        try (FileInputStream fin = new FileInputStream("/tmp/keystore.jks"))
        {
            // Create the KeyStore.
            final KeyStore keystore = KeyStore.getInstance("JKS");
            keystore.load(fin, "password".toCharArray());

            // Create the KeyManager.
            final KeyManagerFactory factory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            factory.init(keystore, "letmein".toCharArray());
            final KeyManager[] manager = factory.getKeyManagers();

            // Create the SSLContext.
            final SSLContext context = SSLContext.getInstance("TLSv1.2");
            context.init(manager, null, null);

            return context;
        }
    }

    private SSLEngine secure (final SSLContext context)
    {
        final SSLEngine engine = context.createSSLEngine();
        engine.setUseClientMode(false);
        return engine;
    }

    /**
     * This method defines how the actor will handle each HTTP request.
     *
     * @param request was just received by the actor from the server.
     * @return the generate HTTP response.
     */
    private ServerSideHttpResponse onHttpRequest (final ServerSideHttpRequest request)
    {
        final String html = String.format("<p>Hello World at %s</p>\r\n", Instant.now());

        final ServerSideHttpResponse response = ServerSideHttpResponse
                .newBuilder()
                .setCorrelationId(request.getCorrelationId())
                .setStatus(200)
                .setBody(ByteString.copyFromUtf8(html))
                .setContentType("text/html")
                .build();

        return response;
    }

    public static void main (String[] args)
            throws Exception
    {
        final Example webapp = new Example();
        webapp.start();
        System.in.read();
    }
}
