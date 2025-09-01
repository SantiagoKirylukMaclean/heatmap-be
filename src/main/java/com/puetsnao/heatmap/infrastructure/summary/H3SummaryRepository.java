package com.puetsnao.heatmap.infrastructure.summary;

import com.puetsnao.heatmap.domain.Metric;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

public interface H3SummaryRepository {
    Map<String, Double> byDay(LocalDate bucketDate, int resolution, Metric metric);
    Map<String, Double> byHour(LocalDateTime bucketHour, int resolution, Metric metric);
}
