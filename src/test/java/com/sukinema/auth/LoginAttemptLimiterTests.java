package com.sukinema.auth;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginAttemptLimiterTests {

    /** Reloj que se adelanta a mano para no tener que esperar de verdad. */
    private static class ManualClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T00:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }

    private final ManualClock clock = new ManualClock();
    private final LoginAttemptLimiter limiter = new LoginAttemptLimiter(clock);

    @Test
    void blocksAfterMaxFailuresAndUnblocksAfterLockDuration() {
        for (int i = 0; i < LoginAttemptLimiter.MAX_FAILURES - 1; i++) {
            limiter.recordFailure("a@b.c");
        }
        assertFalse(limiter.isBlocked("a@b.c"), "aún queda un intento");

        limiter.recordFailure("a@b.c");
        assertTrue(limiter.isBlocked("a@b.c"));
        assertFalse(limiter.isBlocked("otra@b.c"), "el bloqueo es por correo");

        clock.advance(LoginAttemptLimiter.LOCK_DURATION.minusSeconds(1));
        assertTrue(limiter.isBlocked("a@b.c"));

        clock.advance(Duration.ofSeconds(2));
        assertFalse(limiter.isBlocked("a@b.c"), "pasado el tiempo, se puede volver a intentar");
    }

    @Test
    void successResetsTheCounter() {
        for (int i = 0; i < LoginAttemptLimiter.MAX_FAILURES - 1; i++) {
            limiter.recordFailure("a@b.c");
        }
        limiter.reset("a@b.c");
        limiter.recordFailure("a@b.c");
        assertFalse(limiter.isBlocked("a@b.c"));
    }

    @Test
    void oldFailuresDoNotAccumulate() {
        for (int i = 0; i < LoginAttemptLimiter.MAX_FAILURES - 1; i++) {
            limiter.recordFailure("a@b.c");
        }
        clock.advance(LoginAttemptLimiter.LOCK_DURATION.plusSeconds(1));
        limiter.recordFailure("a@b.c");
        assertFalse(limiter.isBlocked("a@b.c"), "los fallos antiguos ya no cuentan");
    }
}
