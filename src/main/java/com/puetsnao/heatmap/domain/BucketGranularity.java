package com.puetsnao.heatmap.domain;

public enum BucketGranularity {
    DAY,
    HOUR;

    public static BucketGranularity from(String raw) {
        return BucketGranularity.valueOf(raw.trim().toUpperCase());
    }
}
