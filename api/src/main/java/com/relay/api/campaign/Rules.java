package com.relay.api.campaign;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class Rules {

    private Rules() {
    }

    public enum Action {
        ALLOW, SKIP, DELAY
    }

    public record Decision(Action action, String reason, Instant notBefore) {
        static Decision allow() {
            return new Decision(Action.ALLOW, null, null);
        }

        static Decision skip(String reason) {
            return new Decision(Action.SKIP, reason, null);
        }

        static Decision delay(String reason, Instant notBefore) {
            return new Decision(Action.DELAY, reason, notBefore);
        }
    }

    public static Decision decide(Job job, boolean suppressed, boolean duplicate, Instant now) {
        if (suppressed) {
            return Decision.skip("recipient_unsubscribed");
        }
        if (duplicate) {
            return Decision.skip("duplicate_message");
        }
        if (job.channel() != Channel.EMAIL && !job.transactional() && isQuietHour(job.timezone(), now)) {
            return Decision.delay("quiet_hours", nextMorning(job.timezone(), now));
        }
        return Decision.allow();
    }

    static boolean isQuietHour(String timezone, Instant now) {
        int hour = now.atZone(ZoneId.of(timezone)).getHour();
        return hour >= 22 || hour < 8;
    }

    static Instant nextMorning(String timezone, Instant now) {
        ZonedDateTime local = now.atZone(ZoneId.of(timezone));
        ZonedDateTime morning = local.getHour() >= 22
                ? local.plusDays(1).withHour(8).withMinute(0).withSecond(0).withNano(0)
                : local.withHour(8).withMinute(0).withSecond(0).withNano(0);
        return morning.toInstant();
    }
}
