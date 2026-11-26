package com.relay.api.campaign;

import jakarta.annotation.PreDestroy;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DeliveryWorker {

    private static final Logger log = LoggerFactory.getLogger(DeliveryWorker.class);
    private static final int MAX_ATTEMPTS = 3;

    private final CampaignRepository campaigns;
    private final RuleRepository rules;
    private final ProviderSimulator providers;
    private final ChannelRateLimiter limiter;
    private final Semaphore slots;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final AtomicBoolean ticking = new AtomicBoolean();
    private final Set<String> dirtyCampaigns = ConcurrentHashMap.newKeySet();

    public DeliveryWorker(
            CampaignRepository campaigns,
            RuleRepository rules,
            ProviderSimulator providers,
            ChannelRateLimiter limiter,
            @Value("${relay.worker.concurrency:16}") int concurrency) {
        this.campaigns = campaigns;
        this.rules = rules;
        this.providers = providers;
        this.limiter = limiter;
        this.slots = new Semaphore(concurrency);
    }

    @Scheduled(fixedDelayString = "${relay.worker.poll-ms:500}")
    public void tick() {
        if (!ticking.compareAndSet(false, true)) {
            return;
        }
        try {
            Instant now = Instant.now();
            campaigns.promoteDueCampaigns(now);

            int free = slots.availablePermits();
            if (free > 0) {
                List<Job> jobs = campaigns.claimDueJobs(Math.min(free, 25), now);
                for (Job job : jobs) {
                    slots.acquireUninterruptibly();
                    executor.submit(() -> {
                        try {
                            deliver(job);
                        } finally {
                            slots.release();
                        }
                    });
                }
            }
            refreshFinishedCampaigns();
        } finally {
            ticking.set(false);
        }
    }

    private void deliver(Job job) {
        Instant now = Instant.now();
        Rules.Decision decision = Rules.decide(
                job,
                rules.isSuppressed(job.tenantId(), job.recipientRef(), job.channel()),
                rules.isDuplicate(job.tenantId(), job.dedupeKey(), job.id(), now.minusSeconds(300)),
                now);

        if (decision.action() == Rules.Action.SKIP) {
            campaigns.markSkipped(job.id(), decision.reason());
            log.info("job.skipped tenantId={} campaignId={} notificationId={} reason={}",
                    job.tenantId(), job.campaignId(), job.id(), decision.reason());
            dirtyCampaigns.add(job.campaignId());
            return;
        }
        if (decision.action() == Rules.Action.DELAY) {
            campaigns.markRetry(job.id(), job.attempts(), decision.notBefore(), decision.reason());
            log.info("job.delayed tenantId={} campaignId={} notificationId={} reason={}",
                    job.tenantId(), job.campaignId(), job.id(), decision.reason());
            dirtyCampaigns.add(job.campaignId());
            return;
        }

        try {
            if (!limiter.acquire(job.channel(), 5_000)) {
                campaigns.markRetry(job.id(), job.attempts(), Instant.now().plusSeconds(1), "rate_limited");
                dirtyCampaigns.add(job.campaignId());
                return;
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            campaigns.markRetry(job.id(), job.attempts(), Instant.now().plusSeconds(1), "interrupted");
            dirtyCampaigns.add(job.campaignId());
            return;
        }

        ProviderSimulator.Result result = providers.send(job.channel());
        int attempt = job.attempts() + 1;

        campaigns.insertAttempt(job.id(), attempt, result);
        if (result.success()) {
            campaigns.markSent(job.id(), attempt);
        } else if (attempt >= MAX_ATTEMPTS) {
            campaigns.markFailed(job.id(), attempt, result.error());
        } else {
            campaigns.markRetry(job.id(), attempt, Instant.now().plus(Backoff.delay(attempt)), result.error());
        }
        log.info("delivery tenantId={} campaignId={} notificationId={} attempt={} status={} error={}",
                job.tenantId(), job.campaignId(), job.id(), attempt,
                result.success() ? "SENT" : attempt >= MAX_ATTEMPTS ? "FAILED" : "RETRY",
                result.error());
        dirtyCampaigns.add(job.campaignId());
    }

    private void refreshFinishedCampaigns() {
        for (String campaignId : List.copyOf(dirtyCampaigns)) {
            if (campaigns.refreshCampaignStatus(campaignId, Instant.now())) {
                dirtyCampaigns.remove(campaignId);
            }
        }
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }
}
