package com.puetsnao.heatmap.application;

import com.puetsnao.heatmap.domain.HeatPoint;
import com.puetsnao.heatmap.domain.Metric;
import com.puetsnao.heatmap.domain.Period;
import com.puetsnao.price.app.PriceQueryPort;
import com.puetsnao.sales.app.SalesQueryPort;
import com.puetsnao.station.app.StationQueryPort;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class DefaultHeatmapServiceTests {

    @Test
    void aggregatesPriceAverageAndVolumeSumByState() {
        StationQueryPort stationQuery = Mockito.mock(StationQueryPort.class);
        PriceQueryPort priceQuery = Mockito.mock(PriceQueryPort.class);
        SalesQueryPort salesQuery = Mockito.mock(SalesQueryPort.class);

        StationQueryPort.StationLocation stA1 = new StationQueryPort.StationLocation("STA", 10.0, 20.0);
        StationQueryPort.StationLocation stA2 = new StationQueryPort.StationLocation("STA", 12.0, 24.0);
        StationQueryPort.StationLocation stB1 = new StationQueryPort.StationLocation("STB", -5.0, 30.0);
        when(stationQuery.stations()).thenReturn(List.of(stA1, stA2, stB1));

        when(priceQuery.averagePriceByState(Mockito.any(), Mockito.any())).thenReturn(Map.of(
                "STA", (100.0 + 200.0) / 2.0,
                "STB", 300.0
        ));

        when(salesQuery.totalVolumeByState(Mockito.any(), Mockito.any())).thenReturn(Map.of(
                "STA", 25.0,
                "STB", 7.0
        ));

        DefaultHeatmapService service = new DefaultHeatmapService(stationQuery, priceQuery, salesQuery);

        var pricePoints = service.heatmap(Metric.PRICE, Period.LAST30D);
        var volumePoints = service.heatmap(Metric.VOLUME, Period.LAST30D);

        assertThat(pricePoints).hasSize(2);
        HeatPoint staPrice = pricePoints.stream().filter(p -> p.state().equals("STA")).findFirst().orElseThrow();
        HeatPoint stbPrice = pricePoints.stream().filter(p -> p.state().equals("STB")).findFirst().orElseThrow();
        assertThat(staPrice.value()).isEqualTo((100.0 + 200.0) / 2.0);
        assertThat(stbPrice.value()).isEqualTo(300.0);

        assertThat(volumePoints).hasSize(2);
        HeatPoint staVol = volumePoints.stream().filter(p -> p.state().equals("STA")).findFirst().orElseThrow();
        HeatPoint stbVol = volumePoints.stream().filter(p -> p.state().equals("STB")).findFirst().orElseThrow();
        assertThat(staVol.value()).isEqualTo(25.0);
        assertThat(stbVol.value()).isEqualTo(7.0);

        // centroid of state STA from (10,20) and (12,24)
        assertThat(staPrice.lat()).isEqualTo((10.0 + 12.0) / 2.0);
        assertThat(staPrice.lon()).isEqualTo((20.0 + 24.0) / 2.0);
    }
}
