package com.puetsnao.heatmap.application;

import com.puetsnao.heatmap.domain.BucketGranularity;
import com.puetsnao.heatmap.domain.Metric;

import java.util.List;

public interface H3HeatmapV2Service {
    // Returns minimal payload pairs [cell, value]
    List<Object[]> queryPairs(Metric metric, int resolution, BucketGranularity bucket, String at, String bbox);
}
