package com.puetsnao.heatmap.dev;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.LocalDate;

@ConfigurationProperties(prefix = "dev.seed")
public record DevSeedProperties(
        Boolean enabled,
        Integer njStationsCount,
        LocalDate day
) {
    public DevSeedProperties {
        if (enabled == null) enabled = false;
        if (njStationsCount == null) njStationsCount = 3000;
        if (day == null) day = LocalDate.of(LocalDate.now().getYear(), 9, 8);
    }
}
