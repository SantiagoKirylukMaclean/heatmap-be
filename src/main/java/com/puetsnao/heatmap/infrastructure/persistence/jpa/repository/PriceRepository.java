package com.puetsnao.heatmap.infrastructure.persistence.jpa.repository;

import com.puetsnao.heatmap.infrastructure.persistence.jpa.entity.PriceEntity;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

public interface PriceRepository extends Repository<PriceEntity, Long> {
    List<PriceEntity> findAll();
    Optional<PriceEntity> findById(Long id);
    boolean existsById(Long id);
    long count();
}
