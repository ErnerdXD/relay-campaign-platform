package com.relay.api.campaign;

import java.util.EnumMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ChannelRateLimiter {

    private final Map<Channel, TokenBucket> buckets = new EnumMap<>(Channel.class);

    public ChannelRateLimiter(
            @Value("${relay.rate-limit.per-minute:100}") int perMinute,
            @Value("${relay.rate-limit.burst:100}") int burst) {
        double refillPerSecond = perMinute / 60.0;
        for (Channel channel : Channel.values()) {
            buckets.put(channel, new TokenBucket(burst, refillPerSecond));
        }
    }

    public boolean acquire(Channel channel, long timeoutMs) throws InterruptedException {
        return buckets.get(channel).acquire(timeoutMs);
    }
}
