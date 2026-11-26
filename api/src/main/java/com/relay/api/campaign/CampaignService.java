package com.relay.api.campaign;

import com.relay.api.common.ApiException;
import com.relay.api.tenant.Tenant;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CampaignService {

    private static final int BATCH_SIZE = 500;

    private final CampaignRepository campaigns;
    private final CsvRecipientParser parser;

    public CampaignService(CampaignRepository campaigns, CsvRecipientParser parser) {
        this.campaigns = campaigns;
        this.parser = parser;
    }

    @Transactional
    public CampaignRepository.CampaignSummary create(Tenant tenant, String name, Channel channel,
            String template, Instant scheduledAt, boolean transactional, MultipartFile csv,
            String idempotencyKey) {

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = campaigns.findByIdempotencyKey(tenant.id(), idempotencyKey);
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        ZonedDateTime monthStart = Instant.now().atZone(ZoneOffset.UTC)
                .withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
        long usedThisMonth = campaigns.countJobsThisMonth(
                tenant.id(), monthStart.toInstant(), monthStart.plusMonths(1).toInstant());
        if (usedThisMonth >= tenant.monthlyQuota()) {
            throw new ApiException(HttpStatus.PAYMENT_REQUIRED, "Monthly message quota exceeded.");
        }

        String id = UUID.randomUUID().toString();
        Instant now = Instant.now();
        campaigns.insertCampaign(id, tenant, name, channel, template, scheduledAt,
                transactional, blankToNull(idempotencyKey), now);

        List<Object[]> recipients = new ArrayList<>(BATCH_SIZE);
        List<Object[]> jobs = new ArrayList<>(BATCH_SIZE);
        long[] total = {0};

        try (InputStream input = csv.getInputStream()) {
            parser.parse(input, channel, row -> {
                recipients.add(new Object[] {
                        tenant.id(), id, row.recipientRef(), blankToNull(row.email()),
                        blankToNull(row.phone()), blankToNull(row.timezone()), Timestamp.from(now)
                });
                jobs.add(new Object[] {
                        tenant.id(), id, row.recipientRef(), channel.name(),
                        Timestamp.from(scheduledAt),
                        dedupeKey(tenant.id(), channel, row.recipientRef(), template),
                        Timestamp.from(now)
                });
                total[0]++;
                if (recipients.size() >= BATCH_SIZE) {
                    flush(recipients, jobs);
                }
            });
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not read the CSV upload.");
        }

        flush(recipients, jobs);

        if (usedThisMonth + total[0] > tenant.monthlyQuota()) {
            throw new ApiException(HttpStatus.PAYMENT_REQUIRED, "Monthly message quota exceeded.");
        }

        return new CampaignRepository.CampaignSummary(id, "SCHEDULED", total[0]);
    }

    private void flush(List<Object[]> recipients, List<Object[]> jobs) {
        if (recipients.isEmpty()) {
            return;
        }
        campaigns.insertRecipients(recipients);
        campaigns.insertJobs(jobs);
        recipients.clear();
        jobs.clear();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String dedupeKey(String tenantId, Channel channel, String recipientRef, String template) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(
                    (tenantId + '|' + channel + '|' + recipientRef + '|' + template)
                            .getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 32);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the JVM", exception);
        }
    }

    public static Channel parseChannel(String value) {
        try {
            return Channel.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "channel must be EMAIL, SMS, or PUSH.");
        }
    }
}
