package com.relay.api.tenant;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SessionController {

    private final TenantRepository tenants;
    private final boolean demo;

    public SessionController(TenantRepository tenants, @Value("${relay.demo:true}") boolean demo) {
        this.tenants = tenants;
        this.demo = demo;
    }

    @GetMapping("/session")
    public Map<String, Object> session() {
        var views = tenants.findAll().stream()
                .map(tenant -> new TenantView(
                        tenant.id(),
                        tenant.name(),
                        tenant.timezone(),
                        demo ? tenant.token() : null))
                .toList();
        return Map.of("tenants", views);
    }

    public record TenantView(String id, String name, String timezone, String token) {
    }
}
