package com.puetsnao.heatmap.infrastructure.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "heatmap.cache")
public record HeatmapCacheProperties(int ttlSeconds) {
}