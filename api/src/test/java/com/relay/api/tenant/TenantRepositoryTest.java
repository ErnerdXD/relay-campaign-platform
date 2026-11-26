package com.relay.api.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class TenantRepositoryTest {

    @Autowired
    TenantRepository tenants;

    @Test
    void findsSeededTenantByToken() {
        var tenant = tenants.findByToken("demo-northstar-local");

        assertThat(tenant).isPresent();
        assertThat(tenant.get().name()).isEqualTo("Northstar Bank");
    }

    @Test
    void listsBothDemoTenants() {
        assertThat(tenants.findAll()).extracting(Tenant::id).containsExactly("meridian", "northstar");
    }
}
