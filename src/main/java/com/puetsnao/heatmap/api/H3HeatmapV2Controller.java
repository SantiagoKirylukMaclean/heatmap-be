package com.puetsnao.heatmap.api;

import com.puetsnao.heatmap.application.H3HeatmapV2Service;
import com.puetsnao.heatmap.domain.BucketGranularity;
import com.puetsnao.heatmap.domain.Metric;
import com.puetsnao.shared.http.EtagService;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/v2/heatmap/h3")
@Tag(name = "H3 Heatmap v2", description = "Aggregated heat map values by H3 cell from H10 children with bbox filter")
public class H3HeatmapV2Controller {

    private final H3HeatmapV2Service service;
    private final EtagService etagService;

    public H3HeatmapV2Controller(H3HeatmapV2Service service, EtagService etagService) {
        this.service = service;
        this.etagService = etagService;
    }

    @GetMapping
    @Operation(
            summary = "Heatmap aggregation by H3 cell (v2)",
            description = "Aggregates from H10 cells to requested resolution using AVG (price) or SUM (volume), filters by bbox, and returns minimal [cell,value] pairs."
    )
    @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(
                    array = @ArraySchema(schema = @Schema(implementation = Object[].class)),
                    examples = @ExampleObject(value = "[[\"85283473fffffff\", 2.15]]")
            )
    )
    @ApiResponse(responseCode = "400", description = "Invalid parameters", content = @Content)
    @ApiResponse(responseCode = "500", description = "Unexpected error", content = @Content)
    public ResponseEntity<List<Object[]>> heatmap(
            @Parameter(description = "Aggregation metric", schema = @Schema(allowableValues = {"price", "volume"}), example = "price")
            @RequestParam(name = "metric") String metric,
            @Parameter(description = "H3 resolution (<= 10; aggregated from H10)", example = "7")
            @RequestParam(name = "resolution") int resolution,
            @Parameter(description = "Bucket granularity: day or hour", schema = @Schema(allowableValues = {"day", "hour"}), example = "day")
            @RequestParam(name = "bucket", defaultValue = "day") String bucket,
            @Parameter(description = "Point in time for the bucket: YYYY-MM-DD for day, or YYYY-MM-DDTHH:00 for hour", example = "2025-09-01")
            @RequestParam(name = "at", required = false) String at,
            @Parameter(description = "Bounding box: minLat,minLon,maxLat,maxLon", example = "39.0,-75.8,41.4,-73.9")
            @RequestParam(name = "bbox") String bbox,
            @RequestHeader(name = "If-None-Match", required = false) String ifNoneMatch
    ) {
        Metric m = Metric.from(metric);
        BucketGranularity b = BucketGranularity.from(bucket);
        String version = switch (b) {
            case DAY -> (at == null || at.isBlank()) ? LocalDate.now().toString() : at;
            case HOUR -> {
                if (at == null || at.isBlank()) {
                    yield LocalDateTime.now().withMinute(0).withSecond(0).withNano(0).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                }
                yield at;
            }
        };
        String etag = etagService.buildWeak("heatmap:h3:v2", m.name().toLowerCase(), String.valueOf(resolution), b.name().toLowerCase(), version, bbox);
        if (etagService.matches(ifNoneMatch, etag)) {
            return ResponseEntity.status(304).eTag(etag).build();
        }
        List<Object[]> payload = service.queryPairs(m, resolution, b, at, bbox);
        return ResponseEntity.ok().eTag(etag).body(payload);
    }
}
