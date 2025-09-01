package com.puetsnao.heatmap.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@Tag(name = "Health", description = "Health check endpoint for liveness and basic build info")
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    private final Environment environment;

    public HealthController(Environment environment) {
        this.environment = environment;
    }

    @GetMapping
    @Operation(
            summary = "Health check",
            description = "Returns application liveness status and optional commit SHA if available"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Service is UP",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = java.util.Map.class),
                    examples = @ExampleObject(value = "{\n  \"status\": \"UP\",\n  \"commitSha\": \"abc1234\"\n}")
            )
    )
    public Map<String, Object> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "UP");

        String commitSha = Optional.ofNullable(environment.getProperty("app.commit-sha"))
                .orElseGet(() -> environment.getProperty("info.commit.id"));

        if (commitSha != null && !commitSha.isBlank()) {
            result.put("commitSha", commitSha);
        }
        log.info("health endpoint invoked status=UP commitSha={}", commitSha);
        return result;
    }
}
