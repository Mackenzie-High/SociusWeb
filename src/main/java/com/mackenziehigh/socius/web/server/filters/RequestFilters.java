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
package com.mackenziehigh.socius.web.server.filters;

import com.mackenziehigh.socius.web.messages.web_m.ServerSideHttpRequest;
import com.mackenziehigh.socius.web.server.filters.RequestFilter.Action;
import com.mackenziehigh.socius.web.server.filters.RequestFilter.ActionType;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.function.Predicate;

/**
 * Utility methods for <code>RequestFilter</code>s.
 */
public final class RequestFilters
{
    /**
     * Prevent Instantiation.
     */
    private RequestFilters ()
    {
        // Pass.
    }

    /**
     * The ACCEPT action-type does not, itself, cause an HTTP response;
     * therefore, we can use a constant representation,
     * because no status-code is needed.
     */
    private static final Action ACCEPT = new RequestFilter.Action()
    {
        @Override
        public RequestFilter.ActionType action ()
        {
            return ActionType.ACCEPT;
        }

        @Override
        public OptionalInt status ()
        {
            return OptionalInt.empty();
        }
    };

    /**
     * The DENY action-type does not cause an HTTP response;
     * therefore, we can use a constant representation,
     * because no status-code is needed.
     */
    private static final Action DENY = new RequestFilter.Action()
    {
        @Override
        public RequestFilter.ActionType action ()
        {
            return ActionType.DENY;
        }

        @Override
        public OptionalInt status ()
        {
            return OptionalInt.empty();
        }
    };

    /**
     * The FORWARD action-type does not, itself, cause an HTTP response;
     * therefore, we can use a constant representation,
     * because no status-code is needed.
     */
    private static final Action FORWARD = new RequestFilter.Action()
    {
        @Override
        public RequestFilter.ActionType action ()
        {
            return ActionType.FORWARD;
        }

        @Override
        public OptionalInt status ()
        {
            return OptionalInt.empty();
        }
    };

    private static final RequestFilter FILTER_ACCEPT = request -> ACCEPT;

    private static final RequestFilter FILTER_DENY = request -> DENY;

    private static final RequestFilter FILTER_FORWARD = request -> FORWARD;

    /**
     * Create a predicate that causes any HTTP request to be accepted.
     *
     * @return the new predicate.
     */
    public static RequestFilter accept ()
    {
        return FILTER_ACCEPT;
    }

    /**
     * Create a predicate that causes any HTTP request, which obeys a given condition, to be accepted.
     *
     * @param condition returns true, if the HTTP request should be accepted.
     * @return the new predicate.
     */
    public static RequestFilter accept (final Predicate<ServerSideHttpRequest> condition)
    {
        return request -> condition.test(request) ? ACCEPT : FORWARD;
    }

    /**
     * Create a predicate that causes any HTTP request to be denied.
     *
     * @return the new predicate.
     */
    public static RequestFilter deny ()
    {
        return FILTER_DENY;
    }

    /**
     * Create a predicate that causes any HTTP request, which violates a given condition, to be denied.
     *
     * @param condition returns true, if the HTTP request should be denied.
     * @return the new predicate.
     */
    public static RequestFilter deny (final Predicate<ServerSideHttpRequest> condition)
    {
        return request -> condition.test(request) ? DENY : FORWARD;
    }

    /**
     * Create a predicate that causes any HTTP request, which violates a given condition, to be rejected.
     *
     * @param status is the HTTP response code that will be used in the rejection response.
     * @param condition returns true, if the HTTP request should be rejected.
     * @return the new predicate.
     * @throws IllegalArgumentException if the status is not a 4xx or 5xx response code.
     */
    public static RequestFilter reject (final int status,
                                        final Predicate<ServerSideHttpRequest> condition)
    {
        final RequestFilter reject = reject(status);
        final RequestFilter conditional = request -> condition.test(request)
                ? reject.apply(request)
                : forward().apply(request);
        return conditional;
    }

    /**
     * Create a predicate that causes any HTTP request to be rejected.
     *
     * @param status is the HTTP response code that will be used in the rejection response.
     * @return the new predicate.
     * @throws IllegalArgumentException if the status is not a 4xx or 5xx response code.
     */
    public static RequestFilter reject (final int status)
    {
        if (status < 400 || status >= 600)
        {
            throw new IllegalArgumentException("4xx or 5xx Response Required: " + status);
        }

        final OptionalInt statusCode = OptionalInt.of(status);

        return request ->
        {
            return new Action()
            {
                @Override
                public ActionType action ()
                {
                    return ActionType.REJECT;
                }

                @Override
                public OptionalInt status ()
                {
                    return statusCode;
                }
            };
        };
    }

    /**
     * Create a predicate that always forwards.
     *
     * <p>
     * This predicate is of limited utility, except within the server itself.
     * </p>
     *
     * @return the new predicate.
     */
    public static RequestFilter forward ()
    {
        return FILTER_FORWARD;
    }

    /**
     * Create a predicate that chains two predicates together.
     *
     * @param first will be applied to incoming requests first.
     * @param second will be applied to incoming requests second,
     * if and only if, the first predicate was inconclusive.
     * @return the new combined predicate.
     */
    public static RequestFilter chain (final RequestFilter first,
                                       final RequestFilter second)
    {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");

        return (request) ->
        {
            final Action inner = first.apply(request);
            final Action outer = inner.action() == ActionType.FORWARD ? second.apply(request) : inner;
            return outer;
        };
    }

}
