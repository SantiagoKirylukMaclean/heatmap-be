package com.puetsnao.heatmap.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "HeatPoint", description = "Aggregated value for a US state and its representative coordinates")
public record HeatPoint(
        @Schema(description = "US state code (ISO 3166-2 or USPS)", example = "TX") String state,
        @Schema(description = "Latitude of the state's representative location", example = "29.76") double lat,
        @Schema(description = "Longitude of the state's representative location", example = "-95.36") double lon,
        @Schema(description = "Aggregated numeric value: avg price or total volume depending on metric", example = "2.15") double value
) {
}
