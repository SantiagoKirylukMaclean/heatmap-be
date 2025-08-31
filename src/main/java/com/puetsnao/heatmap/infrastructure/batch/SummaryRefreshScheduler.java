package com.puetsnao.heatmap.infrastructure.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;

@Component
@ConditionalOnProperty(name = "heatmap.summary-refresh.enabled", havingValue = "true")
public class SummaryRefreshScheduler {

    private static final Logger log = LoggerFactory.getLogger(SummaryRefreshScheduler.class);

    private final JdbcTemplate jdbcTemplate;
    private final SummaryRefreshProperties properties;

    public SummaryRefreshScheduler(JdbcTemplate jdbcTemplate, SummaryRefreshProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${heatmap.summary-refresh.fixed-delay-ms:600000}",
            initialDelayString = "${heatmap.summary-refresh.initial-delay-ms:10000}")
    @Transactional
    public void refresh() {
        LocalDate today = LocalDate.now();
        int windowDays = Math.max(properties.windowDays(), 1);
        LocalDate fromDate = today.minusDays(windowDays);
        LocalDate toDate = today;

        log.info("Starting summary refresh windowDays={} from={} to={}", windowDays, fromDate, toDate);

        int deleted = jdbcTemplate.update(
                "DELETE FROM daily_state_product_summary WHERE bucket_date BETWEEN ? AND ?",
                ps -> {
                    ps.setDate(1, Date.valueOf(fromDate));
                    ps.setDate(2, Date.valueOf(toDate));
                }
        );

        String insertSql = """
                WITH price_daily AS (
                    SELECT CAST(p.effective_at AS DATE) AS bucket_date,
                           s.state AS state,
                           p.product_id AS product_id,
                           SUM(p.amount) AS price_sum,
                           COUNT(*) AS price_count
                    FROM price p
                    JOIN station s ON s.id = p.station_id
                    WHERE CAST(p.effective_at AS DATE) BETWEEN ? AND ?
                    GROUP BY CAST(p.effective_at AS DATE), s.state, p.product_id
                ),
                sales_daily AS (
                    SELECT CAST(sa.sold_at AS DATE) AS bucket_date,
                           s.state AS state,
                           sa.product_id AS product_id,
                           SUM(sa.volume) AS volume_sum,
                           COUNT(*) AS sale_count
                    FROM sales sa
                    JOIN station s ON s.id = sa.station_id
                    WHERE CAST(sa.sold_at AS DATE) BETWEEN ? AND ?
                    GROUP BY CAST(sa.sold_at AS DATE), s.state, sa.product_id
                )
                INSERT INTO daily_state_product_summary (
                    bucket_date, state, product_id, price_sum, price_count, volume_sum, sale_count
                )
                SELECT pd.bucket_date, pd.state, pd.product_id, pd.price_sum, pd.price_count, sd.volume_sum, sd.sale_count
                FROM price_daily pd
                LEFT JOIN sales_daily sd
                  ON pd.bucket_date = sd.bucket_date
                 AND pd.state = sd.state
                 AND pd.product_id = sd.product_id
                UNION ALL
                SELECT sd.bucket_date, sd.state, sd.product_id,
                       CAST(NULL AS DECIMAL), CAST(NULL AS BIGINT), sd.volume_sum, sd.sale_count
                FROM sales_daily sd
                LEFT JOIN price_daily pd
                  ON pd.bucket_date = sd.bucket_date
                 AND pd.state = sd.state
                 AND pd.product_id = sd.product_id
                WHERE pd.bucket_date IS NULL AND pd.state IS NULL AND pd.product_id IS NULL
                """;

        int inserted = jdbcTemplate.update(insertSql, ps -> {
            ps.setDate(1, Date.valueOf(fromDate));
            ps.setDate(2, Date.valueOf(toDate));
            ps.setDate(3, Date.valueOf(fromDate));
            ps.setDate(4, Date.valueOf(toDate));
        });

        int updated = jdbcTemplate.update(
                "UPDATE summary_watermark SET last_run_date = ? WHERE id = 1",
                ps -> ps.setDate(1, Date.valueOf(today))
        );
        if (updated == 0) {
            jdbcTemplate.update(
                    "INSERT INTO summary_watermark (id, last_run_date) VALUES (1, ?)",
                    ps -> ps.setDate(1, Date.valueOf(today))
            );
        }

        log.info("Summary refresh done: deleted={}, inserted={}, watermark={}.", deleted, inserted, today);
    }
}
