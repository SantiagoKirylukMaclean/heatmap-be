package com.puetsnao.heatmap.api;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class HealthControllerTests {

    private MockMvc mockMvc;

    @Test
    public void returnsOk() throws Exception {
        var env = new org.springframework.mock.env.MockEnvironment();
        this.mockMvc = org.springframework.test.web.servlet.setup.MockMvcBuilders
                .standaloneSetup(new HealthController(env))
                .setMessageConverters(new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter())
                .build();

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
    }
}
