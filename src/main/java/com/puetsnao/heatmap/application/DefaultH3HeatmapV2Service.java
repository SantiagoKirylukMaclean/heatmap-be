package com.puetsnao.heatmap.application;

import com.puetsnao.heatmap.domain.BucketGranularity;
import com.puetsnao.heatmap.domain.Metric;
import com.puetsnao.heatmap.infrastructure.summary.H3SummaryRepository;
import com.puetsnao.heatmap.infrastructure.summary.PriceInputs;
import com.uber.h3core.H3Core;
import com.uber.h3core.util.GeoCoord;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class DefaultH3HeatmapV2Service implements H3HeatmapV2Service {

    private final H3SummaryRepository repository;
    private final H3Core h3;

    public DefaultH3HeatmapV2Service(H3SummaryRepository repository) {
        this.repository = repository;
        try {
            this.h3 = H3Core.newInstance();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot initialize H3Core", e);
        }
    }

    @Override
    public List<Object[]> queryPairs(Metric metric, int resolution, BucketGranularity bucket, String at, String bbox) {
        double[] bounds = parseBbox(bbox);
        return switch (bucket) {
            case DAY -> byDay(metric, resolution, at == null || at.isBlank() ? LocalDate.now() : LocalDate.parse(at), bounds);
            case HOUR -> byHour(metric, resolution, parseHourOrDefault(at), bounds);
        };
    }

    private List<Object[]> byDay(Metric metric, int resolution, LocalDate day, double[] bounds) {
        if (metric == Metric.PRICE) {
            Map<String, PriceInputs> base = repository.h10PriceByDay(day);
            return aggregatePrice(base, resolution, bounds);
        } else {
            Map<String, Double> base = repository.h10VolumeByDay(day);
            return aggregateVolume(base, resolution, bounds);
        }
    }

    private List<Object[]> byHour(Metric metric, int resolution, LocalDateTime hour, double[] bounds) {
        if (metric == Metric.PRICE) {
            Map<String, PriceInputs> base = repository.h10PriceByHour(hour);
            return aggregatePrice(base, resolution, bounds);
        } else {
            Map<String, Double> base = repository.h10VolumeByHour(hour);
            return aggregateVolume(base, resolution, bounds);
        }
    }

    private List<Object[]> aggregatePrice(Map<String, PriceInputs> h10, int resolution, double[] bbox) {
        Map<String, PriceInputs> agg = new HashMap<>();
        for (Map.Entry<String, PriceInputs> e : h10.entrySet()) {
            String cell = e.getKey();
            if (!inBbox(cell, bbox)) continue;
            String parent = resolution >= 10 ? cell : h3.h3ToParentAddress(cell, resolution);
            agg.merge(parent, e.getValue(), PriceInputs::add);
        }
        return agg.entrySet().stream()
                .filter(en -> en.getValue().priceCount() > 0)
                .map(en -> new Object[]{en.getKey(), en.getValue().priceSum() / en.getValue().priceCount()})
                .sorted(Comparator.comparing(o -> (String) o[0]))
                .toList();
    }

    private List<Object[]> aggregateVolume(Map<String, Double> h10, int resolution, double[] bbox) {
        Map<String, Double> agg = new HashMap<>();
        for (Map.Entry<String, Double> e : h10.entrySet()) {
            String cell = e.getKey();
            if (!inBbox(cell, bbox)) continue;
            String parent = resolution >= 10 ? cell : h3.h3ToParentAddress(cell, resolution);
            agg.merge(parent, e.getValue(), Double::sum);
        }
        return agg.entrySet().stream()
                .map(en -> new Object[]{en.getKey(), en.getValue()})
                .sorted(Comparator.comparing(o -> (String) o[0]))
                .toList();
    }

    private boolean inBbox(String h3Cell, double[] bbox) {
        GeoCoord coord = h3.h3ToGeo(h3Cell);
        double lat = coord.lat;
        double lon = coord.lng;
        return lat >= bbox[0] && lat <= bbox[2] && lon >= bbox[1] && lon <= bbox[3];
    }

    private static double[] parseBbox(String bbox) {
        if (bbox == null || bbox.isBlank()) throw new IllegalArgumentException("bbox is required: minLat,minLon,maxLat,maxLon");
        String[] parts = bbox.split(",");
        if (parts.length != 4) throw new IllegalArgumentException("bbox format: minLat,minLon,maxLat,maxLon");
        double minLat = Double.parseDouble(parts[0]);
        double minLon = Double.parseDouble(parts[1]);
        double maxLat = Double.parseDouble(parts[2]);
        double maxLon = Double.parseDouble(parts[3]);
        if (minLat > maxLat || minLon > maxLon) throw new IllegalArgumentException("bbox bounds invalid");
        return new double[]{minLat, minLon, maxLat, maxLon};
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
