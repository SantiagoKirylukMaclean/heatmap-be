package com.puetsnao.shared.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "heatmap.cache")
public record HeatmapCacheProperties(int ttlSeconds) {
}