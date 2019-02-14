package com.mackenziehigh.socius.web.server;

import com.mackenziehigh.socius.web.messages.web_m.HttpPrefix;

/**
 *
 * @author mackenzie
 */
interface Precheck
{
    enum Result
    {
        ACCEPT,
        DENY,
        FORWARD
    }

    public Result check (HttpPrefix http);
}
