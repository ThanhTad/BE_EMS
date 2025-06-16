package io.event.ems.service;

import io.event.ems.dto.SeatMapLayoutDTO;
import io.event.ems.dto.SeatMapSummaryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface SeatMapService {
    UUID createSeatMapFromLayout(SeatMapLayoutDTO seatMapLayoutDTO);

    SeatMapLayoutDTO getSeatMapLayout(UUID id);

    Page<SeatMapSummaryDTO> getAllSeatMaps(Pageable pageable);

    Page<SeatMapSummaryDTO> getSeatMapsByVenueId(UUID venueID, Pageable pageable);

    void deleteSeatMap(UUID id);
}
