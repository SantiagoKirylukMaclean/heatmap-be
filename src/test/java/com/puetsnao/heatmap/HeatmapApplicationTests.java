package com.puetsnao.heatmap;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class HeatmapApplicationTests {

	@Test
	void contextLoads() {
		System.out.println("[DEBUG_LOG] Context loaded with 'test' profile");
	}

}
