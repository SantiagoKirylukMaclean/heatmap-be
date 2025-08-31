package com.puetsnao.heatmap.api;

import com.puetsnao.heatmap.application.HeatmapService;
import com.puetsnao.heatmap.domain.HeatPoint;
import com.puetsnao.heatmap.domain.Metric;
import com.puetsnao.heatmap.domain.Period;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HeatmapControllerTests {

    private MockMvc mockMvc;
    private HeatmapService heatmapService;

    @BeforeEach
    void setup() {
        heatmapService = Mockito.mock(HeatmapService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new HeatmapController(heatmapService))
                .setMessageConverters(new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    void returnsHeatmapPoints() throws Exception {
        List<HeatPoint> points = List.of(
                new HeatPoint("STA", 11.0, 22.0, 150.0),
                new HeatPoint("STB", -5.0, 30.0, 300.0)
        );
        when(heatmapService.heatmap(Metric.PRICE, Period.LAST30D)).thenReturn(points);

        mockMvc.perform(get("/api/heatmap")
                        .param("metric", "price")
                        .param("period", "last30d")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].state").value("STA"))
                .andExpect(jsonPath("$[0].lat").value(11.0))
                .andExpect(jsonPath("$[0].lon").value(22.0))
                .andExpect(jsonPath("$[0].value").value(150.0));
    }
}
