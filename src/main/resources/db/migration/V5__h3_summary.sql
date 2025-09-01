-- H3 station index and daily/hourly H3 summaries

CREATE TABLE IF NOT EXISTS station_h3_index (
    station_id BIGINT NOT NULL REFERENCES station(id) ON DELETE CASCADE,
    resolution INT NOT NULL,
    h3_cell VARCHAR(32) NOT NULL,
    CONSTRAINT pk_station_h3 PRIMARY KEY (station_id, resolution)
);

CREATE INDEX IF NOT EXISTS idx_h3_cell_res ON station_h3_index(h3_cell, resolution);

-- Daily summary by H3 cell and product
CREATE TABLE IF NOT EXISTS daily_h3_product_summary (
    bucket_date DATE NOT NULL,
    resolution INT NOT NULL,
    h3_cell VARCHAR(32) NOT NULL,
    product_id BIGINT NOT NULL,
    price_sum DECIMAL(18,4),
    price_count BIGINT,
    volume_sum DECIMAL(18,4),
    sale_count BIGINT,
    CONSTRAINT pk_daily_h3_product PRIMARY KEY (bucket_date, resolution, h3_cell, product_id)
);

CREATE INDEX IF NOT EXISTS idx_dh3_bucket ON daily_h3_product_summary(bucket_date);
CREATE INDEX IF NOT EXISTS idx_dh3_cell ON daily_h3_product_summary(h3_cell);
CREATE INDEX IF NOT EXISTS idx_dh3_res ON daily_h3_product_summary(resolution);

-- Hourly summary by H3 cell and product
CREATE TABLE IF NOT EXISTS hourly_h3_product_summary (
    bucket_hour TIMESTAMP NOT NULL,
    resolution INT NOT NULL,
    h3_cell VARCHAR(32) NOT NULL,
    product_id BIGINT NOT NULL,
    price_sum DECIMAL(18,4),
    price_count BIGINT,
    volume_sum DECIMAL(18,4),
    sale_count BIGINT,
    CONSTRAINT pk_hourly_h3_product PRIMARY KEY (bucket_hour, resolution, h3_cell, product_id)
);

CREATE INDEX IF NOT EXISTS idx_hh3_bucket ON hourly_h3_product_summary(bucket_hour);
CREATE INDEX IF NOT EXISTS idx_hh3_cell ON hourly_h3_product_summary(h3_cell);
CREATE INDEX IF NOT EXISTS idx_hh3_res ON hourly_h3_product_summary(resolution);
