package com.puetsnao.sales.app;

import java.time.LocalDate;
import java.util.Map;

public interface SalesQueryPort {
    Map<String, Double> totalVolumeByState(LocalDate from, LocalDate to);
}