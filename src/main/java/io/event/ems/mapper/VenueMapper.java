package io.event.ems.mapper;

import io.event.ems.dto.VenueDTO;
import io.event.ems.dto.VenueRequestDTO;
import io.event.ems.model.Venue;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface VenueMapper {

    VenueDTO toDTO(Venue venue);

    @Mapping(target = "updatedAt", ignore = true)
    Venue toEntity(VenueRequestDTO dto);

    @Mapping(target = "updatedAt", ignore = true)
    void updateVenueFromDTO(VenueRequestDTO dto, @MappingTarget Venue entity);
}
