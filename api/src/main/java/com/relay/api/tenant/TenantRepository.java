package com.relay.api.tenant;

import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class TenantRepository {

    private static final RowMapper<Tenant> MAPPER = (rs, rowNum) -> new Tenant(
            rs.getString("id"),
            rs.getString("name"),
            rs.getString("token"),
            rs.getString("timezone"),
            rs.getInt("monthly_quota"));

    private final JdbcTemplate jdbc;

    public TenantRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Tenant> findByToken(String token) {
        return jdbc.query("SELECT * FROM tenants WHERE token = ?", MAPPER, token).stream().findFirst();
    }

    public List<Tenant> findAll() {
        return jdbc.query("SELECT * FROM tenants ORDER BY name", MAPPER);
    }
}
