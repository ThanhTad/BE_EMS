package io.event.ems.service;

import io.event.ems.dto.VenueDTO;
import io.event.ems.dto.VenueRequestDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface VenueService {

    Page<VenueDTO> getAllVenues(String keyword, Pageable pageable);

    VenueDTO getVenueById(UUID id);

    VenueDTO createVenue(VenueRequestDTO dto);

    VenueDTO updateVenue(UUID id, VenueRequestDTO dto);

    void deleteVenue(UUID id);
}
