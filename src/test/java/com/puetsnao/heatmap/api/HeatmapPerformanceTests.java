package com.puetsnao.heatmap.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HeatmapPerformanceTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void seedMinimalData() {
        jdbc.update("DELETE FROM daily_state_product_summary");
        jdbc.update("DELETE FROM station");

        // Insert minimal stations for centroid calculation
        jdbc.update("INSERT INTO station (code, name, state, latitude, longitude) VALUES (?,?,?,?,?)",
                "TX001", "Station TX", "TX", new BigDecimal("31.9686"), new BigDecimal("-99.9018"));
        jdbc.update("INSERT INTO station (code, name, state, latitude, longitude) VALUES (?,?,?,?,?)",
                "CA001", "Station CA", "CA", new BigDecimal("36.7783"), new BigDecimal("-119.4179"));

        // Insert summary rows within last30d window
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        insertSummary(yesterday, "TX", 1L, 12.0, 3L, 200.0, 2L);
        insertSummary(today, "TX", 1L, 8.0, 2L, 140.0, 1L);
        insertSummary(yesterday, "CA", 1L, 9.0, 3L, 100.0, 1L);
    }

    @Test
    void cachedHeatmapP95Below1s() throws Exception {
        // Warm up cache
        mockMvc.perform(get("/api/heatmap")
                        .param("metric", "price")
                        .param("period", "last30d")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        int iterations = 40;
        List<Long> latenciesMs = new ArrayList<>(iterations);

        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            mockMvc.perform(get("/api/heatmap")
                            .param("metric", "price")
                            .param("period", "last30d")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
            long end = System.nanoTime();
            latenciesMs.add((end - start) / 1_000_000);
        }

        Collections.sort(latenciesMs);
        long p95 = percentile(latenciesMs, 95);
        assertThat(p95).isLessThan(1000);
    }

    private void insertSummary(LocalDate date, String state, Long productId,
                               Double priceSum, Long priceCount, Double volumeSum, Long saleCount) {
        jdbc.update(
                "INSERT INTO daily_state_product_summary (bucket_date, state, product_id, price_sum, price_count, volume_sum, sale_count) VALUES (?,?,?,?,?,?,?)",
                date, state, productId, priceSum, priceCount, volumeSum, saleCount
        );
    }

    private static long percentile(List<Long> sorted, int p) {
        if (sorted.isEmpty()) return 0;
        int index = (int) Math.ceil(p / 100.0 * sorted.size()) - 1;
        if (index < 0) index = 0;
        if (index >= sorted.size()) index = sorted.size() - 1;
        return sorted.get(index);
        }
}
