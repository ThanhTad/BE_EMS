package io.event.ems.service;

import io.event.ems.dto.EventTicketingResponseDTO;

public interface EventTicketingQueryService {

    EventTicketingResponseDTO getEventTicketingBySlug(String slug);
}
