package com.puetsnao.heatmap.application;

import com.puetsnao.heatmap.domain.H3CellPoint;
import com.puetsnao.heatmap.domain.Metric;
import com.puetsnao.heatmap.infrastructure.summary.H3SummaryRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class DefaultH3HeatmapService implements H3HeatmapService {

    private final H3SummaryRepository repository;

    public DefaultH3HeatmapService(H3SummaryRepository repository) {
        this.repository = repository;
    }

    @Override
    @Cacheable(cacheNames = "heatmap", key = "'heatmap:h3:day:' + #metric.name().toLowerCase() + ':' + #resolution + ':' + #day")
    public List<H3CellPoint> byDay(Metric metric, int resolution, LocalDate day) {
        Map<String, Double> values = repository.byDay(day, resolution, metric);
        return values.entrySet().stream()
                .map(e -> new H3CellPoint(e.getKey(), resolution, e.getValue()))
                .sorted(Comparator.comparing(H3CellPoint::cell))
                .toList();
    }

    @Override
    @Cacheable(cacheNames = "heatmap", key = "'heatmap:h3:hour:' + #metric.name().toLowerCase() + ':' + #resolution + ':' + #hour")
    public List<H3CellPoint> byHour(Metric metric, int resolution, LocalDateTime hour) {
        Map<String, Double> values = repository.byHour(hour, resolution, metric);
        return values.entrySet().stream()
                .map(e -> new H3CellPoint(e.getKey(), resolution, e.getValue()))
                .sorted(Comparator.comparing(H3CellPoint::cell))
                .toList();
    }
}
