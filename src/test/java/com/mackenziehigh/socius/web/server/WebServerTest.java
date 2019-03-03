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

import static junit.framework.Assert.*;
import org.junit.Test;

/**
 * Unit Test.
 */
public final class WebServerTest
{
    private void newConfigurableServer (final String omit)
    {
        final WebServer.Builder builder = WebServer.newWebServer();
        builder.withPredicateAccept();
        fail();
    }

    /**
     * Test: 20190303101911066030
     *
     * <p>
     * Method: <code></code>
     * </p>
     *
     * <p>
     * Case:
     * </p>
     */
    @Test
    public void test20190303101911066030 ()
    {
        System.out.println("Test: 20190303101911066030");
        fail();
    }
}
