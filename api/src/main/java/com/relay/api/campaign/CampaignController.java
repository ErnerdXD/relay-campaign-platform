package com.relay.api.campaign;

import com.relay.api.common.ApiException;
import com.relay.api.tenant.Tenant;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    private final CampaignService campaigns;

    public CampaignController(CampaignService campaigns) {
        this.campaigns = campaigns;
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
