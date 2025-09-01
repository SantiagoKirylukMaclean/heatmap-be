package com.puetsnao.station.infrastructure;

import com.puetsnao.station.app.StationQueryPort;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class DefaultStationQueryAdapter implements StationQueryPort {

    private final StationRepository stationRepository;

    public DefaultStationQueryAdapter(StationRepository stationRepository) {
        this.stationRepository = stationRepository;
    }

    @Override
    public List<StationLocation> stations() {
        return stationRepository.findAll().stream()
                .map(e -> new StationLocation(
                        e.getState(),
                        e.getLatitude().doubleValue(),
                        e.getLongitude().doubleValue()
                ))
                .toList();
    }
}
