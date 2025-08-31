package com.puetsnao.heatmap.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "price")
public class PriceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "station_id", nullable = false)
    private StationEntity station;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @Column(name = "amount", nullable = false, precision = 12, scale = 4)
    private BigDecimal amount;

    @Column(name = "effective_at", nullable = false)
    private LocalDateTime effectiveAt;

    public Long getId() { return id; }
    public StationEntity getStation() { return station; }
    public ProductEntity getProduct() { return product; }
    public BigDecimal getAmount() { return amount; }
    public LocalDateTime getEffectiveAt() { return effectiveAt; }

    public void setStation(StationEntity station) { this.station = station; }
    public void setProduct(ProductEntity product) { this.product = product; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setEffectiveAt(LocalDateTime effectiveAt) { this.effectiveAt = effectiveAt; }
}
