package io.event.ems.service.impl;

import io.event.ems.dto.SectionAvailabilityDTO;
import io.event.ems.model.Event;
import io.event.ems.model.SeatSection;
import io.event.ems.model.Ticket;
import io.event.ems.model.TicketSelectionModeEnum;
import io.event.ems.repository.EventSeatStatusRepository;
import io.event.ems.repository.TicketRepository;
import io.event.ems.service.ChatbotIntentService;
import io.event.ems.service.specialized.EventSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotIntentServiceImpl implements ChatbotIntentService {

    private final TicketRepository ticketRepository;
    private final EventSeatStatusRepository eventSeatStatusRepository;
    private final EventSearchService eventSearchService;

    // =================================================================
    // CONSTANTS
    // =================================================================
    private static final int MAX_TICKETS_TO_SHOW = 10;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy");

    // Message templates
    private static final String MSG_EVENT_NOT_FOUND = "Xin lỗi, tôi không tìm thấy sự kiện nào có tên '%s'.";
    private static final String MSG_EVENT_ENDED = "Sự kiện '%s' đã kết thúc.";
    private static final String MSG_ERROR_OCCURRED = "Xin lỗi, đã có lỗi xảy ra khi %s.";
    private static final String MSG_ASK_EVENT_NAME = "Bạn muốn %s của sự kiện nào?";

    // =================================================================
    // MAIN INTENT HANDLERS
    // =================================================================

    @Override
    public String handleGetEventLocation(Map<String, Object> parameters) {
        log.info("Handling GetEventLocation with parameters: {}", parameters);

        String eventName = extractParameter(parameters, "event-name");
        if (eventName == null) {
            return String.format(MSG_ASK_EVENT_NAME, "hỏi về địa điểm");
        }

        try {
            return processEventQuery(eventName, this::buildLocationResponse);
        } catch (Exception e) {
            log.error("Error in handleGetEventLocation for: {}", eventName, e);
            return String.format(MSG_ERROR_OCCURRED, "tìm thông tin sự kiện");
        }
    }

    @Override
    public String handleCheckTicketAvailability(Map<String, Object> parameters) {
        log.info("Handling CheckTicketAvailability with parameters: {}", parameters);

        String eventName = extractParameter(parameters, "event-name");
        String ticketType = extractParameter(parameters, "ticket-type");

        if (eventName == null) {
            return String.format(MSG_ASK_EVENT_NAME, "kiểm tra vé");
        }

        try {
            return processEventQuery(eventName, event -> buildTicketAvailabilityResponse(event, ticketType));
        } catch (Exception e) {
            log.error("Error in handleCheckTicketAvailability for: {}", eventName, e);
            return String.format(MSG_ERROR_OCCURRED, "kiểm tra vé");
        }
    }

    @Override
    public String handleGetEventSchedule(Map<String, Object> parameters) {
        log.info("Handling GetEventSchedule with parameters: {}", parameters);

        String eventName = extractParameter(parameters, "event-name");
        log.info("Event name: {}", eventName);
        if (eventName == null) {
            return String.format(MSG_ASK_EVENT_NAME, "hỏi lịch trình");
        }

        try {
            return processEventQuery(eventName, this::buildScheduleResponse);
        } catch (Exception e) {
            log.error("Error in handleGetEventSchedule for: {}", eventName, e);
            return String.format(MSG_ERROR_OCCURRED, "tìm lịch trình");
        }
    }

    @Override
    public String handleGetTicketPrices(Map<String, Object> parameters) {
        log.info("Handling GetTicketPrices with parameters: {}", parameters);

        String eventName = extractParameter(parameters, "event-name");
        if (eventName == null) {
            return String.format(MSG_ASK_EVENT_NAME, "hỏi giá vé");
        }

        try {
            return processEventQuery(eventName, this::buildPriceResponse);
        } catch (Exception e) {
            log.error("Error in handleGetTicketPrices for: {}", eventName, e);
            return String.format(MSG_ERROR_OCCURRED, "tìm giá vé");
        }
    }

    // =================================================================
    // CORE PROCESSING METHODS
    // =================================================================

    private String processEventQuery(String eventName, EventResponseBuilder responseBuilder) {
        List<Event> events = eventSearchService.findRelevantEvents(eventName);

        if (events.isEmpty()) {
            return String.format(MSG_EVENT_NOT_FOUND, eventName);
        }

        if (events.size() > 1) {
            return buildClarificationResponse(events, eventName);
        }
        return responseBuilder.build(events.get(0));
    }

    private String buildClarificationResponse(List<Event> events, String originalQuery) {
        return ChatbotResponseBuilder.create()
                .addText(String.format("Tôi tìm thấy %d sự kiện có tên tương tự '%s':", events.size(), originalQuery))
                .addNewLine()
                .addEventList(events)
                .addNewLine()
                .addText("Bạn vui lòng hỏi cụ thể hơn về sự kiện nào không?")
                .build();
    }

    // =================================================================
    // RESPONSE BUILDERS
    // =================================================================

    private String buildLocationResponse(Event event) {
        if (event.getVenue() == null) {
            return String.format("Sự kiện '%s' chưa có thông tin địa điểm cụ thể.", event.getTitle());
        }

        return ChatbotResponseBuilder.create()
                .addText(String.format("Sự kiện '%s' sẽ diễn ra tại:", event.getTitle()))
                .addNewLine()
                .addText(String.format("📍 %s", event.getVenue().getName()))
                .addNewLine()
                .addText(String.format("📍 Địa chỉ: %s, %s", event.getVenue().getAddress(), event.getVenue().getCity()))
                .build();
    }

    private String buildScheduleResponse(Event event) {
        return ChatbotResponseBuilder.create()
                .addText(String.format("📅 Lịch trình sự kiện '%s':", event.getTitle()))
                .addNewLine()
                .addDateTime("🕐 Bắt đầu", event.getStartDate())
                .addDateTime("🕐 Kết thúc", event.getEndDate())
                .build();
    }

    private String buildPriceResponse(Event event) {
        List<Ticket> tickets = ticketRepository.findByEventId(event.getId(), PageRequest.of(0, MAX_TICKETS_TO_SHOW)).getContent();

        if (tickets.isEmpty()) {
            return String.format("Sự kiện '%s' chưa có thông tin giá vé.", event.getTitle());
        }

        return ChatbotResponseBuilder.create()
                .addText(String.format("💰 Bảng giá vé sự kiện '%s':", event.getTitle()))
                .addNewLine()
                .addTicketPrices(tickets)
                .build();
    }

    private String buildTicketAvailabilityResponse(Event event, String ticketType) {
        if (isEventEnded(event)) {
            return String.format(MSG_EVENT_ENDED, event.getTitle());
        }

        boolean isSeatedOrZoned = event.getTicketSelectionMode() != TicketSelectionModeEnum.GENERAL_ADMISSION;

        if (ticketType != null) {
            return isSeatedOrZoned
                    ? handleSeatedSpecificTicket(event, ticketType)
                    : handleGeneralAdmissionSpecificTicket(event, ticketType);
        } else {
            return isSeatedOrZoned
                    ? handleSeatedGeneralAvailability(event)
                    : handleGeneralAdmissionGeneralAvailability(event);
        }
    }

    // =================================================================
    // TICKET AVAILABILITY HANDLERS
    // =================================================================

    private String handleGeneralAdmissionSpecificTicket(Event event, String ticketType) {
        return ticketRepository.findByEventIdAndNameContainingIgnoreCase(event.getId(), ticketType)
                .stream()
                .findFirst()
                .map(ticket -> formatTicketAvailability(ticket, event.getTitle()))
                .orElse(String.format("❓ Sự kiện '%s' không có loại vé tên '%s'.", event.getTitle(), ticketType));
    }

    private String handleGeneralAdmissionGeneralAvailability(Event event) {
        List<Ticket> tickets = ticketRepository.findGeneralAdmissionTicketsByEventId(event.getId());
        List<Ticket> availableTickets = tickets.stream()
                .filter(t -> t.getAvailableQuantity() > 0)
                .collect(Collectors.toList());

        if (availableTickets.isEmpty()) {
            return String.format("❌ Rất tiếc, tất cả các loại vé cho sự kiện '%s' đều đã bán hết.", event.getTitle());
        }

        return ChatbotResponseBuilder.create()
                .addText(String.format("✅ Sự kiện '%s' vẫn còn vé!", event.getTitle()))
                .addNewLine()
                .addText("🎫 Các loại vé còn lại:")
                .addNewLine()
                .addAvailableTickets(availableTickets)
                .addNewLine()
                .addText("Bạn muốn đặt loại vé nào?")
                .build();
    }

    private String handleSeatedSpecificTicket(Event event, String ticketType) {
        List<Ticket> matchingTickets = findMatchingTickets(event.getId(), ticketType);

        if (matchingTickets.isEmpty()) {
            return String.format("❓ Sự kiện '%s' không có loại vé nào tên là '%s'.", event.getTitle(), ticketType);
        }

        long availableSeats = calculateAvailableSeats(event.getId(), matchingTickets);
        return formatSeatedAvailabilityMessage(ticketType, availableSeats);
    }

    private String handleSeatedGeneralAvailability(Event event) {
        long availableSeats = eventSeatStatusRepository.countAvailableSeatsByEventId(event.getId());

        if (availableSeats > 0) {
            return String.format("✅ Sự kiện '%s' vẫn còn khoảng %d chỗ trống. Bạn có thể vào trang sự kiện để xem chi tiết các khu vực và chọn vé.",
                    event.getTitle(), availableSeats);
        } else {
            return String.format("❌ Rất tiếc, sự kiện '%s' đã hết tất cả các chỗ ngồi.", event.getTitle());
        }
    }

    // =================================================================
    // HELPER METHODS
    // =================================================================

    private String extractParameter(Map<String, Object> parameters, String key) {
        if (parameters == null || key == null) {
            return null;
        }

        Object value = parameters.get(key);
        if (value == null) {
            return null;
        }

        String strValue = value.toString().trim();
        return strValue.isEmpty() ? null : strValue;
    }

    private boolean isEventEnded(Event event) {
        return event.getStartDate() != null && event.getStartDate().isBefore(LocalDateTime.now());
    }

    private List<Ticket> findMatchingTickets(UUID eventId, String ticketType) {
        return ticketRepository.findByEventIdAndNameContainingIgnoreCase(eventId, ticketType);
    }

    private long calculateAvailableSeats(UUID eventId, List<Ticket> tickets) {
        List<UUID> sectionIds = tickets.stream()
                .map(Ticket::getAppliesToSection)
                .filter(Objects::nonNull)
                .map(SeatSection::getId)
                .distinct()
                .collect(Collectors.toList());

        if (sectionIds.isEmpty()) {
            return 0;
        }

        List<SectionAvailabilityDTO> availabilityResults =
                eventSeatStatusRepository.countAvailableSeatsInSections(eventId, sectionIds);

        return availabilityResults.stream()
                .mapToLong(SectionAvailabilityDTO::getAvailableCapacity)
                .sum();
    }

    private String formatTicketAvailability(Ticket ticket, String eventTitle) {
        if (ticket.getAvailableQuantity() > 0) {
            return String.format("✅ Vẫn còn vé '%s' cho sự kiện '%s'!\n💰 Giá: %,.0f VND\n🎫 Còn lại: %d vé.",
                    ticket.getName(), eventTitle, ticket.getPrice(), ticket.getAvailableQuantity());
        } else {
            return String.format("❌ Rất tiếc, vé '%s' cho sự kiện '%s' đã bán hết.",
                    ticket.getName(), eventTitle);
        }
    }

    private String formatSeatedAvailabilityMessage(String ticketType, long availableSeats) {
        if (availableSeats > 0) {
            return String.format("✅ Vẫn còn khoảng %d chỗ trống cho loại vé '%s' tại các khu vực khác nhau. Bạn có muốn vào trang sự kiện để chọn chỗ không?",
                    availableSeats, ticketType);
        } else {
            return String.format("❌ Rất tiếc, các khu vực áp dụng loại vé '%s' đều đã hết chỗ.", ticketType);
        }
    }

    // =================================================================
    // FUNCTIONAL INTERFACE FOR RESPONSE BUILDING
    // =================================================================

    @FunctionalInterface
    private interface EventResponseBuilder {
        String build(Event event);
    }

    // =================================================================
    // RESPONSE BUILDER UTILITY CLASS
    // =================================================================

    private static class ChatbotResponseBuilder {
        private final StringBuilder response = new StringBuilder();

        private ChatbotResponseBuilder() {
        }

        public static ChatbotResponseBuilder create() {
            return new ChatbotResponseBuilder();
        }

        public ChatbotResponseBuilder addText(String text) {
            response.append(text);
            return this;
        }

        public ChatbotResponseBuilder addNewLine() {
            response.append("\n");
            return this;
        }

        public ChatbotResponseBuilder addDateTime(String label, LocalDateTime dateTime) {
            if (dateTime != null) {
                response.append(String.format("%s: %s\n", label, dateTime.format(DATETIME_FORMATTER)));
            }
            return this;
        }

        public ChatbotResponseBuilder addTicketPrices(List<Ticket> tickets) {
            tickets.forEach(ticket ->
                    response.append(String.format("• %s: %,.0f VND\n", ticket.getName(), ticket.getPrice())));
            return this;
        }

        public ChatbotResponseBuilder addAvailableTickets(List<Ticket> tickets) {
            tickets.forEach(ticket ->
                    response.append(String.format("• %s: %,.0f VND (còn %d vé)\n",
                            ticket.getName(), ticket.getPrice(), ticket.getAvailableQuantity())));
            return this;
        }

        public ChatbotResponseBuilder addEventList(List<Event> events) {
            for (int i = 0; i < events.size(); i++) {
                Event event = events.get(i);
                response.append(String.format("\n%d. %s", i + 1, event.getTitle()));
                if (event.getStartDate() != null) {
                    response.append(String.format(" (ngày %s)", event.getStartDate().format(DATE_FORMATTER)));
                }
            }
            return this;
        }

        public String build() {
            return response.toString();
        }
    }
}