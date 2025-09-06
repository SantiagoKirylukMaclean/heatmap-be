package com.puetsnao.heatmap.api;

import com.puetsnao.heatmap.application.H3HeatmapService;
import com.puetsnao.heatmap.domain.H3CellPoint;
import com.puetsnao.heatmap.domain.Metric;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.puetsnao.shared.http.CorsProperties;
import com.puetsnao.shared.http.DefaultEtagService;
import com.puetsnao.shared.http.WebCorsConfig;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.TestPropertySource;

@WebMvcTest(controllers = H3HeatmapController.class)
@AutoConfigureMockMvc
@EnableConfigurationProperties(CorsProperties.class)
@Import({WebCorsConfig.class, DefaultEtagService.class})
@TestPropertySource(properties = {
        "app.cors.allowed-origins=http://localhost:5173"
})
class CorsConfigurationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private H3HeatmapService h3HeatmapService;

    @Test
    void allowsCorsOnGetHeatmapEndpoint() throws Exception {
        Mockito.when(h3HeatmapService.query(Mockito.eq(Metric.PRICE), anyInt(), any(), Mockito.anyString()))
                .thenReturn(List.of(new H3CellPoint("85283473fffffff", 7, 1.0)));

        mockMvc.perform(get("/api/heatmap/h3")
                        .param("metric", "price")
                        .param("resolution", "7")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Origin", "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void allowsPreflightOptions() throws Exception {
        mockMvc.perform(options("/api/heatmap/h3")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Methods", org.hamcrest.Matchers.containsString("GET")));
    }
}
