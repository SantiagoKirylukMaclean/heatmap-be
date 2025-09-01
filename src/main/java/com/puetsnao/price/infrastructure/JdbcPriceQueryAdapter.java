package com.puetsnao.price.infrastructure;

import com.puetsnao.price.app.PriceQueryPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Repository
public class JdbcPriceQueryAdapter implements PriceQueryPort {

    private final JdbcTemplate jdbcTemplate;

    public JdbcPriceQueryAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Map<String, Double> averagePriceByState(LocalDate from, LocalDate to) {
        LocalDateTime fromTs = from.atStartOfDay();
        LocalDateTime toExclusive = to.plusDays(1).atStartOfDay();
        String sql = """
            SELECT s.state AS state, AVG(p.amount) AS avg_price
            FROM price p
            JOIN station s ON s.id = p.station_id
            WHERE p.effective_at >= ? AND p.effective_at < ?
            GROUP BY s.state
            """;
        return jdbcTemplate.query(sql, ps -> {
            ps.setTimestamp(1, Timestamp.valueOf(fromTs));
            ps.setTimestamp(2, Timestamp.valueOf(toExclusive));
        }, rs -> {
            Map<String, Double> result = new HashMap<>();
            while (rs.next()) {
                result.put(rs.getString("state"), rs.getBigDecimal("avg_price").doubleValue());
            }
            return result;
        });
    }
}
