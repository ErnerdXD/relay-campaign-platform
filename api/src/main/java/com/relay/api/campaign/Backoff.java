package com.relay.api.campaign;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

public final class Backoff {

    private Backoff() {
    }

    public static Duration delay(int attempt) {
        long base = Math.min(60, 1L << Math.min(attempt - 1, 6));
        long jitterMs = ThreadLocalRandom.current().nextLong(200);
        return Duration.ofSeconds(base).plusMillis(jitterMs);
    }
}
