package com.relay.api.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class TenantAuthFilter extends OncePerRequestFilter {

    private final TenantRepository tenants;

    public TenantAuthFilter(TenantRepository tenants) {
        this.tenants = tenants;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());

        if (path.equals("/health")
                || path.startsWith("/session")
                || path.startsWith("/actuator")
                || "OPTIONS".equals(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            unauthorized(response);
            return;
        }

        var tenant = tenants.findByToken(header.substring(7));
        if (tenant.isEmpty()) {
            unauthorized(response);
            return;
        }

        request.setAttribute("tenant", tenant.get());
        chain.doFilter(request, response);
    }

    private static void unauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/problem+json");
        response.getWriter().write(
                "{\"title\":\"Unauthorized\",\"status\":401,\"detail\":\"A valid tenant bearer token is required.\"}");
    }
}
