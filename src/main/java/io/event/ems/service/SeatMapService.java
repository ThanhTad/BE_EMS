package io.event.ems.service;

import io.event.ems.dto.SeatMapDetailDTO;
import io.event.ems.dto.SeatMapListItemDTO;
import io.event.ems.dto.UpdateSeatMapRequestDTO;

import java.util.List;
import java.util.UUID;

public interface SeatMapService {

    List<SeatMapListItemDTO> getSeatMapsByVenue(UUID venueId);

    SeatMapDetailDTO getSeatMapDetails(UUID seatMapId);

    SeatMapDetailDTO createSeatMap(UUID venueId, UpdateSeatMapRequestDTO dto);

    SeatMapDetailDTO updateSeatMap(UUID seatMapId, UpdateSeatMapRequestDTO dto);

    void deleteSeatMap(UUID seatMapId);
}
