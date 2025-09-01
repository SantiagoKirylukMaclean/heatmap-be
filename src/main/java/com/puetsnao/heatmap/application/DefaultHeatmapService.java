package com.puetsnao.heatmap.application;

import com.puetsnao.heatmap.domain.HeatPoint;
import com.puetsnao.heatmap.domain.Metric;
import com.puetsnao.heatmap.domain.Period;
import com.puetsnao.heatmap.infrastructure.summary.SummaryRepository;
import com.puetsnao.price.app.PriceQueryPort;
import com.puetsnao.sales.app.SalesQueryPort;
import com.puetsnao.station.app.StationQueryPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DefaultHeatmapService implements HeatmapService {

    private final StationQueryPort stationQuery;
    private final PriceQueryPort priceQuery;
    private final SalesQueryPort salesQuery;
    private final SummaryRepository summaryRepository; // optional for tests

    // Constructor used in unit tests (fallback to direct ports)
    public DefaultHeatmapService(StationQueryPort stationQuery,
                                 PriceQueryPort priceQuery,
                                 SalesQueryPort salesQuery) {
        this.stationQuery = stationQuery;
        this.priceQuery = priceQuery;
        this.salesQuery = salesQuery;
        this.summaryRepository = null;
    }

    // Preferred constructor for runtime: use SQL summary repository for aggregations
    @Autowired
    public DefaultHeatmapService(StationQueryPort stationQuery,
                                 PriceQueryPort priceQuery,
                                 SalesQueryPort salesQuery,
                                 SummaryRepository summaryRepository) {
        this.stationQuery = stationQuery;
        this.priceQuery = priceQuery;
        this.salesQuery = salesQuery;
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
            case PRICE -> readAveragePriceByState(fromDate, toDate);
            case VOLUME -> readTotalVolumeByState(fromDate, toDate);
        };

        return aggregated.entrySet().stream()
                .map(e -> toHeatPoint(e.getKey(), centroids.get(e.getKey()), e.getValue()))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(HeatPoint::state))
                .toList();
    }

    private Map<String, Double> readAveragePriceByState(LocalDate fromDate, LocalDate toDate) {
        if (summaryRepository != null) {
            return summaryRepository.averagePriceByState(fromDate, toDate);
        }
        return priceQuery.averagePriceByState(fromDate, toDate);
    }

    private Map<String, Double> readTotalVolumeByState(LocalDate fromDate, LocalDate toDate) {
        if (summaryRepository != null) {
            return summaryRepository.totalVolumeByState(fromDate, toDate);
        }
        return salesQuery.totalVolumeByState(fromDate, toDate);
    }

    private HeatPoint toHeatPoint(String state, StateCentroid centroid, double value) {
        if (centroid == null) return null;
        return new HeatPoint(state, centroid.lat(), centroid.lon(), value);
    }

    private Map<String, StateCentroid> computeStateCentroids() {
        return stationQuery.stations().stream()
                .collect(Collectors.groupingBy(StationQueryPort.StationLocation::state))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> centroid(e.getValue())));
    }

    private StateCentroid centroid(List<StationQueryPort.StationLocation> stations) {
        double avgLat = stations.stream()
                .mapToDouble(StationQueryPort.StationLocation::latitude)
                .average().orElse(0.0);
        double avgLon = stations.stream()
                .mapToDouble(StationQueryPort.StationLocation::longitude)
                .average().orElse(0.0);
        return new StateCentroid(avgLat, avgLon);
    }

    private record StateCentroid(double lat, double lon) {}
}
