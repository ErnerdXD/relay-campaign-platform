package com.relay.api.campaign;

import com.relay.api.tenant.Tenant;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CampaignRepository {

    private final JdbcTemplate jdbc;

    public CampaignRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record CampaignSummary(String id, String status, long totalRecipients) {
    }

    public void insertCampaign(String id, Tenant tenant, String name, Channel channel, String template,
            Instant scheduledAt, boolean transactional, String idempotencyKey, Instant createdAt) {
        jdbc.update("""
                INSERT INTO campaigns
                    (id, tenant_id, name, channel, template, status, scheduled_at,
                     transactional, idempotency_key, created_at)
                VALUES (?, ?, ?, ?, ?, 'SCHEDULED', ?, ?, ?, ?)
                """,
                id, tenant.id(), name, channel.name(), template, Timestamp.from(scheduledAt),
                transactional, idempotencyKey, Timestamp.from(createdAt));
    }

    public Optional<CampaignSummary> findByIdempotencyKey(String tenantId, String key) {
        return jdbc.query("""
                SELECT c.id, c.status,
                       (SELECT COUNT(*) FROM recipients r WHERE r.campaign_id = c.id) AS total_recipients
                FROM campaigns c
                WHERE c.tenant_id = ? AND c.idempotency_key = ?
                """,
                (rs, rowNum) -> new CampaignSummary(
                        rs.getString("id"),
                        rs.getString("status"),
                        rs.getLong("total_recipients")),
                tenantId, key).stream().findFirst();
    }

    public void insertRecipients(List<Object[]> rows) {
        jdbc.batchUpdate("""
                INSERT INTO recipients
                    (tenant_id, campaign_id, recipient_ref, email, phone, timezone, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, rows);
    }

    public void insertJobs(List<Object[]> rows) {
        jdbc.batchUpdate("""
                INSERT INTO notification_jobs
                    (tenant_id, campaign_id, recipient_ref, channel, status, attempts,
                     next_attempt_at, dedupe_key, created_at)
                VALUES (?, ?, ?, ?, 'PENDING', 0, ?, ?, ?)
                """, rows);
    }

    public void promoteDueCampaigns(Instant now) {
        jdbc.update("""
                UPDATE campaigns
                SET status = 'RUNNING', started_at = ?
                WHERE status = 'SCHEDULED' AND scheduled_at <= ?
                """, Timestamp.from(now), Timestamp.from(now));
    }

    public List<Job> claimDueJobs(int limit, Instant now) {
        List<Job> jobs = jdbc.query("""
                SELECT j.id, j.tenant_id, j.campaign_id, j.recipient_ref, j.channel, j.attempts,
                       j.dedupe_key, COALESCE(r.timezone, t.timezone) AS timezone, c.transactional
                FROM notification_jobs j
                JOIN recipients r ON r.campaign_id = j.campaign_id AND r.recipient_ref = j.recipient_ref
                JOIN campaigns c ON c.id = j.campaign_id
                JOIN tenants t ON t.id = j.tenant_id
                WHERE j.status = 'PENDING' AND j.next_attempt_at <= ?
                ORDER BY j.next_attempt_at
                LIMIT ?
                """,
                (rs, rowNum) -> new Job(
                        rs.getLong("id"),
                        rs.getString("tenant_id"),
                        rs.getString("campaign_id"),
                        rs.getString("recipient_ref"),
                        Channel.valueOf(rs.getString("channel")),
                        rs.getInt("attempts"),
                        rs.getString("dedupe_key"),
                        rs.getString("timezone"),
                        rs.getBoolean("transactional")),
                Timestamp.from(now), limit);

        for (Job job : jobs) {
            jdbc.update("UPDATE notification_jobs SET status = 'PROCESSING' WHERE id = ? AND status = 'PENDING'",
                    job.id());
        }
        return jobs;
    }

    public void insertAttempt(long jobId, int attempt, ProviderSimulator.Result result) {
        jdbc.update("""
                INSERT INTO delivery_attempts
                    (job_id, attempt_no, status, error, latency_ms, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                jobId, attempt, result.success() ? "SENT" : "FAILED", result.error(),
                result.latencyMs(), Timestamp.from(Instant.now()));
    }

    public void markSent(long jobId, int attempt) {
        jdbc.update("""
                UPDATE notification_jobs
                SET status = 'SENT', attempts = ?, sent_at = ?, last_error = NULL
                WHERE id = ?
                """, attempt, Timestamp.from(Instant.now()), jobId);
    }

    public void markRetry(long jobId, int attempt, Instant nextAttemptAt, String error) {
        jdbc.update("""
                UPDATE notification_jobs
                SET status = 'PENDING', attempts = ?, next_attempt_at = ?, last_error = ?
                WHERE id = ?
                """, attempt, Timestamp.from(nextAttemptAt), error, jobId);
    }

    public void markFailed(long jobId, int attempt, String error) {
        jdbc.update("""
                UPDATE notification_jobs
                SET status = 'FAILED', attempts = ?, last_error = ?
                WHERE id = ?
                """, attempt, error, jobId);
    }

    public void markSkipped(long jobId, String reason) {
        jdbc.update("""
                UPDATE notification_jobs
                SET status = 'SKIPPED', last_error = ?
                WHERE id = ?
                """, reason, jobId);
    }

    public long countJobsThisMonth(String tenantId, Instant from, Instant to) {
        Long count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM notification_jobs
                WHERE tenant_id = ? AND created_at >= ? AND created_at < ?
                """, Long.class, tenantId, Timestamp.from(from), Timestamp.from(to));
        return count == null ? 0 : count;
    }

    public boolean refreshCampaignStatus(String campaignId, Instant now) {
        int updated = jdbc.update("""
                UPDATE campaigns c
                SET status = CASE
                        WHEN EXISTS (SELECT 1 FROM notification_jobs j
                                     WHERE j.campaign_id = c.id AND j.status IN ('SENT', 'SKIPPED'))
                        THEN 'COMPLETED' ELSE 'FAILED' END,
                    completed_at = ?
                WHERE c.id = ?
                  AND c.status = 'RUNNING'
                  AND NOT EXISTS (SELECT 1 FROM notification_jobs j
                                  WHERE j.campaign_id = c.id AND j.status IN ('PENDING', 'PROCESSING'))
                """, Timestamp.from(now), campaignId);
        return updated > 0;
    }
}
