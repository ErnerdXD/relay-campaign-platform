package com.relay.api.campaign;

import com.relay.api.common.ApiException;
import com.relay.api.tenant.Tenant;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/campaigns")
public class CampaignController {

    private static final Set<String> STATUSES = Set.of("SCHEDULED", "RUNNING", "COMPLETED", "FAILED");

    private final CampaignService campaigns;
    private final CampaignQueryRepository queries;

    public CampaignController(CampaignService campaigns, CampaignQueryRepository queries) {
        this.campaigns = campaigns;
        this.queries = queries;
    }

    @GetMapping
    public CampaignViews.Page list(
            @RequestAttribute("tenant") Tenant tenant,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String channel,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {

        if (page < 0 || pageSize < 1 || pageSize > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "page must be >= 0 and pageSize between 1 and 100.");
        }

        String statusFilter = blankToNull(status);
        if (statusFilter != null) {
            statusFilter = statusFilter.toUpperCase();
            if (!STATUSES.contains(statusFilter)) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "status must be SCHEDULED, RUNNING, COMPLETED, or FAILED.");
            }
        }

        String channelFilter = blankToNull(channel);
        if (channelFilter != null) {
            channelFilter = CampaignService.parseChannel(channelFilter).name();
        }

        return queries.list(tenant.id(), blankToNull(search), statusFilter, channelFilter, page, pageSize);
    }

    @GetMapping("/{id}")
    public CampaignViews.Detail detail(
            @RequestAttribute("tenant") Tenant tenant,
            @PathVariable String id) {
        return queries.detail(tenant.id(), id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Campaign not found."));
    }

    @PostMapping("/{id}/retry-failures")
    public ResponseEntity<Map<String, Object>> retryFailures(
            @RequestAttribute("tenant") Tenant tenant,
            @PathVariable String id) {
        if (!queries.exists(tenant.id(), id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Campaign not found.");
        }
        int requeued = queries.retryFailures(tenant.id(), id, Instant.now());
        return ResponseEntity.accepted().body(Map.of("requeued", requeued));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CampaignRepository.CampaignSummary> create(
            @RequestAttribute("tenant") Tenant tenant,
            @RequestParam String tenantId,
            @RequestParam String name,
            @RequestParam String channel,
            @RequestParam String template,
            @RequestParam String scheduledAt,
            @RequestParam(defaultValue = "false") boolean transactional,
            @RequestPart("csv") MultipartFile csv,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        if (!tenant.id().equals(tenantId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "tenantId does not match the bearer token.");
        }
        if (name.isBlank() || name.length() > 160) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "name is required and must be at most 160 characters.");
        }
        if (template.isBlank() || template.length() > 2000) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "template is required and must be at most 2000 characters.");
        }
        if (csv == null || csv.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "csv upload is required.");
        }

        var summary = campaigns.create(
                tenant,
                name.trim(),
                CampaignService.parseChannel(channel),
                template,
                parseScheduledAt(scheduledAt),
                transactional,
                csv,
                idempotencyKey);

        return ResponseEntity.accepted().body(summary);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static Instant parseScheduledAt(String value) {
        if ("now".equalsIgnoreCase(value)) {
            return Instant.now();
        }
        try {
            return OffsetDateTime.parse(value).toInstant();
        } catch (DateTimeParseException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "scheduledAt must be 'now' or an ISO-8601 timestamp with an offset.");
        }
    }
}
