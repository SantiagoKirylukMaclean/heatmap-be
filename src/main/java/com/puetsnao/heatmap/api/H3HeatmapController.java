package com.puetsnao.heatmap.api;

import com.puetsnao.heatmap.application.H3HeatmapService;
import com.puetsnao.heatmap.domain.BucketGranularity;
import com.puetsnao.heatmap.domain.H3CellPoint;
import com.puetsnao.heatmap.domain.Metric;
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
@RequestMapping("/api/heatmap/h3")
@Tag(name = "H3 Heatmap", description = "Aggregated heat map values by H3 cell")
public class H3HeatmapController {

    private final H3HeatmapService service;

    public H3HeatmapController(H3HeatmapService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(
            summary = "Heatmap aggregation by H3 cell",
            description = "Returns aggregated values by H3 cell for the given metric, resolution and bucket granularity (day/hour). The optional 'at' parameter specifies the date (YYYY-MM-DD) or hour (YYYY-MM-DDTHH:00)."
    )
    @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(
                    array = @ArraySchema(schema = @Schema(implementation = H3CellPoint.class)),
                    examples = @ExampleObject(value = "[\n  { \"cell\": \"85283473fffffff\", \"resolution\": 7, \"value\": 2.15 }\n]")
            )
    )
    @ApiResponse(responseCode = "400", description = "Invalid parameters", content = @Content)
    @ApiResponse(responseCode = "500", description = "Unexpected error", content = @Content)
    public ResponseEntity<List<H3CellPoint>> heatmap(
            @Parameter(description = "Aggregation metric", schema = @Schema(allowableValues = {"price", "volume"}), example = "price")
            @RequestParam(name = "metric") String metric,
            @Parameter(description = "H3 resolution (e.g., 5, 7, 9)", example = "7")
            @RequestParam(name = "resolution") int resolution,
            @Parameter(description = "Bucket granularity: day or hour", schema = @Schema(allowableValues = {"day", "hour"}), example = "day")
            @RequestParam(name = "bucket", defaultValue = "day") String bucket,
            @Parameter(description = "Point in time for the bucket: YYYY-MM-DD for day, or YYYY-MM-DDTHH:00 for hour", example = "2025-09-01")
            @RequestParam(name = "at", required = false) String at
    ) {
        Metric m = Metric.from(metric);
        BucketGranularity b = BucketGranularity.from(bucket);
        return ResponseEntity.ok(service.query(m, resolution, b, at));
    }
}
