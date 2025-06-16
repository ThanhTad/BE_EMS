package io.event.ems.mapper;

import io.event.ems.dto.VenueDTO;
import io.event.ems.model.Venue;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface VenueMapper {

    VenueDTO toDto(Venue venue);

    Venue toEntity(VenueDTO venueDTO);

    void updateVenueFromDto(VenueDTO dto, @MappingTarget Venue entity);
}
