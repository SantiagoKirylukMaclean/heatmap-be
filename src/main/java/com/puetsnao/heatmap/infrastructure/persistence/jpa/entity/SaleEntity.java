package com.puetsnao.heatmap.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sales")
public class SaleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "station_id", nullable = false)
    private StationEntity station;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @Column(name = "sold_at", nullable = false)
    private LocalDateTime soldAt;

    @Column(name = "volume", nullable = false, precision = 14, scale = 3)
    private BigDecimal volume;

    public Long getId() { return id; }
    public StationEntity getStation() { return station; }
    public ProductEntity getProduct() { return product; }
    public LocalDateTime getSoldAt() { return soldAt; }
    public BigDecimal getVolume() { return volume; }

    public void setStation(StationEntity station) { this.station = station; }
    public void setProduct(ProductEntity product) { this.product = product; }
    public void setSoldAt(LocalDateTime soldAt) { this.soldAt = soldAt; }
    public void setVolume(BigDecimal volume) { this.volume = volume; }
}
