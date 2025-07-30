package io.event.ems.service;

import io.event.ems.dto.SectionDetailDTO;
import io.event.ems.dto.SectionRequestDTO;

import java.util.List;
import java.util.UUID;

public interface SectionService {

    List<SectionDetailDTO> getSectionsBySeatMap(UUID seatMapId);

    SectionDetailDTO getSectionById(UUID sectionId);

    SectionDetailDTO createSection(UUID seatMapId, SectionRequestDTO dto);

    SectionDetailDTO updateSection(UUID sectionId, SectionRequestDTO dto);

    void deleteSection(UUID sectionId);
}
