package com.puetsnao.heatmap.api;

import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping(path = "/api/health", produces = MediaType.APPLICATION_JSON_VALUE)
public class HealthController {

    private final Environment environment;

    public HealthController(Environment environment) {
        this.environment = environment;
    }

    @GetMapping
    public Map<String, Object> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "UP");

        String commitSha = Optional.ofNullable(environment.getProperty("app.commit-sha"))
                .orElseGet(() -> environment.getProperty("info.commit.id"));

        if (commitSha != null && !commitSha.isBlank()) {
            result.put("commitSha", commitSha);
        }
        return result;
    }
}
