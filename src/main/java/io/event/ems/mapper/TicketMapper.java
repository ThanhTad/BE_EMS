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

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "event", ignore = true)
    @Mapping(target = "appliesToSection", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "availableQuantity", ignore = true)
    @Mapping(target = "maxPerPurchase", ignore = true)
    Ticket toEntity(TicketRequestDTO ticketRequestDTO);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "event", ignore = true)
    @Mapping(target = "appliesToSection", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "availableQuantity", ignore = true)
    @Mapping(target = "maxPerPurchase", ignore = true)
    void updateTicketFromDTO(TicketRequestDTO dto, @MappingTarget Ticket entity);
}
