package com.relay.api.tenant;

public record Tenant(String id, String name, String token, String timezone, int monthlyQuota) {
}
