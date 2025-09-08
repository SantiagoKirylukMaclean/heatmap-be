package com.puetsnao.heatmap.infrastructure.summary;

import com.puetsnao.heatmap.domain.Metric;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

public interface H3SummaryRepository {
    Map<String, Double> byDay(LocalDate bucketDate, int resolution, Metric metric);
    Map<String, Double> byHour(LocalDateTime bucketHour, int resolution, Metric metric);

    // Base resolution (10) inputs for v2 aggregation
    Map<String, PriceInputs> h10PriceByDay(LocalDate bucketDate);
    Map<String, PriceInputs> h10PriceByHour(LocalDateTime bucketHour);
    Map<String, Double> h10VolumeByDay(LocalDate bucketDate);
    Map<String, Double> h10VolumeByHour(LocalDateTime bucketHour);
}
