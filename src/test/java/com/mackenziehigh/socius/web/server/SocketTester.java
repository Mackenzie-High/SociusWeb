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

import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import org.junit.Assert;

/**
 * An instance of this class facilitates testing Socket I/O in unit-tests.
 */
public final class SocketTester
{

    private final Socket sock;

    /**
     * Sole Constructor.
     *
     * @param host is the address that the server socket is listening on.
     * @param port is the port that the server socket is listening on.
     * @throws IOException
     */
    public SocketTester (final String host,
                         final int port)
            throws IOException
    {
        sock = new Socket(host, port);
    }

    /**
     * Send a string to the server.
     *
     * @param text will be sent to the server.
     * @throws IOException
     */
    public void send (final String text)
            throws IOException
    {
        sock.getOutputStream().write(text.getBytes(StandardCharsets.US_ASCII));
        sock.getOutputStream().flush();
    }

    /**
     * Send an empty-line to the server.
     *
     * @throws IOException
     */
    public void sendln ()
            throws IOException
    {
        sendln("");
    }

    /**
     * Send a string to the server followed by an HTTP-newline.
     *
     * @param text will be sent to the server.
     * @throws IOException
     */
    public void sendln (final String text)
            throws IOException
    {
        send(text + "\r\n");
    }

    /**
     * Expect to receive a string from the server.
     *
     * @param text is expected to come next from the server.
     * @throws IOException
     */
    public void recv (final String text)
            throws IOException
    {
        final byte[] expected = text.getBytes(StandardCharsets.US_ASCII);
        final byte[] actual = new byte[expected.length];
        ByteStreams.read(sock.getInputStream(), actual, 0, actual.length);
        Assert.assertArrayEquals(expected, actual);
    }

    /**
     * Expect to receive a string from the server followed by an HTTP-newline.
     *
     * @param text is expected to come next from the server.
     * @throws IOException
     */
    public void recvln (final String text)
            throws IOException
    {
        recv(text + "\r\n");
    }

    /**
     * Expect to receive an empty-line from the server.
     *
     * @throws IOException
     */
    public void recvln ()
            throws IOException
    {
        recvln("");
    }

    public long exhaust ()
            throws IOException
    {
        return ByteStreams.exhaust(sock.getInputStream());
    }

    /**
     * Expect that the server itself will close the connection.
     *
     * @throws IOException
     */
    public void closed ()
            throws IOException
    {
        try
        {
            /**
             * The isConnected() method does not detect when the server closes the connection.
             * Rather, we need to read from the stream and look for the EOF (-1) marker.
             */
            final int chr = sock.getInputStream().read();
            Assert.assertEquals(-1, chr);
        }
        finally
        {
            close();
        }
    }

    /**
     * Close the client side of the socket.
     *
     * @throws IOException
     */
    public void close ()
            throws IOException
    {
        sock.close();
    }

    /**
     * Cause the current thread to sleep for the given time period.
     *
     * @param millis is how long to sleep.
     */
    public void sleep (final long millis)
    {
        try
        {
            Thread.sleep(millis);
        }
        catch (InterruptedException ex)
        {
            Assert.fail(ex.getMessage());
        }
    }

    /**
     * For debugging purposes, read the response from the server
     * and then print the response to standard-output.
     *
     * @throws IOException
     */
    public void readAndPrint ()
            throws IOException
    {
        for (int c; (c = sock.getInputStream().read()) >= 0;)
        {
            System.out.print(Character.toString((char) c));
        }
    }
}
