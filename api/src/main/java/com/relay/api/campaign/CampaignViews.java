package com.relay.api.campaign;

import java.time.Instant;
import java.util.List;

public final class CampaignViews {

    private CampaignViews() {
    }

    public record Row(
            String id,
            String name,
            Channel channel,
            String status,
            Instant scheduledAt,
            Instant createdAt,
            long totalRecipients,
            long sent,
            long failed,
            long skipped) {
    }

    public record Page(List<Row> items, int page, int pageSize, long total) {
    }

    public record Minute(Instant minute, long sent, long failed) {
    }

    public record Error(String error, long count) {
    }

    public record Detail(
            String id,
            String name,
            Channel channel,
            String status,
            String template,
            boolean transactional,
            Instant scheduledAt,
            Instant createdAt,
            Instant startedAt,
            Instant completedAt,
            long totalRecipients,
            long sent,
            long failed,
            long skipped,
            long pending,
            long retryQueue,
            List<Minute> metrics,
            List<Error> errors) {
    }
}
