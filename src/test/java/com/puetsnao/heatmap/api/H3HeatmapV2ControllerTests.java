package com.puetsnao.heatmap.api;

import com.puetsnao.heatmap.application.H3HeatmapV2Service;
import com.puetsnao.heatmap.domain.Metric;
import com.puetsnao.shared.http.DefaultEtagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class H3HeatmapV2ControllerTests {

    private MockMvc mockMvc;
    private H3HeatmapV2Service service;

    @BeforeEach
    void setup() {
        service = Mockito.mock(H3HeatmapV2Service.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new H3HeatmapV2Controller(service, new DefaultEtagService()))
                .setMessageConverters(new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    void returnsMinimalPairs() throws Exception {
        List<Object[]> pairs = List.of(
                new Object[]{"85283473fffffff", 2.15},
                new Object[]{"85283477fffffff", 1.10}
        );
        Mockito.when(service.queryPairs(Mockito.eq(Metric.PRICE), Mockito.eq(7), Mockito.any(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(pairs);

        mockMvc.perform(get("/api/v2/heatmap/h3")
                        .param("metric", "price")
                        .param("resolution", "7")
                        .param("bucket", "day")
                        .param("at", "2025-09-01")
                        .param("bbox", "39.0,-75.8,41.4,-73.9")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0][0]").value("85283473fffffff"))
                .andExpect(jsonPath("$[0][1]").value(2.15));
    }
}
