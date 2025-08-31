package com.puetsnao.product.infrastructure;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends Repository<ProductEntity, Long> {
    List<ProductEntity> findAll();
    Optional<ProductEntity> findById(Long id);
    boolean existsById(Long id);
    long count();
}
