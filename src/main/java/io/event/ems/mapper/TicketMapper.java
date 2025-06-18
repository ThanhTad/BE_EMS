package io.event.ems.mapper;

import io.event.ems.dto.TicketRequestDTO;
import io.event.ems.dto.TicketResponseDTO;
import io.event.ems.model.Ticket;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface TicketMapper {


    @Mapping(source = "event.id", target = "eventId")
    @Mapping(source = "appliesToSection.id", target = "appliesToSectionId")
    @Mapping(source = "appliesToSection.name", target = "sectionName")
    @Mapping(source = "status.status", target = "statusName")
    TicketResponseDTO toDTO(Ticket ticket);

    Ticket toEntity(TicketRequestDTO ticketRequestDTO);

    void updateTicketFromDTO(TicketRequestDTO dto, @MappingTarget Ticket entity);
}
