package com.mackenziehigh.socius.web;

import java.util.UUID;

interface OutputConnection<T>
{
    public UUID connectionId ();

    public void write (T message);

    public void close ();
}
