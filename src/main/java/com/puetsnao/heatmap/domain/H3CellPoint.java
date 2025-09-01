package com.puetsnao.heatmap.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "H3CellPoint", description = "Aggregated value for an H3 cell")
public record H3CellPoint(
        @Schema(description = "H3 cell address", example = "85283473fffffff") String cell,
        @Schema(description = "H3 resolution", example = "7") int resolution,
        @Schema(description = "Aggregated numeric value: avg price or total volume depending on metric", example = "2.15") double value
) {
}
