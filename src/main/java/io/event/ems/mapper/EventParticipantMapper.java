package io.event.ems.mapper;

import io.event.ems.dto.EventParticipantResponseDTO;
import io.event.ems.model.EventParticipant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EventParticipantMapper {

    @Mapping(source = "event", target = "event")
    @Mapping(source = "user", target = "user")
    @Mapping(source = "status", target = "status")
    EventParticipantResponseDTO toDTO(EventParticipant eventParticipant);

}
