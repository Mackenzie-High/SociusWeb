package com.mackenziehigh.socius.web.server;

/**
 *
 * @author mackenzie
 */
@FunctionalInterface
public interface ServerLoggerFactory
{
    public ServerLogger newServerLogger ();
}
