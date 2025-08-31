package com.puetsnao.heatmap.infrastructure.persistence.jpa.repository;

import com.puetsnao.heatmap.infrastructure.persistence.jpa.entity.ProductEntity;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends Repository<ProductEntity, Long> {
    List<ProductEntity> findAll();
    Optional<ProductEntity> findById(Long id);
    boolean existsById(Long id);
    long count();
}
