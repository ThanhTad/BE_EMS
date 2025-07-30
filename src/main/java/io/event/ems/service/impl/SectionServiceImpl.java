package io.event.ems.service.impl;

import io.event.ems.dto.SectionDetailDTO;
import io.event.ems.dto.SectionRequestDTO;
import io.event.ems.exception.ResourceNotFoundException;
import io.event.ems.mapper.SectionMapper;
import io.event.ems.model.SeatMap;
import io.event.ems.model.SeatSection;
import io.event.ems.repository.SeatMapRepository;
import io.event.ems.repository.SeatSectionRepository;
import io.event.ems.repository.TicketRepository;
import io.event.ems.service.SectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class SectionServiceImpl implements SectionService {

    private final SeatSectionRepository sectionRepository;
    private final SeatMapRepository seatMapRepository;
    private final TicketRepository ticketRepository;
    private final SectionMapper sectionMapper;


    @Override
    @Transactional(readOnly = true)
    public List<SectionDetailDTO> getSectionsBySeatMap(UUID seatMapId) {
        if (!seatMapRepository.existsById(seatMapId)) {
            throw new ResourceNotFoundException("SeatMap not found with ID: " + seatMapId);
        }
        return sectionRepository.findBySeatMapId(seatMapId).stream()
                .map(sectionMapper::toDetailDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SectionDetailDTO getSectionById(UUID sectionId) {
        return sectionRepository.findById(sectionId)
                .map(sectionMapper::toDetailDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found with ID: " + sectionId));
    }

    @Override
    public SectionDetailDTO createSection(UUID seatMapId, SectionRequestDTO dto) {
        SeatMap seatMap = seatMapRepository.findById(seatMapId)
                .orElseThrow(() -> new ResourceNotFoundException("SeatMap not found with ID: " + seatMapId));

        SeatSection newSection = sectionMapper.toEntity(dto);
        newSection.setSeatMap(seatMap);
        newSection.setCapacity(dto.getCapacity());

        SeatSection savedSection = sectionRepository.save(newSection);
        log.info("Created new section '{}' with ID {} for seat map {}", savedSection.getName(), savedSection.getId(), seatMapId);
        return sectionMapper.toDetailDTO(savedSection);
    }

    @Override
    public SectionDetailDTO updateSection(UUID sectionId, SectionRequestDTO dto) {
        SeatSection existingSection = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found with ID: " + sectionId));

        // Validation: Không cho phép giảm capacity xuống thấp hơn tổng số vé đã tạo.
        int totalTicketsConfigured = ticketRepository.sumTotalQuantityBySectionId(sectionId).orElse(0);
        if (dto.getCapacity() < totalTicketsConfigured) {
            throw new IllegalArgumentException("New capacity cannot be less than the total number of tickets already configured.");
        }

        existingSection.setName(dto.getName());
        existingSection.setLayoutData(dto.getLayoutData());
        existingSection.setCapacity(dto.getCapacity());

        SeatSection updatedSection = sectionRepository.save(existingSection);
        log.info("Updated section '{}' with ID {}", updatedSection.getName(), updatedSection.getId());
        return sectionMapper.toDetailDTO(updatedSection);
    }

    @Override
    public void deleteSection(UUID sectionId) {
        if (!sectionRepository.existsById(sectionId)) {
            throw new ResourceNotFoundException("Section not found with ID: " + sectionId);
        }
        if (ticketRepository.existsByAppliesToSectionId(sectionId)) {
            throw new IllegalArgumentException("Cannot delete zone: Tickets are associated with it.");
        }
        sectionRepository.deleteById(sectionId);
        log.info("Deleted section with ID {}", sectionId);
    }
}
