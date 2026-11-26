package com.relay.api.campaign;

public record Job(
        long id,
        String tenantId,
        String campaignId,
        String recipientRef,
        Channel channel,
        int attempts,
        String dedupeKey,
        String timezone,
        boolean transactional) {
}
