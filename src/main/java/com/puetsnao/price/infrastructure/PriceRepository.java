package com.puetsnao.price.infrastructure;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

public interface PriceRepository extends Repository<PriceEntity, Long> {
    List<PriceEntity> findAll();
    Optional<PriceEntity> findById(Long id);
    boolean existsById(Long id);
    long count();
}
