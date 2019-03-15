package com.mackenziehigh.socius.web.server;

import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import org.junit.Assert;

public final class SocketTester
{
    private final Socket sock;

    public SocketTester (final String host,
                         final int port)
            throws IOException
    {
        sock = new Socket(host, port);
    }

    public void send (final String text)
            throws IOException
    {
        sock.getOutputStream().write(text.getBytes(StandardCharsets.US_ASCII));
        sock.getOutputStream().flush();
    }

    public void sendln ()
            throws IOException
    {
        sendln("");
    }

    public void sendln (final String text)
            throws IOException
    {
        send(text + "\r\n");
    }

    public void recv (final String text)
            throws IOException
    {
        final byte[] expected = text.getBytes(StandardCharsets.US_ASCII);
        final byte[] actual = new byte[expected.length];
        ByteStreams.read(sock.getInputStream(), actual, 0, actual.length);
        Assert.assertArrayEquals(expected, actual);
    }

    public void recvln (final String text)
            throws IOException
    {
        recv(text + "\r\n");
    }

    public void recvln ()
            throws IOException
    {
        recvln("");
    }

    public void closed ()
            throws IOException
    {
        try
        {
            final int chr = sock.getInputStream().read();
            Assert.assertEquals(-1, chr);
        }
        finally
        {
            sock.close();
        }
    }

    public void close ()
            throws IOException
    {
        sock.close();
    }

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

    public void readAndPrint ()
            throws IOException
    {
        for (int c; (c = sock.getInputStream().read()) >= 0;)
        {
            System.out.print(Character.toString((char) c));
        }
    }
}
