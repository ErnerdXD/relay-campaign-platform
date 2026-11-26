package com.relay.api.campaign;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class RulesTest {

    private static final Instant QUIET_IN_KUALA_LUMPUR = Instant.parse("2026-01-01T15:00:00Z");
    private static final Instant MIDDAY_IN_KUALA_LUMPUR = Instant.parse("2026-01-01T04:00:00Z");

    @Test
    void skipsSuppressedRecipientsBeforeAnythingElse() {
        var decision = Rules.decide(job(Channel.SMS, false), true, false, QUIET_IN_KUALA_LUMPUR);

        assertThat(decision.action()).isEqualTo(Rules.Action.SKIP);
        assertThat(decision.reason()).isEqualTo("recipient_unsubscribed");
    }

    @Test
    void skipsDuplicateMessages() {
        var decision = Rules.decide(job(Channel.EMAIL, false), false, true, MIDDAY_IN_KUALA_LUMPUR);

        assertThat(decision.action()).isEqualTo(Rules.Action.SKIP);
        assertThat(decision.reason()).isEqualTo("duplicate_message");
    }

    @Test
    void delaysSmsDuringQuietHoursUntilEightLocal() {
        var decision = Rules.decide(job(Channel.SMS, false), false, false, QUIET_IN_KUALA_LUMPUR);

        assertThat(decision.action()).isEqualTo(Rules.Action.DELAY);
        assertThat(decision.notBefore()).isEqualTo(Instant.parse("2026-01-02T00:00:00Z"));
    }

    @Test
    void transactionalSmsBypassesQuietHours() {
        var decision = Rules.decide(job(Channel.SMS, true), false, false, QUIET_IN_KUALA_LUMPUR);

        assertThat(decision.action()).isEqualTo(Rules.Action.ALLOW);
    }

    @Test
    void emailIsAllowedAtNight() {
        var decision = Rules.decide(job(Channel.EMAIL, false), false, false, QUIET_IN_KUALA_LUMPUR);

        assertThat(decision.action()).isEqualTo(Rules.Action.ALLOW);
    }

    private static Job job(Channel channel, boolean transactional) {
        return new Job(1L, "northstar", "campaign", "r1", channel, 0, "hash",
                "Asia/Kuala_Lumpur", transactional);
    }
}
