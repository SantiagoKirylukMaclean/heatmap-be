package com.puetsnao.heatmap.domain;

public enum Period {
    LAST30D;

    public static Period from(String raw) {
        String normalized = raw.trim().toUpperCase().replace("-", "");
        if (normalized.equals("LAST30D")) return LAST30D;
        return Period.valueOf(normalized);
    }
}
