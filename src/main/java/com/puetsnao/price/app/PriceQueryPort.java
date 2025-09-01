package com.puetsnao.price.app;

import java.time.LocalDate;
import java.util.Map;

public interface PriceQueryPort {
    Map<String, Double> averagePriceByState(LocalDate from, LocalDate to);
}