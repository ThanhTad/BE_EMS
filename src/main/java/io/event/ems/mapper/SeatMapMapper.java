package io.event.ems.mapper;

import io.event.ems.dto.SeatDetailDTO;
import io.event.ems.dto.SeatMapDetailDTO;
import io.event.ems.dto.SeatMapSummaryDTO;
import io.event.ems.dto.SectionDetailDTO;
import io.event.ems.model.Seat;
import io.event.ems.model.SeatMap;
import io.event.ems.model.SeatSection;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SeatMapMapper {

    @Mapping(source = "section.id", target = "sectionId")
    @Mapping(source = "section.name", target = "sectionName")
    SeatDetailDTO toSeatDetailDTO(Seat seat);

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
}
