package io.event.ems.service.impl;

import io.event.ems.dto.VenueDTO;
import io.event.ems.dto.VenueRequestDTO;
import io.event.ems.exception.ResourceNotFoundException;
import io.event.ems.mapper.VenueMapper;
import io.event.ems.model.Venue;
import io.event.ems.repository.EventRepository;
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
@Slf4j
@RequiredArgsConstructor
@Transactional
public class VenueServiceImpl implements VenueService {

    private final VenueRepository venueRepository;
    private final VenueMapper venueMapper;
    private final EventRepository eventRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<VenueDTO> getAllVenues(String keyword, Pageable pageable) {
        log.info("Fetching venues with keyword: '{}'", keyword);
        Page<Venue> venuePage;
        if (keyword != null && !keyword.isBlank()) {
            venuePage = venueRepository.findByNameContainingIgnoreCase(keyword, pageable);
        } else {
            venuePage = venueRepository.findAll(pageable);
        }
        return venuePage.map(venueMapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public VenueDTO getVenueById(UUID id) {
        log.info("Fetching venue with ID: {}", id);
        Venue venue = venueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found with ID: " + id));
        return venueMapper.toDTO(venue);
    }

    @Override
    public VenueDTO createVenue(VenueRequestDTO dto) {
        log.info("Creating new venue with name: {}", dto.getName());
        if (venueRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new IllegalArgumentException("A venue with this name already exists.");
        }
        Venue newVenue = venueMapper.toEntity(dto);
        Venue savedVenue = venueRepository.save(newVenue);
        log.info("Successfully created venue with ID: {}", savedVenue.getId());
        return venueMapper.toDTO(savedVenue);
    }

    @Override
    public VenueDTO updateVenue(UUID id, VenueRequestDTO dto) {
        log.info("Updating venue with ID: {}", id);
        Venue existingVenue = venueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found with ID: " + id));

        // Kiểm tra nếu đổi tên sang một tên đã tồn tại (và không phải là chính nó)
        venueRepository.findByNameIgnoreCaseAndIdNot(dto.getName(), id).ifPresent(duplicate -> {
            if (!duplicate.getId().equals(id)) {
                throw new IllegalArgumentException("A venue with this name already exists.");
            }
        });

        venueMapper.updateVenueFromDTO(dto, existingVenue);
        Venue updatedVenue = venueRepository.save(existingVenue);
        log.info("Successfully updated venue with ID: {}", id);
        return venueMapper.toDTO(updatedVenue);
    }

    @Override
    public void deleteVenue(UUID id) {
        log.warn("Attempting to delete venue with ID: {}", id);
        if (!venueRepository.existsById(id)) {
            throw new ResourceNotFoundException("Venue not found with ID: " + id);
        }
        // TODO: Thêm logic kiểm tra xem địa điểm này có đang được sử dụng bởi sự kiện nào không
        if (eventRepository.existsByVenueId(id)) {
            throw new IllegalArgumentException(
                    "Cannot delete venue because it's associated with one or more events. " +
                            "Please unassign the venue from all events first."
            );
        }
        // Nếu có, có thể không cho xóa hoặc phải xóa mềm (soft delete).
        venueRepository.deleteById(id);
        log.info("Successfully deleted venue with ID: {}", id);
    }
}
