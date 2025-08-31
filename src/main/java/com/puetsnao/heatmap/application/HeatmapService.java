package com.puetsnao.heatmap.application;

import com.puetsnao.heatmap.domain.HeatPoint;
import com.puetsnao.heatmap.domain.Metric;
import com.puetsnao.heatmap.domain.Period;

import java.util.List;

public interface HeatmapService {
    List<HeatPoint> heatmap(Metric metric, Period period);
}
