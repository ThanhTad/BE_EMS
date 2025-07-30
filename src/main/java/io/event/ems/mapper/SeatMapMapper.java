package io.event.ems.mapper;

import io.event.ems.dto.*;
import io.event.ems.model.Seat;
import io.event.ems.model.SeatMap;
import io.event.ems.model.SeatSection;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.util.Collection;
import java.util.List;

@Mapper(componentModel = "spring")
public interface SeatMapMapper {

    @Mapping(source = "section.id", target = "sectionId")
    @Mapping(source = "section.name", target = "sectionName")
    SeatDetailDTO toSeatDetailDTO(Seat seat);

    @Mapping(target = "sectionCount", source = "sections", qualifiedByName = "countSections")
    SeatMapListItemDTO toListItemDTO(SeatMap seatMap);

    @Mapping(source = "venue.id", target = "venueId")
    @Mapping(source = "venue.name", target = "venueName")
    SeatMapDetailDTO toDetailDTO(SeatMap seatMap);

    List<SectionDetailDTO> toSectionDetailDTOs(List<SeatSection> sections);

    List<SeatDetailDTO> toSeatDetailDTOs(List<Seat> seats);

    List<SeatDetailDTO> toSeatDetailDTOList(List<Seat> seats);

    @Mapping(source = "seatMap.id", target = "seatMapId")
    SectionDetailDTO toSectionDetailDTO(SeatSection seatSection);

    List<SectionDetailDTO> toSectionDetailDTOList(List<SeatSection> seatSections);

    @Mapping(source = "venue.id", target = "venueId")
    @Mapping(source = "venue.name", target = "venueName")
    SeatMapDetailDTO toSeatMapDetailDTO(SeatMap seatMap);

    @Mapping(source = "venue.name", target = "venueName")
    @Mapping(source = "venue.id", target = "venueId")
    SeatMapSummaryDTO toSummaryDTO(SeatMap seatMap);

    // --- From DTO to Entity (for Updates) ---
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "venue", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateSeatMapFromDto(UpdateSeatMapRequestDTO dto, @MappingTarget SeatMap entity);

    @Mapping(target = "capacity", ignore = true)
    @Mapping(target = "seatMap", ignore = true)
    @Mapping(target = "seats", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    SeatSection toSeatSectionEntity(UpdateSectionRequestDTO dto);

    @Mapping(target = "section", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Seat toSeatEntity(UpdateSeatRequestDTO dto);

    @Named("countSections")
    default int countSections(Collection<SeatSection> sections) {
        return sections == null ? 0 : sections.size();
    }
}
