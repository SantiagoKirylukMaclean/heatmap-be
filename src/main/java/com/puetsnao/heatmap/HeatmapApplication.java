package com.puetsnao.heatmap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.puetsnao")
public class HeatmapApplication {

	public static void main(String[] args) {
		SpringApplication.run(HeatmapApplication.class, args);
	}

}
