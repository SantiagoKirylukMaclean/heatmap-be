package com.puetsnao.heatmap.infrastructure.summary;

public record PriceInputs(double priceSum, long priceCount) {
    public PriceInputs add(PriceInputs other) {
        return new PriceInputs(this.priceSum + other.priceSum, this.priceCount + other.priceCount);
    }
}