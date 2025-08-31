package com.puetsnao.heatmap.infrastructure.batch;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "heatmap.summary-refresh")
public record SummaryRefreshProperties(
        Boolean enabled,
        Integer windowDays,
        Long fixedDelayMs,
        Long initialDelayMs
) {
    public SummaryRefreshProperties {
        if (enabled == null) enabled = false;
        if (windowDays == null) windowDays = 7;
        if (fixedDelayMs == null) fixedDelayMs = 600_000L;
        if (initialDelayMs == null) initialDelayMs = 10_000L;
    }
}