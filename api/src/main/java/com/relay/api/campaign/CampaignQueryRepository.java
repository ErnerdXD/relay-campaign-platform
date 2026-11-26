package com.relay.api.campaign;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class CampaignQueryRepository {

    private final JdbcTemplate jdbc;

    public CampaignQueryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public CampaignViews.Page list(String tenantId, String search, String status, String channel,
            int page, int pageSize) {
        StringBuilder where = new StringBuilder(" WHERE c.tenant_id = ?");
        List<Object> args = new ArrayList<>();
        args.add(tenantId);

        if (search != null && !search.isBlank()) {
            where.append(" AND (LOWER(c.name) LIKE ? OR c.id LIKE ?)");
            args.add("%" + search.toLowerCase(Locale.ROOT) + "%");
            args.add("%" + search + "%");
        }
        if (status != null && !status.isBlank()) {
            where.append(" AND c.status = ?");
            args.add(status);
        }
        if (channel != null && !channel.isBlank()) {
            where.append(" AND c.channel = ?");
            args.add(channel);
        }

        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM campaigns c" + where, Long.class, args.toArray());

        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(pageSize);
        pageArgs.add(page * pageSize);

        List<CampaignViews.Row> items = jdbc.query("""
                SELECT c.id, c.name, c.channel, c.status, c.scheduled_at, c.created_at,
                       (SELECT COUNT(*) FROM notification_jobs j WHERE j.campaign_id = c.id) AS total_recipients,
                       (SELECT COUNT(*) FROM notification_jobs j WHERE j.campaign_id = c.id AND j.status = 'SENT') AS sent,
                       (SELECT COUNT(*) FROM notification_jobs j WHERE j.campaign_id = c.id AND j.status = 'FAILED') AS failed,
                       (SELECT COUNT(*) FROM notification_jobs j WHERE j.campaign_id = c.id AND j.status = 'SKIPPED') AS skipped
                FROM campaigns c
                """ + where + " ORDER BY c.created_at DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> new CampaignViews.Row(
                        rs.getString("id"),
                        rs.getString("name"),
                        Channel.valueOf(rs.getString("channel")),
                        rs.getString("status"),
                        rs.getTimestamp("scheduled_at").toInstant(),
                        rs.getTimestamp("created_at").toInstant(),
                        rs.getLong("total_recipients"),
                        rs.getLong("sent"),
                        rs.getLong("failed"),
                        rs.getLong("skipped")),
                pageArgs.toArray());

        return new CampaignViews.Page(items, page, pageSize, total == null ? 0 : total);
    }

    public Optional<CampaignViews.Detail> detail(String tenantId, String campaignId) {
        List<CampaignViews.Detail> rows = jdbc.query("""
                SELECT c.id, c.name, c.channel, c.status, c.template, c.transactional,
                       c.scheduled_at, c.created_at, c.started_at, c.completed_at,
                       (SELECT COUNT(*) FROM notification_jobs j WHERE j.campaign_id = c.id) AS total_recipients,
                       (SELECT COUNT(*) FROM notification_jobs j WHERE j.campaign_id = c.id AND j.status = 'SENT') AS sent,
                       (SELECT COUNT(*) FROM notification_jobs j WHERE j.campaign_id = c.id AND j.status = 'FAILED') AS failed,
                       (SELECT COUNT(*) FROM notification_jobs j WHERE j.campaign_id = c.id AND j.status = 'SKIPPED') AS skipped,
                       (SELECT COUNT(*) FROM notification_jobs j WHERE j.campaign_id = c.id AND j.status IN ('PENDING', 'PROCESSING')) AS pending,
                       (SELECT COUNT(*) FROM notification_jobs j WHERE j.campaign_id = c.id AND j.status = 'PENDING' AND j.attempts > 0) AS retry_queue
                FROM campaigns c
                WHERE c.id = ? AND c.tenant_id = ?
                """,
                (rs, rowNum) -> new CampaignViews.Detail(
                        rs.getString("id"),
                        rs.getString("name"),
                        Channel.valueOf(rs.getString("channel")),
                        rs.getString("status"),
                        rs.getString("template"),
                        rs.getBoolean("transactional"),
                        rs.getTimestamp("scheduled_at").toInstant(),
                        rs.getTimestamp("created_at").toInstant(),
                        instant(rs.getTimestamp("started_at")),
                        instant(rs.getTimestamp("completed_at")),
                        rs.getLong("total_recipients"),
                        rs.getLong("sent"),
                        rs.getLong("failed"),
                        rs.getLong("skipped"),
                        rs.getLong("pending"),
                        rs.getLong("retry_queue"),
                        metrics(campaignId),
                        errors(campaignId)),
                campaignId, tenantId);

        return rows.stream().findFirst();
    }

    public boolean exists(String tenantId, String campaignId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM campaigns WHERE id = ? AND tenant_id = ?",
                Integer.class, campaignId, tenantId);
        return count != null && count > 0;
    }

    @Transactional
    public int retryFailures(String tenantId, String campaignId, Instant now) {
        int requeued = jdbc.update("""
                UPDATE notification_jobs
                SET status = 'PENDING', attempts = 0, next_attempt_at = ?, last_error = NULL
                WHERE campaign_id = ? AND tenant_id = ? AND status = 'FAILED'
                """, Timestamp.from(now), campaignId, tenantId);

        if (requeued > 0) {
            jdbc.update("""
                    UPDATE campaigns
                    SET status = 'RUNNING', completed_at = NULL
                    WHERE id = ? AND tenant_id = ?
                    """, campaignId, tenantId);
        }
        return requeued;
    }

    private List<CampaignViews.Minute> metrics(String campaignId) {
        return jdbc.query("""
                SELECT DATE_TRUNC('MINUTE', a.created_at) AS bucket,
                       SUM(CASE WHEN a.status = 'SENT' THEN 1 ELSE 0 END) AS sent,
                       SUM(CASE WHEN a.status = 'FAILED' THEN 1 ELSE 0 END) AS failed
                FROM delivery_attempts a
                JOIN notification_jobs j ON j.id = a.job_id
                WHERE j.campaign_id = ? AND a.created_at >= ?
                GROUP BY DATE_TRUNC('MINUTE', a.created_at)
                ORDER BY 1
                """,
                (rs, rowNum) -> new CampaignViews.Minute(
                        rs.getTimestamp("bucket").toInstant(),
                        rs.getLong("sent"),
                        rs.getLong("failed")),
                campaignId, Timestamp.from(Instant.now().minus(1, ChronoUnit.HOURS)));
    }

    private List<CampaignViews.Error> errors(String campaignId) {
        return jdbc.query("""
                SELECT COALESCE(last_error, 'unknown') AS error, COUNT(*) AS count
                FROM notification_jobs
                WHERE campaign_id = ? AND status = 'FAILED'
                GROUP BY COALESCE(last_error, 'unknown')
                ORDER BY count DESC
                """,
                (rs, rowNum) -> new CampaignViews.Error(rs.getString("error"), rs.getLong("count")),
                campaignId);
    }

    private static Instant instant(Timestamp value) {
        return value == null ? null : value.toInstant();
    }
}
