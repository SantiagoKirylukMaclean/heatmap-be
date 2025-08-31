package com.puetsnao.heatmap.application;

import com.puetsnao.heatmap.domain.HeatPoint;
import com.puetsnao.heatmap.domain.Metric;
import com.puetsnao.heatmap.domain.Period;
import com.puetsnao.heatmap.infrastructure.summary.SummaryRepository;
import com.puetsnao.price.infrastructure.PriceRepository;
import com.puetsnao.sales.infrastructure.SaleRepository;
import com.puetsnao.station.infrastructure.StationEntity;
import com.puetsnao.station.infrastructure.StationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DefaultHeatmapService implements HeatmapService {

    private final StationRepository stationRepository;
    private final PriceRepository priceRepository;
    private final SaleRepository saleRepository;
    private final SummaryRepository summaryRepository; // optional for tests

    // Constructor used in unit tests (fallback to in-memory aggregation)
    public DefaultHeatmapService(StationRepository stationRepository,
                                 PriceRepository priceRepository,
                                 SaleRepository saleRepository) {
        this.stationRepository = stationRepository;
        this.priceRepository = priceRepository;
        this.saleRepository = saleRepository;
        this.summaryRepository = null;
    }

    // Preferred constructor for runtime: use SQL summary repository
    @Autowired
    public DefaultHeatmapService(StationRepository stationRepository,
                                 PriceRepository priceRepository,
                                 SaleRepository saleRepository,
                                 SummaryRepository summaryRepository) {
        this.stationRepository = stationRepository;
        this.priceRepository = priceRepository;
        this.saleRepository = saleRepository;
        this.summaryRepository = summaryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    @org.springframework.cache.annotation.Cacheable(cacheNames = "heatmap", key = "'heatmap:v2:' + #metric.name().toLowerCase() + ':' + #period.name().toLowerCase()")
    public List<HeatPoint> heatmap(Metric metric, Period period) {
        LocalDateTime toTs = LocalDateTime.now();
        LocalDateTime fromTs = switch (period) { case LAST30D -> toTs.minusDays(30); };
        LocalDate fromDate = fromTs.toLocalDate();
        LocalDate toDate = toTs.toLocalDate();

        Map<String, StateCentroid> centroids = computeStateCentroids();

        Map<String, Double> aggregated = switch (metric) {
            case PRICE -> readAveragePriceByState(fromDate, toDate, fromTs, toTs);
            case VOLUME -> readTotalVolumeByState(fromDate, toDate, fromTs, toTs);
        };

        return aggregated.entrySet().stream()
                .map(e -> toHeatPoint(e.getKey(), centroids.get(e.getKey()), e.getValue()))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(HeatPoint::state))
                .toList();
    }

    private Map<String, Double> readAveragePriceByState(LocalDate fromDate, LocalDate toDate,
                                                        LocalDateTime fromTs, LocalDateTime toTs) {
        if (summaryRepository != null) {
            return summaryRepository.averagePriceByState(fromDate, toDate);
        }
        return priceAverageByStateInMemory(fromTs, toTs);
    }

    private Map<String, Double> readTotalVolumeByState(LocalDate fromDate, LocalDate toDate,
                                                       LocalDateTime fromTs, LocalDateTime toTs) {
        if (summaryRepository != null) {
            return summaryRepository.totalVolumeByState(fromDate, toDate);
        }
        return volumeSumByStateInMemory(fromTs, toTs);
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

    private Map<String, Double> priceAverageByStateInMemory(LocalDateTime from, LocalDateTime to) {
        record Acc(double sum, long count) {}
        Map<String, Acc> acc = priceRepository.findAll().stream()
                .filter(p -> !p.getEffectiveAt().isBefore(from) && !p.getEffectiveAt().isAfter(to))
                .collect(Collectors.groupingBy(p -> p.getStation().getState(), java.util.stream.Collectors.reducing(
                        new Acc(0, 0),
                        p -> new Acc(p.getAmount().doubleValue(), 1),
                        (a, b) -> new Acc(a.sum + b.sum, a.count + b.count)
                )));
        return acc.entrySet().stream()
                .filter(e -> e.getValue().count > 0)
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().sum / e.getValue().count));
    }

    private Map<String, Double> volumeSumByStateInMemory(LocalDateTime from, LocalDateTime to) {
        return saleRepository.findAll().stream()
                .filter(s -> !s.getSoldAt().isBefore(from) && !s.getSoldAt().isAfter(to))
                .collect(Collectors.groupingBy(s -> s.getStation().getState(), java.util.stream.Collectors.summingDouble(
                        s -> s.getVolume().doubleValue()
                )));
    }

    private record StateCentroid(double lat, double lon) {}
}
