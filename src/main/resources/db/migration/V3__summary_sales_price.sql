-- Daily summary for price and sales by state and product
-- Compatible with PostgreSQL and H2 (PostgreSQL mode)

DROP VIEW IF EXISTS daily_state_product_summary;

CREATE VIEW daily_state_product_summary AS
WITH price_daily AS (
    SELECT
        CAST(p.effective_at AS DATE)               AS bucket_date,
        s.state                                    AS state,
        p.product_id                               AS product_id,
        SUM(p.amount)                              AS price_sum,
        COUNT(*)                                   AS price_count
    FROM price p
    JOIN station s ON s.id = p.station_id
    GROUP BY CAST(p.effective_at AS DATE), s.state, p.product_id
),
sales_daily AS (
    SELECT
        CAST(sa.sold_at AS DATE)                   AS bucket_date,
        s.state                                    AS state,
        sa.product_id                              AS product_id,
        SUM(sa.volume)                             AS volume_sum,
        COUNT(*)                                   AS sale_count
    FROM sales sa
    JOIN station s ON s.id = sa.station_id
    GROUP BY CAST(sa.sold_at AS DATE), s.state, sa.product_id
)
-- price side with optional sales
SELECT
    pd.bucket_date,
    pd.state,
    pd.product_id,
    pd.price_sum,
    pd.price_count,
    sd.volume_sum,
    sd.sale_count
FROM price_daily pd
LEFT JOIN sales_daily sd
  ON pd.bucket_date = sd.bucket_date
 AND pd.state = sd.state
 AND pd.product_id = sd.product_id
UNION ALL
-- sales-only rows (no price on that day/state/product)
SELECT
    sd.bucket_date,
    sd.state,
    sd.product_id,
    CAST(NULL AS DECIMAL) AS price_sum,
    CAST(NULL AS BIGINT)  AS price_count,
    sd.volume_sum,
    sd.sale_count
FROM sales_daily sd
LEFT JOIN price_daily pd
  ON pd.bucket_date = sd.bucket_date
 AND pd.state = sd.state
 AND pd.product_id = sd.product_id
WHERE pd.bucket_date IS NULL AND pd.state IS NULL AND pd.product_id IS NULL;