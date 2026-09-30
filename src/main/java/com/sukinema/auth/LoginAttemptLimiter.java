package com.sukinema.auth;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Frena los intentos de adivinar contraseñas: tras varios fallos seguidos, bloquea ese correo un rato. */
@Component
public class LoginAttemptLimiter {

    static final int MAX_FAILURES = 5;
    static final Duration LOCK_DURATION = Duration.ofMinutes(5);
    private static final int MAX_TRACKED = 10_000;

    private record Attempts(int failures, Instant lastFailure) {
    }

    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();
    private final Clock clock;

    public LoginAttemptLimiter() {
        this(Clock.systemUTC());
    }

    LoginAttemptLimiter(Clock clock) {
        this.clock = clock;
    }

    public boolean isBlocked(String key) {
        Attempts current = attempts.get(key);
        if (current == null) {
            return false;
        }
        if (isExpired(current)) {
            attempts.remove(key);
            return false;
        }
        return current.failures() >= MAX_FAILURES;
    }

    public void recordFailure(String key) {
        if (attempts.size() >= MAX_TRACKED) {
            attempts.values().removeIf(this::isExpired);
        }
        Instant now = clock.instant();
        attempts.merge(key, new Attempts(1, now),
                (previous, first) -> isExpired(previous) ? first : new Attempts(previous.failures() + 1, now));
    }

    public void reset(String key) {
        attempts.remove(key);
    }

    private boolean isExpired(Attempts entry) {
        return clock.instant().isAfter(entry.lastFailure().plus(LOCK_DURATION));
    }
}
