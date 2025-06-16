package io.event.ems.service.impl;

import io.event.ems.dto.SeatLayoutDTO;
import io.event.ems.dto.SeatMapLayoutDTO;
import io.event.ems.dto.SeatMapSummaryDTO;
import io.event.ems.dto.ZoneLayoutDTO;
import io.event.ems.exception.ResourceNotFoundException;
import io.event.ems.mapper.SeatMapMapper;
import io.event.ems.model.Seat;
import io.event.ems.model.SeatMap;
import io.event.ems.model.SeatSection;
import io.event.ems.model.Venue;
import io.event.ems.repository.SeatMapRepository;
import io.event.ems.repository.SeatRepository;
import io.event.ems.repository.SeatSectionRepository;
import io.event.ems.repository.VenueRepository;
import io.event.ems.service.SeatMapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatMapServiceImpl implements SeatMapService {

    private final VenueRepository venueRepository;
    private final SeatMapRepository seatMapRepository;
    private final SeatSectionRepository seatSectionRepository;
    private final SeatRepository seatRepository;
    private SeatMapMapper seatMapMapper;


    @Override
    @Transactional
    public UUID createSeatMapFromLayout(SeatMapLayoutDTO seatMapLayoutDTO) {
        Venue venue = venueRepository.findById(seatMapLayoutDTO.getVenueId())
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found with id: " + seatMapLayoutDTO.getVenueId()));

        SeatMap seatMap = new SeatMap();
        seatMap.setName(seatMapLayoutDTO.getMapName());
        seatMap.setVenue(venue);
        SeatMap savedSeatMap = seatMapRepository.save(seatMap);

        Map<String, SeatSection> sectionMap = new HashMap<>();
        if (seatMapLayoutDTO.getZones() != null && !seatMapLayoutDTO.getZones().isEmpty()) {
            List<SeatSection> sectionsToSave = seatMapLayoutDTO.getZones().stream().map(zoneDTO -> {
                SeatSection section = new SeatSection();
                section.setName(zoneDTO.getName());
                section.setCapacity(zoneDTO.getCapacity());
                section.setLayoutData(zoneDTO.getLayoutData());
                section.setSeatMap(savedSeatMap);
                return section;
            }).toList();

            List<SeatSection> savedSections = seatSectionRepository.saveAll(sectionsToSave);
            savedSections.forEach(section -> sectionMap.put(section.getName(), section));
        }

        if (seatMapLayoutDTO.getSeats() != null && !seatMapLayoutDTO.getSeats().isEmpty()) {
            List<Seat> seatsToSave = seatMapLayoutDTO.getSeats().stream().map(seatDTO -> {
                SeatSection section = sectionMap.get(seatDTO.getSectionName());
                if (section == null) {
                    throw new ResourceNotFoundException("SeatSection not found with name: " + seatDTO.getSectionName());
                }
                Seat seat = new Seat();
                seat.setRowLabel(seatDTO.getRowLabel());
                seat.setSeatNumber(seatDTO.getSeatNumber());
                seat.setSeatType(seatDTO.getSeatType());
                seat.setCoordinates(seatDTO.getCoordinates());
                seat.setSection(section);
                return seat;
            }).toList();

            seatRepository.saveAll(seatsToSave);
        }
        return savedSeatMap.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public SeatMapLayoutDTO getSeatMapLayout(UUID id) {
        SeatMap seatMap = seatMapRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SeatMap not found with id: " + id));

        SeatMapLayoutDTO seatMapLayoutDTO = new SeatMapLayoutDTO();
        seatMapLayoutDTO.setMapName(seatMap.getName());
        seatMapLayoutDTO.setVenueId(seatMap.getVenue().getId());

        List<ZoneLayoutDTO> zones = seatMap.getSections().stream().map(section -> {
            ZoneLayoutDTO zoneLayoutDTO = new ZoneLayoutDTO();
            zoneLayoutDTO.setName(section.getName());
            zoneLayoutDTO.setCapacity(section.getCapacity());
            zoneLayoutDTO.setLayoutData(section.getLayoutData());
            return zoneLayoutDTO;
        }).toList();
        seatMapLayoutDTO.setZones(zones);

        List<SeatLayoutDTO> seats = seatMap.getSections().stream().flatMap(section -> section.getSeats().stream().map(seat -> {
            SeatLayoutDTO seatLayoutDTO = new SeatLayoutDTO();
            seatLayoutDTO.setSectionName(section.getName());
            seatLayoutDTO.setRowLabel(seat.getRowLabel());
            seatLayoutDTO.setSeatNumber(seat.getSeatNumber());
            seatLayoutDTO.setSeatType(seat.getSeatType());
            seatLayoutDTO.setCoordinates(seat.getCoordinates());
            return seatLayoutDTO;
        })).toList();
        seatMapLayoutDTO.setSeats(seats);

        return seatMapLayoutDTO;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SeatMapSummaryDTO> getAllSeatMaps(Pageable pageable) {
        return seatMapRepository.findAll(pageable).map(seatMapMapper::toSummaryDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SeatMapSummaryDTO> getSeatMapsByVenueId(UUID venueID, Pageable pageable) {
        return seatMapRepository.findByVenueId(venueID, pageable).map(seatMapMapper::toSummaryDTO);
    }

    @Override
    @Transactional
    public void deleteSeatMap(UUID id) {
        if (!seatMapRepository.existsById(id)) {
            throw new ResourceNotFoundException("SeatMap not found with id: " + id);
        }
        seatMapRepository.deleteById(id);
    }
}
