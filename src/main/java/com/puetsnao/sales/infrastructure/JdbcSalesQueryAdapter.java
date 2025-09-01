package com.puetsnao.sales.infrastructure;

import com.puetsnao.sales.app.SalesQueryPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Repository
public class JdbcSalesQueryAdapter implements SalesQueryPort {

    private final JdbcTemplate jdbcTemplate;

    public JdbcSalesQueryAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Map<String, Double> totalVolumeByState(LocalDate from, LocalDate to) {
        LocalDateTime fromTs = from.atStartOfDay();
        LocalDateTime toExclusive = to.plusDays(1).atStartOfDay();
        String sql = """
            SELECT s.state AS state, SUM(sales.volume) AS total_volume
            FROM sales
            JOIN station s ON s.id = sales.station_id
            WHERE sales.sold_at >= ? AND sales.sold_at < ?
            GROUP BY s.state
            """;
        return jdbcTemplate.query(sql, ps -> {
            ps.setTimestamp(1, Timestamp.valueOf(fromTs));
            ps.setTimestamp(2, Timestamp.valueOf(toExclusive));
        }, rs -> {
            Map<String, Double> result = new HashMap<>();
            while (rs.next()) {
                result.put(rs.getString("state"), rs.getBigDecimal("total_volume").doubleValue());
            }
            return result;
        });
    }
}
