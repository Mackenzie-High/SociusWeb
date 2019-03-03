package com.mackenziehigh.socius.web.server;

/**
 *
 */
@FunctionalInterface
public interface ConnectionLoggerFactory
{
    public ConnectionLogger newConnectionLogger ();
}
