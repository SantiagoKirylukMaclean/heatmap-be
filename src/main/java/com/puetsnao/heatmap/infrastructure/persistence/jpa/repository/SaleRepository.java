package com.puetsnao.heatmap.infrastructure.persistence.jpa.repository;

import com.puetsnao.heatmap.infrastructure.persistence.jpa.entity.SaleEntity;
import org.springframework.data.repository.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SaleRepository extends Repository<SaleEntity, Long> {
    List<SaleEntity> findAll();
    Optional<SaleEntity> findById(Long id);
    boolean existsById(Long id);
    long count();

    List<SaleEntity> findByStationIdAndProductIdAndSoldAtBetween(Long stationId, Long productId, LocalDateTime from, LocalDateTime to);
}
