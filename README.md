*This project is still under initial development!*

Socius Web provides an embeddable asynchronous non-blocking message-oriented [Netty](https://github.com/netty/netty)-based HTTP proxy server that facilitates the processing of HTTP requests as a multiplexed stream of generic [Protobuf](https://developers.google.com/protocol-buffers/)-encoded messages flowing through one-or-more [Cascade](https://github.com/Mackenzie-High/Cascade)-based actors. 

Let's break that down a little:
1. Socius Web is an **HTTP server**.
2. Socius Web is **embeddable**, which means that the server is simply an object within your Java program, rather than a standalone process of its own. 
3. Socius Web is **asynchronous**, which means the server can handle many requests concurrently. 
4. Socius Web is **non-blocking**, which means the server uses NIO-based sockets. Consequently, the server can handle thousands of simultaneous connections using a single thread. Other servers, which use blocking sockets, spawn a separate thread for each connection, which does not scale well.
5. Socius Web is **message-oriented**, which means that requests/responses are converted to/from message objects, where each message has a finite size. Thus, Socius Web is *not* appropriate, if you need streaming capablities. 

Of Note:
* Socius Web is intended for use as an upstream server hidden behind a load balancer or other edge server. 
* Socius Web is *not* intended to be used directly as an edge server itself. 
* Socius Web is *not* a general purpose web server. 

# Getting Started

## Example without SSL/TLS

```java
package com.mackenziehigh.socius.web.examples;

import com.google.protobuf.ByteString;
import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.cascade.Cascade.Stage;
import com.mackenziehigh.cascade.Cascade.Stage.Actor;
import com.mackenziehigh.socius.web.messages.web_m.ServerSideHttpRequest;
import com.mackenziehigh.socius.web.messages.web_m.ServerSideHttpResponse;
import com.mackenziehigh.socius.web.server.WebServer;
import java.io.IOException;
import java.time.Instant;

/**
 * Trivial Server Example.
 */
public final class Example
{
    /**
     * Create and start the server.
     */
    private void start ()
    {
        final WebServer server = WebServer.newWebServer()
                .withDefaultSettings()
                .withPort(8080)
                .withAcceptFilter()
                .withUnsecureSockets()
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
         * The server will send requests to the actor.
         * The actor will then send responses to the server.
         */
        server.requestsOut().connect(webapp.input());
        server.responsesIn().connect(webapp.output());
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
            throws IOException
    {
        final Example webapp = new Example();
        webapp.start();
        System.in.read();
    }
}
```

## Example with Self-Signed SSL/TLS

### Generate Self-Signed Certificate

```bash
cd /tmp/ ; rm -f /tmp/keystore.jks ; keytool -genkey -keyalg RSA -alias selfsigned -keystore keystore.jks -storepass password -keypass letmein -validity 360 -keysize 2048 -ext SAN=DNS:localhost,IP:127.0.0.1 -validity 9999 -dname "CN=AA, OU=BB, O=CC, L=Washington, ST=DC, C=US"
```

### Source Code

```java
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
         * The server will send requests to the actor.
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
```

### Query Server

```bash
curl -k "https://127.0.0.1:8080"
```

# Encoding

Socius Web uses Protobuf-encoded messages between itself and the connected web-application. 

Message Definitions: [web_m.proto](https://github.com/Mackenzie-High/SociusWeb/blob/master/src/main/java/com/mackenziehigh/socius/web/messages/web_m.proto)

Advantages:
1. Since the messages are Protobuf-based, they can be trivially serialized/deserialized. Moreover, the encoding is not Java-centric. Rather, the messages can be deserialized easily in any language that Protobuf supports. 
2. The messages are inherently immutable. 

Disadvantages:
1. GC Pressure

## Requests

| Attribute                                   | Type                         | About                  |
| ------------------------------------------- | ---------------------------- | ---------------------- |
| Server Name | String | Human readable name of the server receiving the request. |
| Server ID   | String | UUID of the server receiving the request. |
| Reply To    | String | Optional, ignored by server, for application use. | 

## Responses

# Configuration Options

Socius Web is configurable to meet your security and performance needs. 

General Server Settings:

| Name    | Description | Default |
| ------- | ----------- | ------- | 
| Bind Address           | The network-interface that the server will listen on. | 127.0.0.1 |
| Port                   | The port number that the server will listen on.       | 8080      | 
| Soft Connection Limit  | The limit at which new connections will be sent a default error-response. | TODO |
| Hard Connection Limit  | The limit at which new connections will simply not be accepted. | TODO |

General Per Connection Settings:

| Name    | Description | Default |
| ------- | ----------- | ------- |
| Uplink Timeout         | The max duration to wait for a request to be read of the socket | TODO |
| Response Timeout       | The max duration to wait for a response to be formulated.  | TODO |
| Downlink Timeout       | The max duration to wait for a response to be written to the socket. | TODO |
| Request Filter         | The predicate(s) that filter out unwanted requests    | DENY      | 

General HTTP Settings: 
| Name    | Description | Default |
| ------- | ----------- | ------- |
| Max Request Size       | The max number of bytes in a single HTTP request  | TODO      |
| Max Initial Line Size  | The max number of bytes in the first line of the request | TODO |
| Max Headers Size       | The max number of bytes in the initial request headers. | TODO |

Settings for Compression of HTTP Responses:

| Name    | Description | Default |
| ------- | ----------- | ------- | 
| Compression Level       | A value between zero (no compression) and nine (max compression) | TODO |
| Compression Window Bits | A value between nine (worse compression, less memory) and fifteen (better compression, more memory). | TODO |
| Compression Memory Level | A value between one (worse compression, less memory) and nine (better compression, more memory). | TODO |
| Compression Threshold | The minimum size a response must be in order for compression to be applied. | TODO |

Settings for Advanced Performance Tuning:

| Name    | Description | Default |
| ------- | ----------- | ------- | 
| Max Messages Per Read  | The max number of times the socket will be polled per read.   | TODO |  
| Recv Allocator Min     | The min size of the memory buffers used for receiving data.   | TODO | 
| Recv Allocator Max     | The max size of the memory buffers used for receiving data.   | TODO |
| Recv Allocator Initial | The initial size of the memory buffers used for receiving data.   | TODO | 



## Defaults

## Bind Address

## Port

## Request Filters

### Request Filter - Default

### Request Filter - ACCEPT

### Request Filter - REJECT

### Request Filter - DENY

## Max Request Size

## Max Initial Line Size

## Max Headers Size

## Soft Connection Limit

## Hard Connection Limit 

## Uplink Timeout

## Response Timeout

## Downlink Timeout

# Loggers

## Server Loggers

## Connection Loggers

# Why HTTP 1.0 Responses

TODO


