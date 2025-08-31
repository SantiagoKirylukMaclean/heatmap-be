package com.puetsnao.sales.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface SaleRepository extends JpaRepository<SaleEntity, Long> {
    List<SaleEntity> findByStationIdAndProductIdAndSoldAtBetween(Long stationId, Long productId, LocalDateTime from, LocalDateTime to);
}
