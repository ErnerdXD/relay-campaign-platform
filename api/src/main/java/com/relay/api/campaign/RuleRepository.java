package com.relay.api.campaign;

import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RuleRepository {

    private final JdbcTemplate jdbc;

    public RuleRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean isSuppressed(String tenantId, String recipientRef, Channel channel) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM suppressions
                WHERE tenant_id = ? AND recipient_ref = ? AND channel = ?
                """, Integer.class, tenantId, recipientRef, channel.name());
        return count != null && count > 0;
    }

    public boolean isDuplicate(String tenantId, String dedupeKey, long jobId, Instant since) {
        if (dedupeKey == null) {
            return false;
        }
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM notification_jobs
                WHERE tenant_id = ? AND dedupe_key = ? AND status = 'SENT'
                  AND sent_at >= ? AND id <> ?
                """, Integer.class, tenantId, dedupeKey, Timestamp.from(since), jobId);
        return count != null && count > 0;
    }
}
