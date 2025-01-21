package io.event.ems.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import io.event.ems.dto.EventParticipantDTO;
import io.event.ems.model.EventParticipant;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface EventParticipantMapper {

    @Mapping(target = "eventId", source = "event.id")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "statusId", source = "status.id")
    EventParticipantDTO toDTO(EventParticipant eventParticipant);

    @Mapping(target = "event.id", source = "eventId")
    @Mapping(target = "user.id", source = "userId")
    @Mapping(target = "status.id", source = "statusId")
    EventParticipant toEntity(EventParticipantDTO dto);

    @Mapping(target = "event.id", source = "eventId")
    @Mapping(target = "user.id", source = "userId")
    @Mapping(target = "status.id", source = "statusId")
    void updateEntityFromDTO(EventParticipantDTO dto, @MappingTarget EventParticipant entity);

}
