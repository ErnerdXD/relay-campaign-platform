package com.relay.api.campaign;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class BackoffTest {

    @Test
    void growsExponentiallyAndStopsAtSixtySeconds() {
        assertThat(Backoff.delay(1)).isBetween(Duration.ofSeconds(1), Duration.ofMillis(1300));
        assertThat(Backoff.delay(2)).isBetween(Duration.ofSeconds(2), Duration.ofMillis(2300));
        assertThat(Backoff.delay(3)).isBetween(Duration.ofSeconds(4), Duration.ofMillis(4300));
        assertThat(Backoff.delay(99)).isBetween(Duration.ofSeconds(60), Duration.ofMillis(60300));
    }
}
