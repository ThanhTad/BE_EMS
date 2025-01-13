package io.event.ems.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import io.event.ems.dto.TicketPurchaseDTO;
import io.event.ems.model.TicketPurchase;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface TicketPurchaseMapper {

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "ticket.id", target = "ticketId")
    @Mapping(source = "status.id", target = "statusId")
    TicketPurchaseDTO toDTO(TicketPurchase ticketPurchase);

    @Mapping(source = "userId", target = "user.id")
    @Mapping(source = "ticketId", target = "ticket.id")
    @Mapping(source = "statusId", target = "status.id")
    TicketPurchase toEntity(TicketPurchaseDTO ticketPurchaseDTO);

    @Mapping(source = "userId", target = "user.id")
    @Mapping(source = "ticketId", target = "ticket.id")
    @Mapping(source = "statusId", target = "status.id")
    void updateTicketPurchaseFromDTO(TicketPurchaseDTO ticketPurchaseDTO, @MappingTarget TicketPurchase ticketPurchase);


}
