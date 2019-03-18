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
package com.mackenziehigh.socius.web.server.loggers;

import com.mackenziehigh.socius.web.messages.web_m.ServerSideHttpRequest;
import com.mackenziehigh.socius.web.messages.web_m.ServerSideHttpResponse;
import com.mackenziehigh.socius.web.server.WebServer;
import java.net.InetSocketAddress;

/**
 * Logger for Web-Servers.
 */
public interface WebLogger
{
    /**
     * This method is invoked whenever a new connection is established,
     * etc, so that a dedicated logger instance is used per connection,
     * which facilitates storing connection-specific state within the instance.
     *
     * @return a new logger instance based on this one.
     */
    public WebLogger extend ();

    /**
     * This method is invoked when the web-server starts up.
     *
     * @param server is the web-server itself.
     */
    public void onStarted (WebServer server);

    /**
     * This method is invoked when the web-server shuts down.
     */
    public void onStopped ();

    /**
     * This method is invoked when a new socket connection is established.
     *
     * <p>
     * Before this method is invoked, <code>extend()</code> will be invoked.
     * This method is invoked on the instance returned by <code>extend()</code>.
     * </p>
     *
     * @param local is the local-address of the socket.
     * @param remote is the remote-address of the socket.
     */
    public void onConnect (InetSocketAddress local,
                           InetSocketAddress remote);

    /**
     * This method is invoked when the web-server disconnects from a socket.
     *
     * <p>
     * This method is invoked on the same instance as <code>connect()</code>.
     * </p>
     *
     * @param local is the local-address of the socket.
     * @param remote is the remote-address of the socket.
     */
    public void onDisconnect (InetSocketAddress local,
                              InetSocketAddress remote);

    /**
     * This method is invoked, if an incoming HTTP request is deemed
     * to be acceptable, according to the pre-check rules.
     *
     * <p>
     * This method is invoked before the HTTP request is fully translated.
     * </p>
     *
     * <p>
     * This method is invoked on the same instance as <code>connect()</code>.
     * </p>
     *
     * @param request is an incoming (partial) HTTP request.
     */
    public void onAccepted (ServerSideHttpRequest request);

    /**
     * This method is invoked, if an incoming HTTP request is deemed
     * to be rejected, according to the pre-check rules.
     *
     * <p>
     * This method is invoked before the HTTP request is fully translated.
     * </p>
     *
     * <p>
     * This method is invoked on the same instance as <code>connect()</code>.
     * </p>
     *
     * @param request is an incoming (partial) HTTP request.
     * @param status is the HTTP status-code used in the rejection response.
     */
    public void onRejected (ServerSideHttpRequest request,
                            int status);

    /**
     * This method is invoked, if an incoming HTTP request is deemed
     * to be denied, according to the pre-check rules.
     *
     * <p>
     * This method is invoked before the HTTP request is fully translated.
     * </p>
     *
     * <p>
     * This method is invoked on the same instance as <code>connect()</code>.
     * </p>
     *
     * @param request is an incoming (partial) HTTP request.
     */
    public void onDenied (ServerSideHttpRequest request);

    /**
     * This method is invoked once an incoming HTTP request is fully-translated
     * and is being sent to the connected actors for processing.
     *
     * <p>
     * This method is invoked on the same instance as <code>connect()</code>.
     * </p>
     *
     * @param request is an incoming (fully-translated) HTTP request.
     */
    public void onRequest (ServerSideHttpRequest request);

    /**
     * This method is invoked when a response is ready to be sent to the client.
     *
     * <p>
     * This method is only invoked given responses from connected actors and/or response-timeouts.
     * This method is not invoked when the response is due to a request-filter rejection
     * or any other phase of the reception pipeline.
     * </p>
     *
     * <p>
     * This method is invoked on the same instance as <code>connect()</code>.
     * </p>
     *
     * @param response is the response that will be sent to the client.
     */
    public void onResponse (ServerSideHttpResponse response);

    /**
     * This method is invoked, if the connection shall be closed due to an uplink-timeout expiration.
     *
     * <p>
     * This method is invoked on the same instance as <code>connect()</code>.
     * </p>
     */
    public void onUplinkTimeout ();

    /**
     * This method is invoked, if the connection shall be closed due to an downlink-timeout expiration.
     *
     * <p>
     * This method is invoked on the same instance as <code>connect()</code>.
     * </p>
     */
    public void onDownlinkTimeout ();

    /**
     * This method is invoked, if the connection shall be closed due to an response-timeout expiration.
     *
     * <p>
     * This method is invoked on the same instance as <code>connect()</code>.
     * </p>
     */
    public void onResponseTimeout ();

    /**
     * This method is invoked, if the connection shall be closed due to an connection-timeout expiration.
     *
     * <p>
     * This method is invoked on the same instance as <code>connect()</code>.
     * </p>
     */
    public void onConnectionTimeout ();

    /**
     * This method is invoked whenever a connection is rejected,
     * because too many connections are currently open.
     *
     * @param count is the number of open connections.
     */
    public void onTooManyConnections (int count);

    /**
     * This method is invoked, if an unexpected exception occurs.
     *
     * <p>
     * This method may be invoked on any instance at any time.
     * </p>
     *
     * @param cause is the reason that that this method is being invoked.
     */
    public void onException (Throwable cause);

}
