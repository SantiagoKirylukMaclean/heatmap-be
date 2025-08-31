package com.puetsnao.heatmap.application;

import com.puetsnao.heatmap.domain.HeatPoint;
import com.puetsnao.heatmap.domain.Metric;
import com.puetsnao.heatmap.domain.Period;
import com.puetsnao.price.infrastructure.PriceEntity;
import com.puetsnao.price.infrastructure.PriceRepository;
import com.puetsnao.sales.infrastructure.SaleEntity;
import com.puetsnao.sales.infrastructure.SaleRepository;
import com.puetsnao.station.infrastructure.StationEntity;
import com.puetsnao.station.infrastructure.StationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DefaultHeatmapService implements HeatmapService {

    private final StationRepository stationRepository;
    private final PriceRepository priceRepository;
    private final SaleRepository saleRepository;

    public DefaultHeatmapService(StationRepository stationRepository,
                                 PriceRepository priceRepository,
                                 SaleRepository saleRepository) {
        this.stationRepository = stationRepository;
        this.priceRepository = priceRepository;
        this.saleRepository = saleRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<HeatPoint> heatmap(Metric metric, Period period) {
        LocalDateTime to = LocalDateTime.now();
        LocalDateTime from = switch (period) {
            case LAST30D -> to.minusDays(30);
        };

        Map<String, StateCentroid> centroids = computeStateCentroids();

        return switch (metric) {
            case PRICE -> priceAverageByState(from, to).entrySet().stream()
                    .map(e -> toHeatPoint(e.getKey(), centroids.get(e.getKey()), e.getValue()))
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(HeatPoint::state))
                    .toList();
            case VOLUME -> volumeSumByState(from, to).entrySet().stream()
                    .map(e -> toHeatPoint(e.getKey(), centroids.get(e.getKey()), e.getValue()))
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(HeatPoint::state))
                    .toList();
        };
    }

    private HeatPoint toHeatPoint(String state, StateCentroid centroid, double value) {
        if (centroid == null) return null;
        return new HeatPoint(state, centroid.lat(), centroid.lon(), value);
    }

    private Map<String, StateCentroid> computeStateCentroids() {
        return stationRepository.findAll().stream()
                .collect(Collectors.groupingBy(StationEntity::getState))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> centroid(e.getValue())));
    }

    private StateCentroid centroid(List<StationEntity> stations) {
        double avgLat = stations.stream()
                .map(StationEntity::getLatitude)
                .map(BigDecimal::doubleValue)
                .mapToDouble(Double::doubleValue)
                .average().orElse(0.0);
        double avgLon = stations.stream()
                .map(StationEntity::getLongitude)
                .map(BigDecimal::doubleValue)
                .mapToDouble(Double::doubleValue)
                .average().orElse(0.0);
        return new StateCentroid(avgLat, avgLon);
    }

    private Map<String, Double> priceAverageByState(LocalDateTime from, LocalDateTime to) {
        record Acc(double sum, long count) {}
        Map<String, Acc> acc = priceRepository.findAll().stream()
                .filter(p -> !p.getEffectiveAt().isBefore(from) && !p.getEffectiveAt().isAfter(to))
                .collect(Collectors.groupingBy(p -> p.getStation().getState(), Collectors.reducing(
                        new Acc(0, 0),
                        p -> new Acc(p.getAmount().doubleValue(), 1),
                        (a, b) -> new Acc(a.sum + b.sum, a.count + b.count)
                )));
        return acc.entrySet().stream()
                .filter(e -> e.getValue().count > 0)
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().sum / e.getValue().count));
    }

    private Map<String, Double> volumeSumByState(LocalDateTime from, LocalDateTime to) {
        return saleRepository.findAll().stream()
                .filter(s -> !s.getSoldAt().isBefore(from) && !s.getSoldAt().isAfter(to))
                .collect(Collectors.groupingBy(s -> s.getStation().getState(), Collectors.summingDouble(
                        s -> s.getVolume().doubleValue()
                )));
    }

    private record StateCentroid(double lat, double lon) {}
}
