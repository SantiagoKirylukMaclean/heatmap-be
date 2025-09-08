package com.puetsnao.heatmap.dev;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Profile("dev")
@Configuration
public class DevDatasetLoader {

    private static final int DAYS = 300; // ~10 months
    private static final int RECORDS_PER_DAY_PER_COMBO = 3; // 3 prices and 3 sales per day
    private static final int BATCH_SIZE = 1000;

    @Bean
    CommandLineRunner seedDevData(JdbcTemplate jdbc, DevSeedProperties seedProps) {
        return args -> {
            Long stationCount = jdbc.queryForObject("select count(*) from station", Long.class);
            Long productCount = jdbc.queryForObject("select count(*) from product", Long.class);
            if (stationCount == null || stationCount == 0 || productCount == null || productCount == 0) {
                System.out.println("[DEBUG_LOG] Reference data missing; skip synthetic loader");
                return;
            }

            Long priceCount = jdbc.queryForObject("select count(*) from price", Long.class);
            Long salesCount = jdbc.queryForObject("select count(*) from sales", Long.class);
            if ((priceCount != null && priceCount > 0) || (salesCount != null && salesCount > 0)) {
                System.out.println("[DEBUG_LOG] price/sales already populated; skip synthetic loader");
                return;
            }

            List<Long> productIds = jdbc.queryForList("select id from product order by id", Long.class);

            if (!Boolean.TRUE.equals(seedProps.enabled())) {
                System.out.println("[DEBUG_LOG] dev.seed.enabled=false; skip synthetic loader");
                return;
            }

            if (Boolean.TRUE.equals(seedProps.enabled())) {
                int target = seedProps.njStationsCount();
                Long njCountObj = jdbc.queryForObject("select count(*) from station where state = 'NJ'", Long.class);
                int njCount = njCountObj == null ? 0 : njCountObj.intValue();
                int missing = Math.max(0, target - njCount);
                if (missing > 0) {
                    List<Object[]> stationBatch = new ArrayList<>(BATCH_SIZE);
                    Random rnd = new Random(123);
                    for (int i = 0; i < missing; i++) {
                        int seq = njCount + i + 1;
                        String code = String.format("NJ-%05d", seq);
                        String name = "NJ Station " + code;
                        double lat = 39.5 + rnd.nextDouble() * (41.4 - 39.5);
                        double lon = -75.6 + rnd.nextDouble() * (-73.9 + 75.6);
                        stationBatch.add(new Object[]{code, name, "NJ", lat, lon});
                        if (stationBatch.size() >= BATCH_SIZE) {
                            jdbc.batchUpdate("INSERT INTO station (code, name, state, latitude, longitude) VALUES (?,?,?,?,?)", stationBatch);
                            stationBatch.clear();
                        }
                    }
                    if (!stationBatch.isEmpty()) {
                        jdbc.batchUpdate("INSERT INTO station (code, name, state, latitude, longitude) VALUES (?,?,?,?,?)", stationBatch);
                        stationBatch.clear();
                    }
                    System.out.println("[DEBUG_LOG] Seeded NJ stations: +" + missing + " (total target=" + target + ")");
                }

                List<Long> stationIds = jdbc.queryForList("select id from station where state = 'NJ' order by id limit " + target, Long.class);
                System.out.println("[DEBUG_LOG] Generating heavy NJ dataset for dev: stations=" + stationIds.size() + ", products=" + productIds.size());

                int totalPrice = 0;
                int totalSales = 0;
                List<Object[]> priceBatch = new ArrayList<>(BATCH_SIZE);
                List<Object[]> salesBatch = new ArrayList<>(BATCH_SIZE);
                LocalDate seedDay = seedProps.day();
                Random random = new Random(42);
                for (Long stationId : stationIds) {
                    for (Long productId : productIds) {
                        for (int hour = 0; hour < 24; hour++) {
                            // Price row per hour
                            BigDecimal base = basePrice(productId);
                            BigDecimal jitter = BigDecimal.valueOf(random.nextDouble() * 0.2 - 0.1);
                            BigDecimal amount = base.add(jitter).setScale(4, RoundingMode.HALF_UP);
                            LocalDateTime priceTs = seedDay.atTime(hour, random.nextInt(60));
                            priceBatch.add(new Object[]{stationId, productId, amount, priceTs});
                            if (priceBatch.size() >= BATCH_SIZE) {
                                batchInsertPrice(jdbc, priceBatch);
                                totalPrice += priceBatch.size();
                                priceBatch.clear();
                            }
                            // Sales row per hour
                            BigDecimal volume = BigDecimal.valueOf(50 + random.nextInt(300) + random.nextDouble()).setScale(3, RoundingMode.HALF_UP);
                            LocalDateTime saleTs = seedDay.atTime(hour, random.nextInt(60));
                            salesBatch.add(new Object[]{stationId, productId, saleTs, volume});
                            if (salesBatch.size() >= BATCH_SIZE) {
                                batchInsertSales(jdbc, salesBatch);
                                totalSales += salesBatch.size();
                                salesBatch.clear();
                            }
                        }
                    }
                }
                if (!priceBatch.isEmpty()) { batchInsertPrice(jdbc, priceBatch); totalPrice += priceBatch.size(); priceBatch.clear(); }
                if (!salesBatch.isEmpty()) { batchInsertSales(jdbc, salesBatch); totalSales += salesBatch.size(); salesBatch.clear(); }
                Map<String, Object> counts = jdbc.queryForMap("select (select count(*) from price) as price_count, (select count(*) from sales) as sales_count");
                System.out.println("[DEBUG_LOG] Heavy NJ dataset loaded: price=" + counts.get("price_count") + ", sales=" + counts.get("sales_count"));
                return;
            }

            List<Long> stationIds = jdbc.queryForList("select id from station order by id", Long.class);

            System.out.println("[DEBUG_LOG] Generating synthetic dataset for dev: stations=" + stationIds.size() + ", products=" + productIds.size());

            LocalDate startDate = LocalDate.now().minusDays(DAYS);
            Random random = new Random(42);

            int totalPrice = 0;
            int totalSales = 0;

            List<Object[]> priceBatch = new ArrayList<>(BATCH_SIZE);
            List<Object[]> salesBatch = new ArrayList<>(BATCH_SIZE);

            for (Long stationId : stationIds) {
                for (Long productId : productIds) {
                    LocalDate day = startDate;
                    for (int d = 0; d < DAYS; d++) {
                        for (int n = 0; n < RECORDS_PER_DAY_PER_COMBO; n++) {
                            // Price row
                            BigDecimal base = basePrice(productId);
                            BigDecimal jitter = BigDecimal.valueOf(random.nextDouble() * 0.2 - 0.1);
                            BigDecimal amount = base.add(jitter).setScale(4, RoundingMode.HALF_UP);
                            LocalDateTime priceTs = LocalDateTime.of(day, LocalTime.of(random.nextInt(24), random.nextInt(60)));
                            priceBatch.add(new Object[]{stationId, productId, amount, priceTs});
                            if (priceBatch.size() >= BATCH_SIZE) {
                                batchInsertPrice(jdbc, priceBatch);
                                totalPrice += priceBatch.size();
                                priceBatch.clear();
                            }

                            // Sales row
                            BigDecimal volume = BigDecimal.valueOf(50 + random.nextInt(300) + random.nextDouble()).setScale(3, RoundingMode.HALF_UP);
                            LocalDateTime saleTs = LocalDateTime.of(day, LocalTime.of(random.nextInt(24), random.nextInt(60)));
                            salesBatch.add(new Object[]{stationId, productId, saleTs, volume});
                            if (salesBatch.size() >= BATCH_SIZE) {
                                batchInsertSales(jdbc, salesBatch);
                                totalSales += salesBatch.size();
                                salesBatch.clear();
                            }
                        }
                        day = day.plusDays(1);
                    }
                }
            }

            if (!priceBatch.isEmpty()) {
                batchInsertPrice(jdbc, priceBatch);
                totalPrice += priceBatch.size();
                priceBatch.clear();
            }
            if (!salesBatch.isEmpty()) {
                batchInsertSales(jdbc, salesBatch);
                totalSales += salesBatch.size();
                salesBatch.clear();
            }

            Map<String, Object> counts = jdbc.queryForMap("select (select count(*) from price) as price_count, (select count(*) from sales) as sales_count");
            System.out.println("[DEBUG_LOG] Synthetic dataset loaded: price=" + counts.get("price_count") + ", sales=" + counts.get("sales_count"));
        };
    }

    private static BigDecimal basePrice(Long productId) {
        // Simple base by product type order: 1:UNL ~2.5, 2:PREM ~3.2, 3:DSL ~3.0
        int idx = (int)(productId % 3);
        return switch (idx) {
            case 1 -> BigDecimal.valueOf(3.20);
            case 2 -> BigDecimal.valueOf(3.00);
            default -> BigDecimal.valueOf(2.50);
        };
    }

    private static void batchInsertPrice(JdbcTemplate jdbc, List<Object[]> rows) {
        jdbc.batchUpdate("INSERT INTO price (station_id, product_id, amount, effective_at) VALUES (?,?,?,?)", rows);
    }

    private static void batchInsertSales(JdbcTemplate jdbc, List<Object[]> rows) {
        jdbc.batchUpdate("INSERT INTO sales (station_id, product_id, sold_at, volume) VALUES (?,?,?,?)", rows);
    }
}
