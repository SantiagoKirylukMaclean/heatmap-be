package com.puetsnao.heatmap.infrastructure.summary;

import com.puetsnao.heatmap.domain.Metric;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Repository
public class DefaultH3SummaryRepository implements H3SummaryRepository {

    private final JdbcTemplate jdbcTemplate;

    public DefaultH3SummaryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Map<String, Double> byDay(LocalDate bucketDate, int resolution, Metric metric) {
        String sql = switch (metric) {
            case PRICE -> """
                SELECT h3_cell,
                       SUM(COALESCE(price_sum, 0)) / NULLIF(SUM(COALESCE(price_count, 0)), 0) AS value
                FROM daily_h3_product_summary
                WHERE bucket_date = ? AND resolution = ?
                GROUP BY h3_cell
                HAVING SUM(COALESCE(price_count, 0)) > 0
                """;
            case VOLUME -> """
                SELECT h3_cell,
                       SUM(COALESCE(volume_sum, 0)) AS value
                FROM daily_h3_product_summary
                WHERE bucket_date = ? AND resolution = ?
                GROUP BY h3_cell
                """;
        };
        return jdbcTemplate.query(sql, ps -> {
            ps.setDate(1, Date.valueOf(bucketDate));
            ps.setInt(2, resolution);
        }, rs -> {
            Map<String, Double> result = new HashMap<>();
            while (rs.next()) {
                String cell = rs.getString("h3_cell");
                BigDecimal value = rs.getBigDecimal("value");
                result.put(cell, value != null ? value.doubleValue() : 0.0);
            }
            return result;
        });
    }

    @Override
    public Map<String, Double> byHour(LocalDateTime bucketHour, int resolution, Metric metric) {
        String sql = switch (metric) {
            case PRICE -> """
                SELECT h3_cell,
                       SUM(COALESCE(price_sum, 0)) / NULLIF(SUM(COALESCE(price_count, 0)), 0) AS value
                FROM hourly_h3_product_summary
                WHERE bucket_hour = ? AND resolution = ?
                GROUP BY h3_cell
                HAVING SUM(COALESCE(price_count, 0)) > 0
                """;
            case VOLUME -> """
                SELECT h3_cell,
                       SUM(COALESCE(volume_sum, 0)) AS value
                FROM hourly_h3_product_summary
                WHERE bucket_hour = ? AND resolution = ?
                GROUP BY h3_cell
                """;
        };
        return jdbcTemplate.query(sql, ps -> {
            ps.setTimestamp(1, Timestamp.valueOf(bucketHour));
            ps.setInt(2, resolution);
        }, rs -> {
            Map<String, Double> result = new HashMap<>();
            while (rs.next()) {
                String cell = rs.getString("h3_cell");
                BigDecimal value = rs.getBigDecimal("value");
                result.put(cell, value != null ? value.doubleValue() : 0.0);
            }
            return result;
        });
    }
}
