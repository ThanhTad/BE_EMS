package io.event.ems.mapper;

import io.event.ems.dto.SeatMapSummaryDTO;
import io.event.ems.model.SeatMap;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SeatMapMapper {

    @Mapping(source = "venue.name", target = "venueName")
    @Mapping(source = "venue.id", target = "venueId")
    SeatMapSummaryDTO toSummaryDTO(SeatMap seatMap);
}
