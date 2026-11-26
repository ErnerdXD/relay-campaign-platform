package com.relay.api.campaign;

import com.relay.api.common.ApiException;
import com.relay.api.tenant.Tenant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/provider")
public class ProviderController {

    private final DeliveryWorker worker;

    public ProviderController(DeliveryWorker worker) {
        this.worker = worker;
    }

    @PostMapping("/{channel}/send")
    public Map<String, Object> send(
            @RequestAttribute("tenant") Tenant tenant,
            @PathVariable String channel,
            @RequestBody Map<String, Long> body) {

        Long notificationId = body.get("notificationId");
        if (notificationId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "notificationId is required.");
        }

        return worker.sendNow(notificationId, tenant.id(), CampaignService.parseChannel(channel));
    }
}
