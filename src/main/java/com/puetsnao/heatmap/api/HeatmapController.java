package com.puetsnao.heatmap.api;

import com.puetsnao.heatmap.application.HeatmapService;
import com.puetsnao.heatmap.domain.HeatPoint;
import com.puetsnao.heatmap.domain.Metric;
import com.puetsnao.heatmap.domain.Period;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/heatmap")
@Tag(name = "Heatmap", description = "Aggregated heat map values by US state")
public class HeatmapController {

    private final HeatmapService heatmapService;

    public HeatmapController(HeatmapService heatmapService) {
        this.heatmapService = heatmapService;
    }

    @GetMapping
    @Operation(
            summary = "Heatmap aggregation by state",
            description = "Returns a list of heat points aggregated by state for the given metric and period."
    )
    @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(
                    array = @ArraySchema(schema = @Schema(implementation = HeatPoint.class)),
                    examples = @ExampleObject(value = "[\n  { \"state\": \"TX\", \"lat\": 29.76, \"lon\": -95.36, \"value\": 2.15 },\n  { \"state\": \"IL\", \"lat\": 41.87, \"lon\": -87.62, \"value\": 2.09 }\n]")
            )
    )
    @ApiResponse(responseCode = "400", description = "Invalid metric or period", content = @Content)
    @ApiResponse(responseCode = "500", description = "Unexpected error", content = @Content)
    public ResponseEntity<List<HeatPoint>> heatmap(
            @Parameter(
                    description = "Aggregation metric",
                    schema = @Schema(allowableValues = {"price", "volume"}),
                    example = "price"
            )
            @RequestParam(name = "metric") String metric,
            @Parameter(
                    description = "Aggregation period",
                    schema = @Schema(allowableValues = {"last30d"}),
                    example = "last30d"
            )
            @RequestParam(name = "period", defaultValue = "last30d") String period) {
        Metric m = Metric.from(metric);
        Period p = Period.from(period);
        return ResponseEntity.ok(heatmapService.heatmap(m, p));
    }
}
