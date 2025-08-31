package com.puetsnao.heatmap.infrastructure.persistence.jpa.repository;

import com.puetsnao.heatmap.infrastructure.persistence.jpa.entity.StationEntity;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

public interface StationRepository extends Repository<StationEntity, Long> {
    List<StationEntity> findAll();
    Optional<StationEntity> findById(Long id);
    boolean existsById(Long id);
    long count();
}
