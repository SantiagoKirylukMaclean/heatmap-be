package com.puetsnao.heatmap.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rate-limit")
public record RateLimitProperties(
        Boolean enabled,
        Integer capacity,
        Integer refillTokens,
        Integer refillPeriodSeconds
) {
    public RateLimitProperties {
        if (enabled == null) enabled = false;
        if (capacity == null) capacity = 100;
        if (refillTokens == null) refillTokens = 50;
        if (refillPeriodSeconds == null) refillPeriodSeconds = 60;
    }
}
