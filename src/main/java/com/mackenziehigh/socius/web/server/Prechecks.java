package com.mackenziehigh.socius.web.server;

import com.mackenziehigh.socius.web.messages.web_m.ServerSideHttpRequest;
import com.mackenziehigh.socius.web.server.Precheck.Action;
import com.mackenziehigh.socius.web.server.Precheck.ActionType;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.function.Predicate;

/**
 * Utility methods for <code>Precheck</code>s.
 */
public final class Prechecks
{

    /**
     * The ACCEPT action-type does not, itself, cause an HTTP response;
     * therefore, we can use a constant representation,
     * because no status-code is needed.
     */
    private static final Action ACCEPT = new Precheck.Action()
    {
        @Override
        public Precheck.ActionType action ()
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
    private static final Action DENY = new Precheck.Action()
    {
        @Override
        public Precheck.ActionType action ()
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
    private static final Action FORWARD = new Precheck.Action()
    {
        @Override
        public Precheck.ActionType action ()
        {
            return ActionType.FORWARD;
        }

        @Override
        public OptionalInt status ()
        {
            return OptionalInt.empty();
        }
    };

    private static final Precheck PRECHECK_ACCEPT = request -> ACCEPT;

    private static final Precheck PRECHECK_DENY = request -> DENY;

    private static final Precheck PRECHECK_FORWARD = request -> FORWARD;

    /**
     * Create a predicate that causes any HTTP request to be accepted.
     *
     * @return the new predicate.
     */
    public static Precheck accept ()
    {
        return PRECHECK_ACCEPT;
    }

    /**
     * Create a predicate that causes any HTTP request, which obeys a given condition, to be accepted.
     *
     * @param condition returns true, if the HTTP request should be accepted.
     * @return the new predicate.
     */
    public static Precheck accept (final Predicate<ServerSideHttpRequest> condition)
    {
        return request -> condition.test(request) ? ACCEPT : FORWARD;
    }

    /**
     * Create a predicate that causes any HTTP request to be denied.
     *
     * @return the new predicate.
     */
    public static Precheck deny ()
    {
        return PRECHECK_DENY;
    }

    /**
     * Create a predicate that causes any HTTP request, which violates a given condition, to be denied.
     *
     * @param condition returns true, if the HTTP request should be denied.
     * @return the new predicate.
     */
    public static Precheck deny (final Predicate<ServerSideHttpRequest> condition)
    {
        return request -> condition.test(request) ? DENY : FORWARD;
    }

    /**
     * Create a predicate that causes any HTTP request, which violates a given condition, to be rejected.
     *
     * @param condition returns true, if the HTTP request should be rejected.
     * @param status is the HTTP response code that will be used in the rejection response.
     * @return the new predicate.
     * @throws IllegalArgumentException if the status is not a 4xx or 5xx response code.
     */
    public static Precheck reject (final Predicate<ServerSideHttpRequest> condition,
                                   final int status)
    {
        final Precheck reject = reject(status);
        final Precheck conditional = request -> condition.test(request)
                ? reject.check(request)
                : forward().check(request);
        return conditional;
    }

    /**
     * Create a predicate that causes any HTTP request to be rejected.
     *
     * @param status is the HTTP response code that will be used in the rejection response.
     * @return the new predicate.
     * @throws IllegalArgumentException if the status is not a 4xx or 5xx response code.
     */
    public static Precheck reject (final int status)
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
     * This predicate is of limited utility,
     * except within the server itself.
     * </p>
     *
     * @return the new predicate.
     */
    public static Precheck forward ()
    {
        return PRECHECK_FORWARD;
    }

    /**
     * Create a predicate that chains two predicates together.
     *
     * @param first will be applied to incoming requests first.
     * @param second will be applied to incoming requests second,
     * if and only if, the first predicate was inconclusive.
     * @return the new combined predicate.
     */
    public static Precheck chain (final Precheck first,
                                  final Precheck second)
    {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");

        return (request) ->
        {
            final Action inner = first.check(request);
            final Action outer = inner.action() == ActionType.FORWARD ? second.check(request) : inner;
            return outer;
        };
    }
}
