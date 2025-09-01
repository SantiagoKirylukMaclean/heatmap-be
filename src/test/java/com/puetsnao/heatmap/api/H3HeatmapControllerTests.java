package com.puetsnao.heatmap.api;

import com.puetsnao.heatmap.application.H3HeatmapService;
import com.puetsnao.heatmap.domain.H3CellPoint;
import com.puetsnao.heatmap.domain.Metric;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class H3HeatmapControllerTests {

    private MockMvc mockMvc;
    private H3HeatmapService service;

    @BeforeEach
    void setup() {
        service = Mockito.mock(H3HeatmapService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new H3HeatmapController(service))
                .setMessageConverters(new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    void returnsH3Cells() throws Exception {
        List<H3CellPoint> points = List.of(
                new H3CellPoint("85283473fffffff", 7, 2.15),
                new H3CellPoint("85283477fffffff", 7, 1.10)
        );
        when(service.query(Mockito.eq(Metric.PRICE), anyInt(), any(), Mockito.anyString())).thenReturn(points);

        mockMvc.perform(get("/api/heatmap/h3")
                        .param("metric", "price")
                        .param("resolution", "7")
                        .param("bucket", "day")
                        .param("at", "2025-09-01")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cell").value("85283473fffffff"))
                .andExpect(jsonPath("$[0].resolution").value(7))
                .andExpect(jsonPath("$[0].value").value(2.15));
    }
}
