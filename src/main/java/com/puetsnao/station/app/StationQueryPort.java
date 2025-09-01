package com.puetsnao.station.app;

import java.util.List;

public interface StationQueryPort {
    List<StationLocation> stations();

    record StationLocation(String state, double latitude, double longitude) {}
}