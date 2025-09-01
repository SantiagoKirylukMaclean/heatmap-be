package com.puetsnao.heatmap.application;

import com.puetsnao.heatmap.domain.BucketGranularity;
import com.puetsnao.heatmap.domain.H3CellPoint;
import com.puetsnao.heatmap.domain.Metric;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface H3HeatmapService {
    List<H3CellPoint> byDay(Metric metric, int resolution, LocalDate day);
    List<H3CellPoint> byHour(Metric metric, int resolution, LocalDateTime hour);

    default List<H3CellPoint> query(Metric metric, int resolution, BucketGranularity bucket, String at) {
        return switch (bucket) {
            case DAY -> byDay(metric, resolution, (at == null || at.isBlank()) ? LocalDate.now() : LocalDate.parse(at));
            case HOUR -> byHour(metric, resolution, parseHourOrDefault(at));
        };
    }

    private static LocalDateTime parseHourOrDefault(String at) {
        if (at == null || at.isBlank()) {
            return LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);
        }
        try {
            return LocalDateTime.parse(at);
        } catch (Exception ignored) {
        }
        try {
            return LocalDate.parse(at).atStartOfDay();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid 'at' value for hour bucket. Expected 'yyyy-MM-dd' or 'yyyy-MM-ddTHH[:mm[:ss]]'", e);
        }
    }
}
