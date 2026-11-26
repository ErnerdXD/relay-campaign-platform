package com.relay.api.campaign;

import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

@Component
public class ProviderSimulator {

    public record Result(boolean success, String error, int latencyMs) {
    }

    private static final String[] ERRORS = {
            "provider_timeout", "temporary_unavailable", "upstream_throttled"
    };

    public Result send(Channel channel) {
        int latency = ThreadLocalRandom.current().nextInt(50, 201);
        try {
            Thread.sleep(latency);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return new Result(false, "interrupted", latency);
        }

        boolean failed = ThreadLocalRandom.current().nextDouble() < 0.20;
        if (failed) {
            String error = ERRORS[ThreadLocalRandom.current().nextInt(ERRORS.length)];
            return new Result(false, error, latency);
        }
        return new Result(true, null, latency);
    }
}
