package com.puetsnao.heatmap.domain;

public enum Metric {
    PRICE,
    VOLUME;

    public static Metric from(String raw) {
        return Metric.valueOf(raw.trim().toUpperCase());
    }
}
