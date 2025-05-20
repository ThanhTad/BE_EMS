package io.event.ems.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import io.event.ems.dto.EventRequestDTO;
import io.event.ems.dto.EventResponseDTO;
import io.event.ems.model.Event;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE, uses = {
        UserMapper.class, CategoryMapper.class })
public interface EventMapper {

    EventResponseDTO toDTO(Event event);

    @Mapping(target = "creator", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "categories", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "currentParticipants", ignore = true)
    Event toEntity(EventRequestDTO eventRequestDTO);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "creator", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "categories", ignore = true)
    @Mapping(target = "currentParticipants", ignore = true)
    void updateEventFromDTO(EventRequestDTO dto, @MappingTarget Event entity);

}
