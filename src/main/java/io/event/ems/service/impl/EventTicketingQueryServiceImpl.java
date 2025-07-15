package io.event.ems.service.impl;

import io.event.ems.dto.*;
import io.event.ems.exception.ResourceNotFoundException;
import io.event.ems.mapper.EventMapper;
import io.event.ems.model.*;
import io.event.ems.repository.EventRepository;
import io.event.ems.repository.EventSeatStatusRepository;
import io.event.ems.repository.SeatMapRepository;
import io.event.ems.repository.TicketRepository;
import io.event.ems.service.EventTicketingQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class EventTicketingQueryServiceImpl implements EventTicketingQueryService {

    private final EventRepository eventRepository;

    private final TicketRepository ticketRepository;

    private final SeatMapRepository seatMapRepository;

    private final EventSeatStatusRepository eventSeatStatusRepository;

    private final EventMapper eventMapper;

    @Override
    public EventTicketingResponseDTO getEventTicketingBySlug(String slug) {
        log.info("Getting event ticketing for slug: {}", slug);
        Event event = eventRepository.findBySlugWithDetails(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with slug: " + slug));

        if (!event.getIsPublic() || !"APPROVED".equalsIgnoreCase(event.getStatus().getStatus())) {
            throw new IllegalArgumentException("Event is not public or not approved");
        }

        EventTicketingResponseDTO responseDTO = eventMapper.eventToEventTicketingResponseDto(event);

        switch (event.getTicketSelectionMode()) {
            case GENERAL_ADMISSION -> responseDTO.setTicketingData(getGeneralAdmissionTicketing(event.getId()));
            case ZONED_ADMISSION -> responseDTO.setTicketingData(getZonedAdmissionTicketing(event));
            case RESERVED_SEATING -> responseDTO.setTicketingData(getReservedSeatingTicketing(event));
        }
        return responseDTO;
    }

    private ReservedSeatingDTO getReservedSeatingTicketing(Event event) {
        if (event.getSeatMap() == null) {
            throw new IllegalStateException("Event with RESERVED_SEATING must have a seat map.");
        }

        SeatMap seatMap = seatMapRepository.findByIdWithSectionsAndSeats(event.getSeatMap().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Seat map not found for event"));

        List<EventSeatStatus> seatStatuses = eventSeatStatusRepository.findByEventIdAndSeatMapId(event.getId(), seatMap.getId());

        Map<UUID, EventSeatStatus> statusMap = seatStatuses.stream()
                .collect(Collectors.toMap(ess -> ess.getSeat().getId(), ess -> ess));

        List<SectionDTO> sections = seatMap.getSections().stream()
                .map(section -> buildSectionDto(event.getId(), section, statusMap))
                .collect(Collectors.toList());

        return new ReservedSeatingDTO(seatMap.getId(), seatMap.getName(), sections, seatMap.getLayoutData());
    }

    private ZonedAdmissionDTO getZonedAdmissionTicketing(Event event) {
        if (event.getSeatMap() == null) {
            throw new IllegalStateException("Event with ZONED_ADMISSION must have a seat map.");
        }

        SeatMap seatMap = seatMapRepository.findByIdWithSections(event.getSeatMap().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Seat map not found for event"));

        // Lấy TẤT CẢ các loại vé của sự kiện một lần duy nhất để tối ưu
        List<Ticket> allEventTickets = ticketRepository.findByEventId(event.getId(), Pageable.unpaged()).getContent();

        // Nhóm các vé theo sectionId để tra cứu dễ dàng
        Map<UUID, List<Ticket>> ticketsBySection = allEventTickets.stream()
                .filter(t -> t.getAppliesToSection() != null)
                .collect(Collectors.groupingBy(t -> t.getAppliesToSection().getId()));

        List<ZoneDTO> zones = seatMap.getSections().stream()
                // Truyền map vé vào hàm build để nó không cần query lại DB
                .map(section -> buildZoneDTO(section, ticketsBySection.get(section.getId())))
                .toList();

        return new ZonedAdmissionDTO(seatMap.getId(), seatMap.getName(), zones, seatMap.getLayoutData());
    }

    private GeneralAdmissionDTO getGeneralAdmissionTicketing(UUID eventId) {
        List<Ticket> tickets = ticketRepository.findGeneralAdmissionTicketsByEventId(eventId);
        List<TicketTypeDTO> ticketDTOs = tickets.stream()
                .map(this::buildTicketTypeDTO)
                .collect(Collectors.toList());

        int totalCapacity = tickets.stream()
                .mapToInt(t -> t.getTotalQuantity() != null ? t.getTotalQuantity() : 0)
                .sum();

        int availableCapacity = tickets.stream()
                .mapToInt(t -> t.getAvailableQuantity() != null ? t.getAvailableQuantity() : 0)
                .sum();

        return new GeneralAdmissionDTO(ticketDTOs, totalCapacity, availableCapacity);
    }

    private ZoneDTO buildZoneDTO(SeatSection section, List<Ticket> sectionTickets) {
        // Nếu không có vé nào được gán cho khu vực này, nó không thể bán
        if (sectionTickets == null || sectionTickets.isEmpty()) {
            return new ZoneDTO(
                    section.getId(),
                    section.getName(),
                    section.getCapacity(),
                    0, // 0 vé còn trống
                    Collections.emptyList(),
                    "UNAVAILABLE", // Trạng thái mới: không có vé để bán
                    section.getLayoutData()
            );
        }

        // Đếm số ghế còn trống trong khu vực này cho sự kiện này
        int availableCapacity = sectionTickets.stream()
                .mapToInt(t -> t.getAvailableQuantity() != null ? t.getAvailableQuantity() : 0)
                .sum();

        // Tìm các loại vé áp dụng cho khu vực này
        List<TicketTypeDTO> ticketDTOs = sectionTickets.stream()
                .map(this::buildTicketTypeDTO)
                .toList();

        // 3. Xác định trạng thái của cả khu vực
        String status;
        if (availableCapacity > 0) {
            status = "AVAILABLE";
        } else {
            // Kiểm tra xem tổng số vé ban đầu có lớn hơn 0 không.
            // Nếu không có vé nào được cấu hình, nó là "UNAVAILABLE", không phải "SOLD_OUT".
            boolean wasEverOnSale = sectionTickets.stream().anyMatch(t -> t.getTotalQuantity() != null && t.getTotalQuantity() > 0);
            status = wasEverOnSale ? "SOLD_OUT" : "UNAVAILABLE";
        }

        // 4. Trả về DTO hoàn chỉnh
        return new ZoneDTO(
                section.getId(),
                section.getName(),
                section.getCapacity(),
                availableCapacity,
                ticketDTOs,
                status,
                section.getLayoutData()
        );
    }

    private SectionDTO buildSectionDto(UUID eventId, SeatSection section, Map<UUID, EventSeatStatus> statusMap) {
        List<SeatDTO> seats = section.getSeats() != null
                ? section.getSeats().stream()
                .map(seat -> buildSeatDto(seat, statusMap.get(seat.getId())))
                .collect(Collectors.toList())
                : Collections.emptyList();

        int availableCapacity = (int) seats.stream().filter(seat -> "available".equalsIgnoreCase(seat.getStatus())).count();

        List<Ticket> sectionTickets = ticketRepository.findByEventIdAndSectionId(eventId, section.getId());
        List<TicketTypeDTO> ticketDTOs = sectionTickets.stream()
                .map(this::buildTicketTypeDTO)
                .toList();

        return new SectionDTO(
                section.getId(),
                section.getName(),
                section.getCapacity(),
                availableCapacity,
                seats,
                ticketDTOs,
                section.getLayoutData()
        );
    }

    private SeatDTO buildSeatDto(Seat seat, EventSeatStatus status) {
        SeatDTO dto = new SeatDTO();
        dto.setSeatId(seat.getId());
        dto.setRowLabel(seat.getRowLabel());
        dto.setSeatNumber(seat.getSeatNumber());
        dto.setSeatType(seat.getSeatType());
        dto.setCoordinates(seat.getCoordinates());

        if (status != null) {
            dto.setStatus(status.getStatus());
            dto.setHeldUntil(status.getHeldUntil());
            if (status.getTicket() != null) {
                dto.setPrice(status.getPriceAtPurchase() != null ? status.getPriceAtPurchase() : status.getTicket().getPrice());
                dto.setTicketTypeName(status.getTicket().getName());
                dto.setTicketId(status.getTicket().getId());
            }
        } else {
            // If no status exists for this seat/event, it's considered available by default.
            // This might happen if event_seat_status records are created on-demand.
            dto.setStatus("available");
        }

        return dto;
    }

    private TicketTypeDTO buildTicketTypeDTO(Ticket ticket) {
        boolean isOnSale = ticket.getAvailableQuantity() != null && ticket.getAvailableQuantity() > 0;

        return new TicketTypeDTO(ticket.getId(),
                ticket.getName(),
                ticket.getPrice(),
                ticket.getDescription(),
                ticket.getTotalQuantity(),
                ticket.getAvailableQuantity(),
                ticket.getMaxPerPurchase(),
                ticket.getSaleStartDate(),
                ticket.getSaleEndDate(),
                isOnSale);
    }
}
