package io.event.ems.service.impl; // Hoặc package tương ứng của bạn

import io.event.ems.dto.*;
import io.event.ems.exception.HoldNotFoundException;
import io.event.ems.exception.InvalidBookingRequestException;
import io.event.ems.exception.ResourceNotFoundException;
import io.event.ems.exception.SeatsNotAvailableException;
import io.event.ems.model.*;
import io.event.ems.repository.*;
import io.event.ems.service.EmailService;
import io.event.ems.service.QrCodeService;
import io.event.ems.service.TicketBookingService;
import io.event.ems.service.specialized.GeneralAdmissionBookingService;
import io.event.ems.service.specialized.PaymentGatewayService;
import io.event.ems.service.specialized.SeatBookingService;
import io.event.ems.util.HoldInfo;
import io.event.ems.util.RedisKeyUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketBookingServiceImpl implements TicketBookingService {

    // === REPOSITORIES ===
    private final EventRepository eventRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final StatusCodeRepository statusCodeRepository;
    private final EventSeatStatusRepository eventSeatStatusRepository;
    private final TicketPurchaseRepository ticketPurchaseRepository;
    private final PurchasedGaTicketRepository purchasedGaTicketRepository;

    // === SPECIALIZED SERVICES ===
    private final SeatBookingService seatBookingService;
    private final GeneralAdmissionBookingService generalAdmissionBookingService;
    private final PaymentGatewayService paymentGatewayService;
    private final QrCodeService qrCodeService;
    private final EmailService emailService;

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public HoldResponseDTO hold(HoldRequestDTO request) {
        log.debug("Processing hold request: {}", request);

        validateHoldRequest(request);

        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + request.getEventId()));

        String mode = event.getTicketSelectionMode().toString();

        if (("RESERVED_SEATING".equals(mode) || "ZONED_ADMISSION".equals(mode)) && request.getSeatIds() != null) {
            return seatBookingService.holdSeats(request);
        } else if ("GENERAL_ADMISSION".equals(mode) && request.getGaItems() != null) {
            return generalAdmissionBookingService.holdTickets(request);
        } else {
            throw new InvalidBookingRequestException("Hold request does not match event's ticket selection mode.");
        }
    }

    private void validateHoldRequest(HoldRequestDTO request) {
        if (request.getEventId() == null) {
            throw new InvalidBookingRequestException("Event ID is required");
        }
        if (request.getUserId() == null) {
            throw new InvalidBookingRequestException("User ID is required");
        }
    }

    @Override
    @Transactional
    public BookingConfirmationDTO checkout(CheckoutRequestDTO request) {
        log.info("Starting checkout process for holdId: {}", request.getHoldId());

        String holdKey = RedisKeyUtil.getHoldKey(request.getHoldId());
        HoldInfo holdInfo = (HoldInfo) redisTemplate.opsForValue().get(holdKey);

        if (holdInfo == null || !holdInfo.getUserId().equals(request.getUserId())) {
            throw new HoldNotFoundException("Hold ID is invalid, has expired, or does not belong to the user.");
        }

        BookingConfirmationDTO confirmation;
        if ("RESERVED_SEATING".equals(holdInfo.getType())) {
            confirmation = processReservedSeatingCheckout(request, holdInfo);
        } else if ("GENERAL_ADMISSION".equals(holdInfo.getType())) {
            confirmation = processGeneralAdmissionCheckout(request, holdInfo);
        } else {
            throw new InvalidBookingRequestException("Invalid hold type: " + holdInfo.getType());
        }

        redisTemplate.delete(holdKey);
        log.info("Checkout successful for holdId: {}. Hold key deleted from Redis.", request.getHoldId());

        return confirmation;
    }

    private BookingConfirmationDTO processReservedSeatingCheckout(CheckoutRequestDTO request, HoldInfo holdInfo) {
        log.debug("Processing checkout for reserved seating: {}", request.getHoldId());

        List<EventSeatStatus> heldSeatStatuses = eventSeatStatusRepository.findAllByIdInWithDetailsAndLock(holdInfo.getSeatIds());
        if (heldSeatStatuses.size() != holdInfo.getSeatIds().size()) {
            throw new SeatsNotAvailableException("One or more seats were booked by another user during hold time.");
        }

        Map<UUID, Ticket> sectionToTicketMap = getTicketsForSections(holdInfo.getEventId(), heldSeatStatuses);
        BigDecimal totalPrice = calculateTotalPrice(heldSeatStatuses, sectionToTicketMap);

        String transactionId = paymentGatewayService.processPayment(request.getPaymentToken(), totalPrice);

        TicketPurchase savedPurchase = createAndSaveTicketPurchase(request.getUserId(), holdInfo.getEventId(), totalPrice, transactionId);

        updateSeatStatuses(heldSeatStatuses, savedPurchase, sectionToTicketMap);

        prepareAndSendConfirmationEmail(savedPurchase);

        return buildConfirmationDTO(savedPurchase);
    }

    private BookingConfirmationDTO processGeneralAdmissionCheckout(CheckoutRequestDTO request, HoldInfo holdInfo) {
        log.debug("Processing checkout for general admission: {}", request.getHoldId());

        Map<UUID, Ticket> ticketMap = getTicketsForGaItems(holdInfo.getGaItems());
        BigDecimal totalPrice = calculateTotalPriceForGa(holdInfo.getGaItems(), ticketMap);

        String transactionId = paymentGatewayService.processPayment(request.getPaymentToken(), totalPrice);

        TicketPurchase savedPurchase = createAndSaveTicketPurchase(request.getUserId(), holdInfo.getEventId(), totalPrice, transactionId);

        updateGaTicketsAndRedis(holdInfo.getGaItems(), savedPurchase, ticketMap);

        prepareAndSendConfirmationEmail(savedPurchase);

        return buildConfirmationDTO(savedPurchase);
    }

    // ========================================================================
    // === CÁC HÀM HELPER ĐƯỢC TÁCH NHỎ ===
    // ========================================================================

    private TicketPurchase createAndSaveTicketPurchase(UUID userId, UUID eventId, BigDecimal totalPrice, String transactionId) {
        TicketPurchase purchase = new TicketPurchase();
        purchase.setUser(userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId)));
        purchase.setEvent(eventRepository.findById(eventId).orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + eventId)));
        purchase.setTotalPrice(totalPrice);
        purchase.setTransactionId(transactionId);
        purchase.setPaymentMethod("CARD"); // Giả lập
        purchase.setStatus(statusCodeRepository.findByEntityTypeAndStatus("TICKET_PURCHASE", "SUCCESS")
                .orElseThrow(() -> new IllegalStateException("Status 'SUCCESS' for TICKET_PURCHASE not configured.")));
        return ticketPurchaseRepository.save(purchase);
    }

    // --- Helper cho Reserved Seating ---

    private Map<UUID, Ticket> getTicketsForSections(UUID eventId, List<EventSeatStatus> seatStatuses) {
        // Bước 1: Lấy danh sách unique section IDs
        List<UUID> sectionIds = seatStatuses.stream()
                .map(ess -> ess.getSeat().getSection().getId())
                .distinct().toList();

        // Bước 2: Lấy tất cả tickets trong 1 query thay vì query từng section
        return ticketRepository.findByEventIdAndSectionIdsWithDetails(eventId, sectionIds)
                .stream().collect(Collectors.toMap(
                        ticket -> ticket.getAppliesToSection().getId(),
                        Function.identity()
                ));
    }

    private BigDecimal calculateTotalPrice(List<EventSeatStatus> seatStatuses, Map<UUID, Ticket> sectionToTicketMap) {
        BigDecimal subTotal = BigDecimal.ZERO;
        for (EventSeatStatus seatStatus : seatStatuses) {
            Ticket ticket = sectionToTicketMap.get(seatStatus.getSeat().getSection().getId());
            if (ticket == null) throw new ResourceNotFoundException("Pricing ticket not found for section.");
            subTotal = subTotal.add(ticket.getPrice());
        }
        return subTotal.add(subTotal.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_UP)); // 5% fee
    }

    private void updateSeatStatuses(List<EventSeatStatus> statuses, TicketPurchase purchase, Map<UUID, Ticket> sectionToTicketMap) {
        for (EventSeatStatus ess : statuses) {
            Ticket ticket = sectionToTicketMap.get(ess.getSeat().getSection().getId());
            ess.setStatus("SOLD");
            ess.setTicketPurchase(purchase);
            ess.setTicket(ticket);
            ess.setPriceAtPurchase(ticket.getPrice());
        }
        eventSeatStatusRepository.saveAll(statuses);
    }

    // --- Helper cho General Admission ---

    private Map<UUID, Ticket> getTicketsForGaItems(List<HoldRequestDTO.GeneralAdmissionHoldItem> gaRequests) {
        List<UUID> ticketIds = gaRequests.stream().map(HoldRequestDTO.GeneralAdmissionHoldItem::getTicketId).toList();
        return ticketRepository.findAllById(ticketIds).stream()
                .collect(Collectors.toMap(Ticket::getId, Function.identity()));
    }

    private BigDecimal calculateTotalPriceForGa(List<HoldRequestDTO.GeneralAdmissionHoldItem> gaRequests, Map<UUID, Ticket> ticketMap) {
        BigDecimal subTotal = BigDecimal.ZERO;
        for (HoldRequestDTO.GeneralAdmissionHoldItem req : gaRequests) {
            Ticket ticket = ticketMap.get(req.getTicketId());
            subTotal = subTotal.add(ticket.getPrice().multiply(new BigDecimal(req.getQuantity())));
        }
        return subTotal.add(subTotal.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_UP)); // 5% fee
    }

    private void updateGaTicketsAndRedis(List<HoldRequestDTO.GeneralAdmissionHoldItem> gaRequests, TicketPurchase purchase, Map<UUID, Ticket> ticketMap) {
        for (HoldRequestDTO.GeneralAdmissionHoldItem req : gaRequests) {
            int updatedRows = ticketRepository.decreaseAvailableQuantity(req.getTicketId(), req.getQuantity());
            if (updatedRows == 0) {
                throw new SeatsNotAvailableException("Tickets for '" + ticketMap.get(req.getTicketId()).getName() + "' sold out during checkout.");
            }

            PurchasedGaTicket pgt = new PurchasedGaTicket();
            pgt.setTicketPurchase(purchase);
            pgt.setTicket(ticketMap.get(req.getTicketId()));
            pgt.setQuantity(req.getQuantity());
            pgt.setPricePerTicket(ticketMap.get(req.getTicketId()).getPrice());
            purchasedGaTicketRepository.save(pgt);

            String heldCountKey = RedisKeyUtil.getGeneralAdmissionHeldCountKey(req.getTicketId());
            redisTemplate.opsForValue().decrement(heldCountKey, req.getQuantity());
        }
    }

    // --- Helper chung cho Email và DTO Response ---

    private void prepareAndSendConfirmationEmail(TicketPurchase purchase) {
        Map<String, byte[]> inlineQrImages = new HashMap<>();
        List<Map<String, Object>> ticketDetailsForEmail = new ArrayList<>();

        processReservedSeatsForEmail(purchase, inlineQrImages, ticketDetailsForEmail);
        processGaTicketsForEmail(purchase, inlineQrImages, ticketDetailsForEmail);

        emailService.sendGroupTicketConfirmation(
                purchase.getUser().getEmail(),
                purchase.getUser().getFullName(),
                purchase.getTransactionId(),
                purchase.getEvent().getTitle(),
                ticketDetailsForEmail,
                inlineQrImages
        );
    }

    private void processReservedSeatsForEmail(TicketPurchase purchase,
                                              Map<String, byte[]> inlineQrImages,
                                              List<Map<String, Object>> ticketDetailsForEmail) {
        List<EventSeatStatus> soldSeats = eventSeatStatusRepository.findByTicketPurchaseId(purchase.getId());
        for (EventSeatStatus seat : soldSeats) {
            byte[] qrImage = qrCodeService.generateQrCodeForReservedSeat(seat);
            String cid = "qr_" + seat.getId();
            inlineQrImages.put(cid, qrImage);

            Map<String, Object> ticketInfo = new HashMap<>();
            ticketInfo.put("name", seat.getTicket().getName());
            ticketInfo.put("quantity", 1);
            ticketInfo.put("price", seat.getPriceAtPurchase());
            ticketInfo.put("details", String.format("Khu %s - Hàng %s - Ghế %s",
                    seat.getSeat().getSection().getName(), seat.getSeat().getRowLabel(), seat.getSeat().getSeatNumber()));
            ticketInfo.put("cid", cid);
            ticketDetailsForEmail.add(ticketInfo);
        }
    }

    private void processGaTicketsForEmail(TicketPurchase purchase,
                                          Map<String, byte[]> inlineQrImages,
                                          List<Map<String, Object>> ticketDetailsForEmail) {
        List<PurchasedGaTicket> purchasedGaTickets = purchasedGaTicketRepository.findByTicketPurchaseId(purchase.getId());
        for (PurchasedGaTicket gaTicket : purchasedGaTickets) {
            byte[] qrImage = qrCodeService.generateQrCodeForGaTicketGroup(gaTicket);
            String cid = "qr_" + gaTicket.getId();
            inlineQrImages.put(cid, qrImage);

            Map<String, Object> ticketInfo = new HashMap<>();
            ticketInfo.put("name", gaTicket.getTicket().getName());
            ticketInfo.put("quantity", gaTicket.getQuantity());
            ticketInfo.put("price", gaTicket.getPricePerTicket());
            ticketInfo.put("details", "Vé vào cửa tự do");
            ticketInfo.put("cid", cid);
            ticketDetailsForEmail.add(ticketInfo);
        }
    }


    private BookingConfirmationDTO buildConfirmationDTO(TicketPurchase purchase) {
        List<PurchasedItemDTO> purchasedItems = new ArrayList<>();

        List<EventSeatStatus> soldSeats = eventSeatStatusRepository.findByTicketPurchaseId(purchase.getId());
        soldSeats.forEach(seat -> purchasedItems.add(
                PurchasedItemDTO.builder()
                        .ticketName(seat.getTicket().getName())
                        .price(seat.getPriceAtPurchase())
                        .quantity(1)
                        .seatInfo(PurchasedItemDTO.SeatInfo.builder()
                                .sectionName(seat.getSeat().getSection().getName())
                                .rowLabel(seat.getSeat().getRowLabel())
                                .seatNumber(seat.getSeat().getSeatNumber())
                                .build())
                        .build()
        ));

        List<PurchasedGaTicket> purchasedGaTickets = purchasedGaTicketRepository.findByTicketPurchaseId(purchase.getId());
        purchasedGaTickets.forEach(ga -> purchasedItems.add(
                PurchasedItemDTO.builder()
                        .ticketName(ga.getTicket().getName())
                        .price(ga.getPricePerTicket().multiply(new BigDecimal(ga.getQuantity())))
                        .quantity(ga.getQuantity())
                        .seatInfo(null)
                        .build()
        ));

        return BookingConfirmationDTO.builder()
                .purchaseId(purchase.getId())
                .eventName(purchase.getEvent().getTitle())
                .purchaseDate(Instant.from(purchase.getPurchaseDate()))
                .totalPrice(purchase.getTotalPrice())
                .purchasedItems(purchasedItems)
                .build();
    }
}