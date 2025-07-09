package io.event.ems.service;

import java.util.Map;

public interface ChatbotIntentService {

    String handleGetEventLocation(Map<String, Object> parameters);

    String handleCheckTicketAvailability(Map<String, Object> parameters);

    String handleGetEventSchedule(Map<String, Object> parameters);

    String handleGetTicketPrices(Map<String, Object> parameters);
}
