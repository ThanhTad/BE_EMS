package io.event.ems.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import io.event.ems.dto.TicketDTO;
import io.event.ems.model.Ticket;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface TicketMapper {

    
    @Mapping(source = "event.id", target = "eventId")
    @Mapping(source = "status.id", target = "statusId")
    TicketDTO toDTO(Ticket ticket);

    @Mapping(source = "eventId", target = "event.id")
    @Mapping(source = "statusId", target = "status.id")
    Ticket toEntity(TicketDTO ticketDTO);

    @Mapping(source = "eventId", target = "event.id")
    @Mapping(source = "statusId", target = "status.id")
    void updateTicketFromDTO(TicketDTO dto, @MappingTarget Ticket entity);
    
}
