package com.puetsnao.heatmap.infrastructure.summary;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Repository
public class DefaultSummaryRepository implements SummaryRepository {

    private final JdbcTemplate jdbcTemplate;

    public DefaultSummaryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Map<String, Double> averagePriceByState(LocalDate fromDate, LocalDate toDate) {
        String sql = """
            SELECT state,
                   SUM(COALESCE(price_sum, 0)) AS total_price_sum,
                   SUM(COALESCE(price_count, 0)) AS total_price_count
            FROM daily_state_product_summary
            WHERE bucket_date BETWEEN ? AND ?
            GROUP BY state
            """;
        return jdbcTemplate.query(sql, ps -> {
            ps.setDate(1, Date.valueOf(fromDate));
            ps.setDate(2, Date.valueOf(toDate));
        }, rs -> {
            Map<String, Double> result = new HashMap<>();
            while (rs.next()) {
                String state = rs.getString("state");
                BigDecimal sum = rs.getBigDecimal("total_price_sum");
                long count = rs.getLong("total_price_count");
                if (count > 0) {
                    double avg = (sum != null ? sum.doubleValue() : 0.0) / (double) count;
                    result.put(state, avg);
                }
            }
            return result;
        });
    }

    @Override
    public Map<String, Double> totalVolumeByState(LocalDate fromDate, LocalDate toDate) {
        String sql = """
            SELECT state,
                   SUM(COALESCE(volume_sum, 0)) AS total_volume
            FROM daily_state_product_summary
            WHERE bucket_date BETWEEN ? AND ?
            GROUP BY state
            """;
        return jdbcTemplate.query(sql, ps -> {
            ps.setDate(1, Date.valueOf(fromDate));
            ps.setDate(2, Date.valueOf(toDate));
        }, rs -> {
            Map<String, Double> result = new HashMap<>();
            while (rs.next()) {
                String state = rs.getString("state");
                BigDecimal total = rs.getBigDecimal("total_volume");
                result.put(state, total != null ? total.doubleValue() : 0.0);
            }
            return result;
        });
    }
}
