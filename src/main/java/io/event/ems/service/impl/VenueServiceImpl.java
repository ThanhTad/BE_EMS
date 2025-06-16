package io.event.ems.service.impl;

import io.event.ems.dto.VenueDTO;
import io.event.ems.exception.ResourceNotFoundException;
import io.event.ems.mapper.VenueMapper;
import io.event.ems.model.Venue;
import io.event.ems.repository.VenueRepository;
import io.event.ems.service.VenueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VenueServiceImpl implements VenueService {

    private final VenueRepository venueRepository;
    private final VenueMapper venueMapper;


    @Override
    @Transactional
    public VenueDTO createVenue(VenueDTO venueDTO) {
        if (venueRepository.existsByName(venueDTO.getName())) {
            throw new IllegalArgumentException("Venue with name: " + venueDTO.getName() + " already exists");
        }
        Venue venue = venueMapper.toEntity(venueDTO);
        Venue savedVenue = venueRepository.save(venue);
        return venueMapper.toDto(savedVenue);
    }

    @Override
    @Transactional(readOnly = true)
    public VenueDTO getVenueById(UUID id) {
        Venue venue = venueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found with id: " + id));
        return venueMapper.toDto(venue);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VenueDTO> getAllVenues(Pageable pageable) {
        return venueRepository.findAll(pageable)
                .map(venueMapper::toDto);
    }

    @Override
    @Transactional
    public VenueDTO updateVenue(UUID id, VenueDTO venueDTO) {
        Venue existVenue = venueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found with id: " + id));
        venueRepository.findByName(venueDTO.getName()).ifPresent(foundVenue -> {
            if (!foundVenue.getId().equals(id)) {
                throw new IllegalArgumentException("Venue name '" + venueDTO.getName() + "' is already in use.");
            }
        });
        venueMapper.updateVenueFromDto(venueDTO, existVenue);
        Venue updatedVenue = venueRepository.save(existVenue);
        return venueMapper.toDto(updatedVenue);
    }

    @Override
    @Transactional
    public void deleteVenue(UUID id) {
        if (venueRepository.existsById(id)) {
            throw new ResourceNotFoundException("Venue not found with id: " + id);
        }
        venueRepository.deleteById(id);
    }
}
