package com.puetsnao.heatmap;

import com.puetsnao.heatmap.infrastructure.batch.SummaryRefreshProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.puetsnao")
@EnableJpaRepositories(basePackages = "com.puetsnao")
@EntityScan(basePackages = "com.puetsnao")
@EnableCaching
@EnableScheduling
@EnableConfigurationProperties({SummaryRefreshProperties.class, com.puetsnao.heatmap.infrastructure.security.RateLimitProperties.class, com.puetsnao.shared.http.CorsProperties.class, com.puetsnao.heatmap.dev.DevSeedProperties.class})
public class HeatmapApplication {

	public static void main(String[] args) {
		SpringApplication.run(HeatmapApplication.class, args);
	}

}
