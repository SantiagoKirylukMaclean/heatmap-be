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
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class DefaultHeatmapServiceTests {

    @Test
    void aggregatesPriceAverageAndVolumeSumByState() {
        StationRepository stationRepository = Mockito.mock(StationRepository.class);
        PriceRepository priceRepository = Mockito.mock(PriceRepository.class);
        SaleRepository saleRepository = Mockito.mock(SaleRepository.class);

        StationEntity stA1 = station("A1", "STA", 10.0, 20.0);
        StationEntity stA2 = station("A2", "STA", 12.0, 24.0);
        StationEntity stB1 = station("B1", "STB", -5.0, 30.0);
        when(stationRepository.findAll()).thenReturn(List.of(stA1, stA2, stB1));

        PriceEntity p1 = price(stA1, 100.0, LocalDateTime.now().minusDays(5));
        PriceEntity p2 = price(stA2, 200.0, LocalDateTime.now().minusDays(3));
        PriceEntity p3 = price(stB1, 300.0, LocalDateTime.now().minusDays(2));
        when(priceRepository.findAll()).thenReturn(List.of(p1, p2, p3));

        SaleEntity s1 = sale(stA1, 10.0, LocalDateTime.now().minusDays(1));
        SaleEntity s2 = sale(stA2, 15.0, LocalDateTime.now().minusDays(1));
        SaleEntity s3 = sale(stB1, 7.0, LocalDateTime.now().minusDays(1));
        when(saleRepository.findAll()).thenReturn(List.of(s1, s2, s3));

        DefaultHeatmapService service = new DefaultHeatmapService(stationRepository, priceRepository, saleRepository);

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

    private StationEntity station(String code, String state, double lat, double lon) {
        StationEntity s = new StationEntity();
        s.setCode(code);
        s.setName(code);
        s.setState(state);
        s.setLatitude(BigDecimal.valueOf(lat));
        s.setLongitude(BigDecimal.valueOf(lon));
        return s;
        }

    private PriceEntity price(StationEntity station, double amount, LocalDateTime at) {
        PriceEntity p = new PriceEntity();
        p.setStation(station);
        p.setProduct(null);
        p.setAmount(BigDecimal.valueOf(amount));
        p.setEffectiveAt(at);
        return p;
    }

    private SaleEntity sale(StationEntity station, double volume, LocalDateTime at) {
        SaleEntity s = new SaleEntity();
        s.setStation(station);
        s.setProduct(null);
        s.setVolume(BigDecimal.valueOf(volume));
        s.setSoldAt(at);
        return s;
    }
}
