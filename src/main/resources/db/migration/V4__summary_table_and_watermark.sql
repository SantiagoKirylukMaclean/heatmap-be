-- Replace view with physical table for incremental refresh and add watermark table

-- Drop the existing view if present (will be replaced by a table of the same name)
DROP VIEW IF EXISTS daily_state_product_summary;

-- Physical summary table with the same logical schema as the prior view
CREATE TABLE IF NOT EXISTS daily_state_product_summary (
    bucket_date DATE NOT NULL,
    state VARCHAR(64) NOT NULL,
    product_id BIGINT NOT NULL,
    price_sum DECIMAL(18,4),
    price_count BIGINT,
    volume_sum DECIMAL(18,4),
    sale_count BIGINT,
    CONSTRAINT pk_daily_state_product_summary PRIMARY KEY (bucket_date, state, product_id)
);

CREATE INDEX IF NOT EXISTS idx_dsps_state ON daily_state_product_summary(state);
CREATE INDEX IF NOT EXISTS idx_dsps_bucket_date ON daily_state_product_summary(bucket_date);

-- Watermark table to track last successful refresh date
CREATE TABLE IF NOT EXISTS summary_watermark (
    id INT PRIMARY KEY,
    last_run_date DATE NOT NULL
);

