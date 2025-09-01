package com.puetsnao.heatmap.infrastructure.summary;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class DefaultSummaryRepositoryTests {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private SummaryRepository summaryRepository;

    private LocalDate d1;
    private LocalDate d2;

    @BeforeEach
    void setup() {
        jdbc.update("DELETE FROM daily_state_product_summary");
        d1 = LocalDate.now().minusDays(3);
        d2 = LocalDate.now().minusDays(2);

        // TX two products and two days
        insertRow(d1, "TX", 1L, 10.0, 2L, 100.0, 3L);
        insertRow(d1, "TX", 2L, 4.0, 1L, 50.0, 1L);
        insertRow(d2, "TX", 1L, 6.0, 2L, 30.0, 1L);

        // CA one product one day
        insertRow(d1, "CA", 1L, 9.0, 3L, 70.0, 2L);
    }

    @Test
    void averagePriceByStateAggregatesWeightedByCount() {
        Map<String, Double> result = summaryRepository.averagePriceByState(d1, LocalDate.now());
        // TX: price_sum total = 10 + 4 + 6 = 20; price_count total = 2 + 1 + 2 = 5 => avg = 4.0
        // CA: 9 / 3 = 3.0
        assertThat(result).containsKeys("TX", "CA");
        assertThat(result.get("TX")).isNotNull();
        assertThat(result.get("CA")).isNotNull();
        assertThat(result.get("TX")).isCloseTo(4.0, offset(1e-9));
        assertThat(result.get("CA")).isCloseTo(3.0, offset(1e-9));
    }

    @Test
    void totalVolumeByStateSumsVolumeAcrossRows() {
        Map<String, Double> result = summaryRepository.totalVolumeByState(d1, LocalDate.now());
        // TX: 100 + 50 + 30 = 180
        // CA: 70
        assertThat(result).containsKeys("TX", "CA");
        assertThat(result.get("TX")).isCloseTo(180.0, offset(1e-9));
        assertThat(result.get("CA")).isCloseTo(70.0, offset(1e-9));
    }

    private void insertRow(LocalDate date, String state, Long productId,
                           Double priceSum, Long priceCount,
                           Double volumeSum, Long saleCount) {
        jdbc.update(
                "INSERT INTO daily_state_product_summary (bucket_date, state, product_id, price_sum, price_count, volume_sum, sale_count) VALUES (?,?,?,?,?,?,?)",
                date, state, productId, priceSum, priceCount, volumeSum, saleCount
        );
    }

}
