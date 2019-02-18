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

import com.mackenziehigh.socius.web.messages.web_m;

/**
 * Checks a partial HTTP message for authorization, relevance, etc.
 */
interface Precheck
{
    enum Result
    {
        /**
         * Meaning: Accept the HTTP request, unconditionally.
         */
        ACCEPT,
        /**
         * Meaning: Reject the HTTP request, unconditionally.
         */
        DENY,
        /**
         * Meaning: Forward the HTTP request to the next pre-check,
         * because this pre-check was unable to accept or deny the HTTP request.
         */
        FORWARD
    }

    /**
     * Quickly verify whether a request should be accepted by the server.
     *
     * <p>
     * The request will not have a body, as it has not been read off the socket yet.
     * Likewise, the request will not have a sequence-number or a correlation-Id,
     * because those are only assigned to full requests that are accepted by the server.
     * </p>
     *
     * @param http contains the initial header information of the request.
     * @return a result indicating whether to accept or deny the request.
     */
    public Result check (web_m.HttpRequest http);
}
