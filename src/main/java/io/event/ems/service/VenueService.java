package io.event.ems.service;

import io.event.ems.dto.VenueDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface VenueService {

    VenueDTO createVenue(VenueDTO venueDTO);

    VenueDTO getVenueById(UUID id);

    Page<VenueDTO> getAllVenues(Pageable pageable);

    VenueDTO updateVenue(UUID id, VenueDTO venueDTO);

    void deleteVenue(UUID id);


}
