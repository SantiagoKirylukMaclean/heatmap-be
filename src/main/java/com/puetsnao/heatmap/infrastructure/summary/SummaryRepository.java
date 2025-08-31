package com.puetsnao.heatmap.infrastructure.summary;

import java.time.LocalDate;
import java.util.Map;

public interface SummaryRepository {
    Map<String, Double> averagePriceByState(LocalDate fromDate, LocalDate toDate);
    Map<String, Double> totalVolumeByState(LocalDate fromDate, LocalDate toDate);
}