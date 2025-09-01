package com.puetsnao.heatmap.infrastructure.batch;

import com.uber.h3core.H3Core;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty(name = "heatmap.summary-refresh.enabled", havingValue = "true")
public class H3SummaryRefreshScheduler {

    private static final Logger log = LoggerFactory.getLogger(H3SummaryRefreshScheduler.class);

    private static final int[] RESOLUTIONS = new int[]{5, 7, 9};

    private final JdbcTemplate jdbcTemplate;
    private final SummaryRefreshProperties properties;

    public H3SummaryRefreshScheduler(JdbcTemplate jdbcTemplate, SummaryRefreshProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${heatmap.summary-refresh.fixed-delay-ms:600000}",
            initialDelayString = "${heatmap.summary-refresh.initial-delay-ms:15000}")
    @Transactional
    public void refreshH3() {
        LocalDate today = LocalDate.now();
        int windowDays = Math.max(properties.windowDays(), 1);
        LocalDate fromDate = today.minusDays(windowDays);
        LocalDate toDate = today;

        log.info("Starting H3 summary refresh windowDays={} from={} to={}", windowDays, fromDate, toDate);

        upsertStationH3Index();
        refreshDailyH3(fromDate, toDate);
        refreshHourlyH3(fromDate, toDate);

        log.info("H3 summary refresh done for window [{}..{}]", fromDate, toDate);
    }

    private void upsertStationH3Index() {
        List<StationRow> stations = jdbcTemplate.query(
                "SELECT id, latitude, longitude FROM station",
                (rs, rowNum) -> new StationRow(rs.getLong("id"), rs.getDouble("latitude"), rs.getDouble("longitude"))
        );
        if (stations.isEmpty()) return;

        jdbcTemplate.update("DELETE FROM station_h3_index");

        try {
            H3Core h3 = H3Core.newInstance();
            List<IndexRow> indexRows = new ArrayList<>();
            for (StationRow s : stations) {
                for (int res : RESOLUTIONS) {
                    String cell = h3.geoToH3Address(s.lat(), s.lon(), res);
                    indexRows.add(new IndexRow(s.id(), res, cell));
                }
            }
            batchInsertIndex(indexRows);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build H3 index", e);
        }
    }

    private void batchInsertIndex(List<IndexRow> rows) {
        jdbcTemplate.batchUpdate(
                "INSERT INTO station_h3_index (station_id, resolution, h3_cell) VALUES (?, ?, ?)",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        IndexRow r = rows.get(i);
                        ps.setLong(1, r.stationId());
                        ps.setInt(2, r.resolution());
                        ps.setString(3, r.h3Cell());
                    }

                    @Override
                    public int getBatchSize() {
                        return rows.size();
                    }
                }
        );
    }

    private void refreshDailyH3(LocalDate fromDate, LocalDate toDate) {
        int del = jdbcTemplate.update(
                "DELETE FROM daily_h3_product_summary WHERE bucket_date BETWEEN ? AND ?",
                ps -> {
                    ps.setDate(1, Date.valueOf(fromDate));
                    ps.setDate(2, Date.valueOf(toDate));
                }
        );
        log.info("Deleted {} rows from daily_h3_product_summary for window", del);

        String insertSql = """
                WITH price_daily AS (
                    SELECT CAST(p.effective_at AS DATE) AS bucket_date,
                           i.resolution AS resolution,
                           i.h3_cell AS h3_cell,
                           p.product_id AS product_id,
                           SUM(p.amount) AS price_sum,
                           COUNT(*) AS price_count
                    FROM price p
                    JOIN station_h3_index i ON i.station_id = p.station_id
                    WHERE CAST(p.effective_at AS DATE) BETWEEN ? AND ?
                    GROUP BY CAST(p.effective_at AS DATE), i.resolution, i.h3_cell, p.product_id
                ),
                sales_daily AS (
                    SELECT CAST(sa.sold_at AS DATE) AS bucket_date,
                           i.resolution AS resolution,
                           i.h3_cell AS h3_cell,
                           sa.product_id AS product_id,
                           SUM(sa.volume) AS volume_sum,
                           COUNT(*) AS sale_count
                    FROM sales sa
                    JOIN station_h3_index i ON i.station_id = sa.station_id
                    WHERE CAST(sa.sold_at AS DATE) BETWEEN ? AND ?
                    GROUP BY CAST(sa.sold_at AS DATE), i.resolution, i.h3_cell, sa.product_id
                )
                INSERT INTO daily_h3_product_summary (
                    bucket_date, resolution, h3_cell, product_id, price_sum, price_count, volume_sum, sale_count
                )
                SELECT pd.bucket_date, pd.resolution, pd.h3_cell, pd.product_id, pd.price_sum, pd.price_count, sd.volume_sum, sd.sale_count
                FROM price_daily pd
                LEFT JOIN sales_daily sd
                  ON pd.bucket_date = sd.bucket_date
                 AND pd.resolution = sd.resolution
                 AND pd.h3_cell = sd.h3_cell
                 AND pd.product_id = sd.product_id
                UNION ALL
                SELECT sd.bucket_date, sd.resolution, sd.h3_cell, sd.product_id,
                       CAST(NULL AS DECIMAL), CAST(NULL AS BIGINT), sd.volume_sum, sd.sale_count
                FROM sales_daily sd
                LEFT JOIN price_daily pd
                  ON pd.bucket_date = sd.bucket_date
                 AND pd.resolution = sd.resolution
                 AND pd.h3_cell = sd.h3_cell
                 AND pd.product_id = sd.product_id
                WHERE pd.bucket_date IS NULL AND pd.resolution IS NULL AND pd.h3_cell IS NULL AND pd.product_id IS NULL
                """;

        int ins = jdbcTemplate.update(insertSql, ps -> {
            ps.setDate(1, Date.valueOf(fromDate));
            ps.setDate(2, Date.valueOf(toDate));
            ps.setDate(3, Date.valueOf(fromDate));
            ps.setDate(4, Date.valueOf(toDate));
        });
        log.info("Inserted {} rows into daily_h3_product_summary", ins);
    }

    private void refreshHourlyH3(LocalDate fromDate, LocalDate toDate) {
        // compute full-hour window [fromDate 00:00, toDate 23:00]
        LocalDateTime fromHour = fromDate.atStartOfDay();
        LocalDateTime toHour = toDate.atTime(23, 0).truncatedTo(ChronoUnit.HOURS);

        int del = jdbcTemplate.update(
                "DELETE FROM hourly_h3_product_summary WHERE bucket_hour BETWEEN ? AND ?",
                ps -> {
                    ps.setTimestamp(1, java.sql.Timestamp.valueOf(fromHour));
                    ps.setTimestamp(2, java.sql.Timestamp.valueOf(toHour));
                }
        );
        log.info("Deleted {} rows from hourly_h3_product_summary for window", del);

        String insertSql = """
                WITH price_hourly AS (
                    SELECT DATE_TRUNC('hour', p.effective_at) AS bucket_hour,
                           i.resolution AS resolution,
                           i.h3_cell AS h3_cell,
                           p.product_id AS product_id,
                           SUM(p.amount) AS price_sum,
                           COUNT(*) AS price_count
                    FROM price p
                    JOIN station_h3_index i ON i.station_id = p.station_id
                    WHERE DATE_TRUNC('hour', p.effective_at) BETWEEN ? AND ?
                    GROUP BY DATE_TRUNC('hour', p.effective_at), i.resolution, i.h3_cell, p.product_id
                ),
                sales_hourly AS (
                    SELECT DATE_TRUNC('hour', sa.sold_at) AS bucket_hour,
                           i.resolution AS resolution,
                           i.h3_cell AS h3_cell,
                           sa.product_id AS product_id,
                           SUM(sa.volume) AS volume_sum,
                           COUNT(*) AS sale_count
                    FROM sales sa
                    JOIN station_h3_index i ON i.station_id = sa.station_id
                    WHERE DATE_TRUNC('hour', sa.sold_at) BETWEEN ? AND ?
                    GROUP BY DATE_TRUNC('hour', sa.sold_at), i.resolution, i.h3_cell, sa.product_id
                )
                INSERT INTO hourly_h3_product_summary (
                    bucket_hour, resolution, h3_cell, product_id, price_sum, price_count, volume_sum, sale_count
                )
                SELECT ph.bucket_hour, ph.resolution, ph.h3_cell, ph.product_id, ph.price_sum, ph.price_count, sh.volume_sum, sh.sale_count
                FROM price_hourly ph
                LEFT JOIN sales_hourly sh
                  ON ph.bucket_hour = sh.bucket_hour
                 AND ph.resolution = sh.resolution
                 AND ph.h3_cell = sh.h3_cell
                 AND ph.product_id = sh.product_id
                UNION ALL
                SELECT sh.bucket_hour, sh.resolution, sh.h3_cell, sh.product_id,
                       CAST(NULL AS DECIMAL), CAST(NULL AS BIGINT), sh.volume_sum, sh.sale_count
                FROM sales_hourly sh
                LEFT JOIN price_hourly ph
                  ON ph.bucket_hour = sh.bucket_hour
                 AND ph.resolution = sh.resolution
                 AND ph.h3_cell = sh.h3_cell
                 AND ph.product_id = sh.product_id
                WHERE ph.bucket_hour IS NULL AND ph.resolution IS NULL AND ph.h3_cell IS NULL AND ph.product_id IS NULL
                """;

        int ins = jdbcTemplate.update(insertSql, ps -> {
            ps.setTimestamp(1, java.sql.Timestamp.valueOf(fromHour));
            ps.setTimestamp(2, java.sql.Timestamp.valueOf(toHour));
            ps.setTimestamp(3, java.sql.Timestamp.valueOf(fromHour));
            ps.setTimestamp(4, java.sql.Timestamp.valueOf(toHour));
        });
        log.info("Inserted {} rows into hourly_h3_product_summary", ins);
    }

    private record StationRow(long id, double lat, double lon) { }
    private record IndexRow(long stationId, int resolution, String h3Cell) { }
}
