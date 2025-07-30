package io.event.ems.service.impl;

import io.event.ems.dto.*;
import io.event.ems.exception.ResourceNotFoundException;
import io.event.ems.mapper.SeatMapMapper;
import io.event.ems.model.Seat;
import io.event.ems.model.SeatMap;
import io.event.ems.model.SeatSection;
import io.event.ems.model.Venue;
import io.event.ems.repository.SeatMapRepository;
import io.event.ems.repository.VenueRepository;
import io.event.ems.service.SeatMapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class SeatMapServiceImpl implements SeatMapService {

    private final SeatMapRepository seatMapRepository;
    private final VenueRepository venueRepository;
    private final SeatMapMapper seatMapMapper;

    @Override
    @Transactional(readOnly = true)
    public List<SeatMapListItemDTO> getSeatMapsByVenue(UUID venueId) {
        log.info("Fetching seat maps for venue ID: {}", venueId);
        List<SeatMap> seatMaps = seatMapRepository.findByVenueId(venueId);
        return seatMaps.stream().map(seatMapMapper::toListItemDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SeatMapDetailDTO getSeatMapDetails(UUID seatMapId) {
        log.info("Fetching details for seat map ID: {}", seatMapId);
        SeatMap seatMap = seatMapRepository.findByIdWithFullDetails(seatMapId)
                .orElseThrow(() -> new ResourceNotFoundException("SeatMap not found with ID: " + seatMapId));
        return seatMapMapper.toDetailDTO(seatMap);
    }

    @Override
    public SeatMapDetailDTO createSeatMap(UUID venueId, UpdateSeatMapRequestDTO dto) {
        log.info("Creating a new seat map for venue ID: {}", venueId);
        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found with ID: " + venueId));

        SeatMap newSeatMap = new SeatMap();
        newSeatMap.setVenue(venue);
        newSeatMap.setName(dto.getName());
        newSeatMap.setDescription(dto.getDescription());
        newSeatMap.setLayoutData(dto.getLayoutData());

        // Logic upsert sections và seats
        updateSectionsAndSeats(newSeatMap, dto.getSections());

        SeatMap savedSeatMap = seatMapRepository.save(newSeatMap);
        return seatMapMapper.toDetailDTO(savedSeatMap);
    }

    @Override
    public SeatMapDetailDTO updateSeatMap(UUID seatMapId, UpdateSeatMapRequestDTO dto) {
        log.info("Updating seat map with ID: {}", seatMapId);
        SeatMap seatMap = seatMapRepository.findByIdWithFullDetails(seatMapId)
                .orElseThrow(() -> new ResourceNotFoundException("SeatMap not found with ID: " + seatMapId));

        seatMapMapper.updateSeatMapFromDto(dto, seatMap);
        updateSectionsAndSeats(seatMap, dto.getSections());

        SeatMap updatedSeatMap = seatMapRepository.save(seatMap);
        return seatMapMapper.toDetailDTO(updatedSeatMap);
    }

    @Override
    public void deleteSeatMap(UUID seatMapId) {
        log.warn("Deleting seat map with ID: {}", seatMapId);
        if (!seatMapRepository.existsById(seatMapId)) {
            throw new ResourceNotFoundException("SeatMap not found with ID: " + seatMapId);
        }
        // TODO: Add logic to check if seat map is in use by an event
        seatMapRepository.deleteById(seatMapId);
    }

    private void updateSectionsAndSeats(SeatMap seatMap, List<UpdateSectionRequestDTO> sectionDTOs) {
        Map<UUID, SeatSection> existingSectionsMap = seatMap.getSections().stream()
                .collect(Collectors.toMap(SeatSection::getId, Function.identity()));

        Set<UUID> updatedSectionIds = sectionDTOs.stream()
                .map(UpdateSectionRequestDTO::getId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        // Xóa các section không còn tồn tại trong request
        existingSectionsMap.values().stream()
                .filter(section -> !updatedSectionIds.contains(section.getId()))
                .forEach(seatMap::removeSection);

        // Cập nhật hoặc thêm mới section
        for (UpdateSectionRequestDTO sectionDto : sectionDTOs) {
            SeatSection section = existingSectionsMap.getOrDefault(sectionDto.getId(), new SeatSection());
            section.setName(sectionDto.getName());
            section.setLayoutData(sectionDto.getLayoutData());
            section.setSeatMap(seatMap);

            updateSeats(section, sectionDto.getSeats());

            if (section.getId() == null) {
                seatMap.addSection(section);
            }
        }
    }

    private void updateSeats(SeatSection section, List<UpdateSeatRequestDTO> seatDTOs) {
        Map<UUID, Seat> existingSeatsMap = section.getSeats().stream()
                .collect(Collectors.toMap(Seat::getId, Function.identity()));

        Set<UUID> updatedSeatIds = seatDTOs.stream()
                .map(UpdateSeatRequestDTO::getId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        // Xóa ghế
        existingSeatsMap.values().stream()
                .filter(seat -> !updatedSeatIds.contains(seat.getId()))
                .forEach(section::removeSeat);

        // Cập nhật/thêm ghế
        for (UpdateSeatRequestDTO seatDto : seatDTOs) {
            Seat seat = existingSeatsMap.getOrDefault(seatDto.getId(), new Seat());
            seat.setRowLabel(seatDto.getRowLabel());
            seat.setSeatNumber(seatDto.getSeatNumber());
            seat.setSeatType(seatDto.getSeatType());
            seat.setCoordinates(seatDto.getCoordinates());
            seat.setSection(section);

            if (seat.getId() == null) {
                section.addSeat(seat);
            }
        }
        section.setCapacity(section.getSeats().size());
    }
}
