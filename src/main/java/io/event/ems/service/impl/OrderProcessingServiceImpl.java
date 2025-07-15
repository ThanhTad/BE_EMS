package io.event.ems.service.impl;

import io.event.ems.dto.EmailDetails;
import io.event.ems.dto.PaymentDetailsDTO;
import io.event.ems.dto.TicketHoldRequestDTO;
import io.event.ems.dto.TicketPurchaseConfirmationDTO;
import io.event.ems.exception.ResourceNotFoundException;
import io.event.ems.model.*;
import io.event.ems.repository.*;
import io.event.ems.service.EmailService;
import io.event.ems.service.OrderProcessingService;
import io.event.ems.service.PaymentGatewayService;
import io.event.ems.service.QrCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderProcessingServiceImpl implements OrderProcessingService {

    private final TicketPurchaseRepository ticketPurchaseRepository;

    private final EventSeatStatusRepository eventSeatStatusRepository;

    private final TicketRepository ticketRepository;

    private final EventRepository eventRepository;

    private final UserRepository userRepository;

    private final StatusCodeRepository statusCodeRepository;

    private final PurchasedGaTicketRepository purchasedGaTicketRepository;

    private final PaymentGatewayService paymentGatewayService;

    private final EmailService emailService;

    private final QrCodeService qrCodeService;

    private final TransactionTemplate transactionTemplate;

    private static final BigDecimal SERVICE_FEE_PERCENTAGE = new BigDecimal("0.05");

    @Override
    public TicketPurchaseConfirmationDTO finalizePurchase(HoldData holdData, PaymentDetailsDTO paymentDetails) {
        log.info("Finalizing purchase for hold [ID={}]", holdData.getHoldId());

        // --- BƯỚC 1: TÍNH TOÁN GIÁ (BÊN NGOÀI TRANSACTION) ---
        // Việc này chỉ đọc dữ liệu, không cần transaction ghi.
        BigDecimal totalPrice = calculateTotalPrice(holdData);

        // --- BƯỚC 2: XỬ LÝ THANH TOÁN (BÊN NGOÀI TRANSACTION) ---
        // Gọi đến dịch vụ ngoài (I/O intensive), không nên giữ DB transaction mở.
        String transactionId = paymentGatewayService.processPayment(paymentDetails.getPaymentToken(), totalPrice);

        // --- BƯỚC 3: GHI DỮ LIỆU VÀO DATABASE (BÊN TRONG TRANSACTION) ---
        // Sử dụng TransactionTemplate để đảm bảo chỉ khối code này là transactional.
        TicketPurchase savedPurchase = transactionTemplate.execute(status ->
                processAndSavePurchase(holdData, totalPrice, transactionId)
        );

        if (savedPurchase == null) {
            // Logic hoàn tiền nếu transaction thất bại
            // paymentGatewayService.refund(transactionId);
            throw new RuntimeException("Failed to save purchase details to database after successful payment.");
        }

        // --- BƯỚC 4: GỬI EMAIL XÁC NHẬN (BÊN NGOÀI TRANSACTION) ---
        try {
            prepareAndSendConfirmationEmail(savedPurchase);
        } catch (Exception e) {
            log.error("CRITICAL: Purchase [ID={}] was successful, but confirmation email failed to send.", savedPurchase.getId(), e);
        }

        return new TicketPurchaseConfirmationDTO(
                savedPurchase.getId(),
                "Your purchase was successful. A confirmation email has been sent.",
                savedPurchase.getPurchaseDate()
        );
    }

    // ========================================================================
    // === CÁC HÀM HELPER CHÍNH THEO TỪNG BƯỚC ===
    // ========================================================================

    private TicketPurchase processAndSavePurchase(HoldData holdData, BigDecimal totalPrice, String transactionId) {
        TicketPurchase purchase = createAndSaveTicketPurchase(holdData, totalPrice, transactionId);

        var request = holdData.getRequest();
        if (request.getSelectionMode() == TicketSelectionModeEnum.RESERVED_SEATING) {
            updateSeatStatuses(purchase, request.getSeatIds());
        } else {
            updateGaTickets(purchase, request.getGaItems());
        }
        return purchase;
    }

    private void prepareAndSendConfirmationEmail(TicketPurchase purchase) {
        log.info("Preparing confirmation email for purchase [ID={}]", purchase.getId());

        // Lấy lại thông tin chi tiết của đơn hàng vừa tạo để đảm bảo dữ liệu mới nhất
        List<EventSeatStatus> soldSeats = eventSeatStatusRepository.findByTicketPurchaseId(purchase.getId());
        List<PurchasedGaTicket> purchasedGaTickets = purchasedGaTicketRepository.findByTicketPurchaseId(purchase.getId());

        Map<String, byte[]> inlineQrImages = new HashMap<>();
        List<EmailDetails.TicketInfo> ticketDetailsForEmail = new ArrayList<>();

        // 1. Xử lý vé ghế ngồi
        for (EventSeatStatus seat : soldSeats) {
            byte[] qrImage = qrCodeService.generateQrCodeForReservedSeat(seat);
            String cid = "qr_" + seat.getId();
            inlineQrImages.put(cid, qrImage);

            ticketDetailsForEmail.add(new EmailDetails.TicketInfo(
                    seat.getTicket().getName(),
                    1,
                    seat.getPriceAtPurchase(),
                    String.format("Khu %s - Hàng %s - Ghế %s",
                            seat.getSeat().getSection().getName(), seat.getSeat().getRowLabel(), seat.getSeat().getSeatNumber()),
                    cid
            ));
        }

        // 2. Xử lý vé GA/Zoned
        for (PurchasedGaTicket gaTicket : purchasedGaTickets) {
            byte[] qrImage = qrCodeService.generateQrCodeForGaTicket(gaTicket);
            String cid = "qr_" + gaTicket.getId();
            inlineQrImages.put(cid, qrImage);

            ticketDetailsForEmail.add(new EmailDetails.TicketInfo(
                    gaTicket.getTicket().getName(),
                    gaTicket.getQuantity(),
                    gaTicket.getPricePerTicket(),
                    "Vé vào cửa tự do", // Hoặc tên Zone nếu là Zoned Admission
                    cid
            ));
        }

        // 3. Tạo DTO chi tiết và gửi đi
        EmailDetails emailDetails = new EmailDetails(
                purchase.getUser().getEmail(),
                purchase.getUser().getFullName(),
                purchase.getEvent().getTitle(),
                purchase.getTransactionId(),
                ticketDetailsForEmail,
                inlineQrImages
        );

        emailService.sendPurchaseConfirmationEmail(emailDetails);
    }

    // ========================================================================
    // === CÁC HÀM HELPER CHI TIẾT HƠN ===
    // ========================================================================

    private BigDecimal calculateTotalPrice(HoldData holdData) {
        TicketHoldRequestDTO request = holdData.getRequest();
        BigDecimal subTotal;

        if (request.getSelectionMode() == TicketSelectionModeEnum.RESERVED_SEATING) {
            subTotal = calculateSubTotalForReserved(holdData.getEventId(), request.getSeatIds());
        } else if (request.getSelectionMode() == TicketSelectionModeEnum.GENERAL_ADMISSION || request.getSelectionMode() == TicketSelectionModeEnum.ZONED_ADMISSION) {
            subTotal = calculateSubTotalForGa(request.getGaItems());
        } else {
            throw new IllegalArgumentException("Unsupported calculation mode: " + request.getSelectionMode());
        }

        // Tính và cộng thêm phí dịch vụ
        BigDecimal serviceFee = subTotal.multiply(SERVICE_FEE_PERCENTAGE).setScale(2, RoundingMode.HALF_UP);
        log.debug("Calculated SubTotal: {}, ServiceFee: {}, Total: {}", subTotal, serviceFee, subTotal.add(serviceFee));

        return subTotal.add(serviceFee);
    }

    private BigDecimal calculateSubTotalForReserved(UUID eventId, List<UUID> seatIds) {
        List<EventSeatStatus> statuses = eventSeatStatusRepository.findAllByEventIdAndSeatIdInWithTicket(eventId, seatIds);

        if (statuses.size() != seatIds.size()) {
            throw new ResourceNotFoundException("Could not find all requested seats for price calculation.");
        }

        return calculateSubTotal(statuses);
    }

    private BigDecimal calculateSubTotal(List<EventSeatStatus> statuses) {
        BigDecimal subTotal = BigDecimal.ZERO;
        for (EventSeatStatus status : statuses) {
            Ticket ticket = status.getTicket();
            if (ticket == null || ticket.getPrice() == null) {
                throw new IllegalStateException("Seat status is missing pricing information.");
            }
            subTotal = subTotal.add(ticket.getPrice());
        }
        return subTotal;
    }

    private BigDecimal calculateSubTotalForGa(List<TicketHoldRequestDTO.GeneralAdmissionItem> gaItems) {
        List<UUID> ticketIds = gaItems.stream().map(TicketHoldRequestDTO.GeneralAdmissionItem::getTicketId).toList();
        Map<UUID, Ticket> ticketMap = ticketRepository.findAllById(ticketIds)
                .stream().collect(Collectors.toMap(Ticket::getId, Function.identity()));

        if (ticketMap.size() != ticketIds.size()) {
            throw new ResourceNotFoundException("One or more ticket types could not be found during price calculation.");
        }

        BigDecimal subTotal = BigDecimal.ZERO;
        for (var item : gaItems) {
            Ticket ticket = ticketMap.get(item.getTicketId());
            subTotal = subTotal.add(ticket.getPrice().multiply(new BigDecimal(item.getQuantity())));
        }

        return subTotal;
    }

    private TicketPurchase createAndSaveTicketPurchase(HoldData holdData, BigDecimal totalPrice, String transactionId) {
        User user = userRepository.findById(holdData.getUserId()).orElseThrow(() -> new ResourceNotFoundException("User not found: " + holdData.getUserId()));
        Event event = eventRepository.findById(holdData.getEventId()).orElseThrow(() -> new ResourceNotFoundException("Event not found: " + holdData.getEventId()));
        StatusCode successStatus = statusCodeRepository.findByEntityTypeAndStatus("TICKET_PURCHASE", "SUCCESS")
                .orElseThrow(() -> new IllegalStateException("Status 'SUCCESS' for TICKET_PURCHASE not configured."));

        TicketPurchase purchase = new TicketPurchase();
        purchase.setUser(user);
        purchase.setEvent(event);
        purchase.setTotalPrice(totalPrice);
        purchase.setTransactionId(transactionId);
        purchase.setPaymentMethod("CARD");
        purchase.setStatus(successStatus);

        return ticketPurchaseRepository.save(purchase);
    }

    private void updateSeatStatuses(TicketPurchase purchase, List<UUID> seatIds) {
        List<EventSeatStatus> statuses = eventSeatStatusRepository.findAllByEventIdAndSeatIdIn(purchase.getEvent().getId(), seatIds);
        if (statuses.size() != seatIds.size()) {
            throw new IllegalArgumentException("Data inconsistency: Not all seat statuses found for checkout.");
        }

        // Cần lấy thông tin giá vé
        Map<UUID, Ticket> sectionToTicketMap = getTicketsForSections(purchase.getEvent().getId(), statuses);

        for (EventSeatStatus ess : statuses) {
            Ticket ticket = sectionToTicketMap.get(ess.getSeat().getSection().getId());
            ess.setStatus("SOLD");
            ess.setTicketPurchase(purchase);
            ess.setPriceAtPurchase(ticket.getPrice());
        }
        eventSeatStatusRepository.saveAll(statuses);
    }

    private void updateGaTickets(TicketPurchase purchase, List<TicketHoldRequestDTO.GeneralAdmissionItem> gaItems) {
        Map<UUID, Ticket> ticketMap = getTicketsForGaItems(gaItems);

        for (var item : gaItems) {
            // Giảm số lượng vé trong DB một cách an toàn
            int updatedRows = ticketRepository.decreaseAvailableQuantity(item.getTicketId(), item.getQuantity());
            if (updatedRows == 0) {
                // Nếu không thành công, transaction sẽ rollback toàn bộ
                throw new IllegalArgumentException("Tickets for '" + ticketMap.get(item.getTicketId()).getName() + "' sold out during checkout.");
            }
        }

        // Nếu tất cả đều thành công, tạo các bản ghi chi tiết
        createPurchasedGaTickets(gaItems, purchase, ticketMap);
    }

    private Map<UUID, Ticket> getTicketsForSections(UUID eventId, List<EventSeatStatus> seatStatuses) {
        List<UUID> sectionIds = seatStatuses.stream()
                .map(ess -> ess.getSeat().getSection().getId())
                .distinct().toList();
        return ticketRepository.findByEventIdAndSectionIdIn(eventId, sectionIds)
                .stream().collect(Collectors.toMap(
                        ticket -> ticket.getAppliesToSection().getId(),
                        Function.identity()
                ));
    }

    private Map<UUID, Ticket> getTicketsForGaItems(List<TicketHoldRequestDTO.GeneralAdmissionItem> gaItems) {
        List<UUID> ticketIds = gaItems.stream().map(TicketHoldRequestDTO.GeneralAdmissionItem::getTicketId).toList();
        return ticketRepository.findAllById(ticketIds).stream().collect(Collectors.toMap(Ticket::getId, Function.identity()));
    }

    private void createPurchasedGaTickets(List<TicketHoldRequestDTO.GeneralAdmissionItem> gaItems, TicketPurchase purchase, Map<UUID, Ticket> ticketMap) {
        List<PurchasedGaTicket> purchasedTickets = new ArrayList<>();
        for (var item : gaItems) {
            PurchasedGaTicket pgt = new PurchasedGaTicket();
            pgt.setTicketPurchase(purchase);
            pgt.setTicket(ticketMap.get(item.getTicketId()));
            pgt.setQuantity(item.getQuantity());
            pgt.setPricePerTicket(ticketMap.get(item.getTicketId()).getPrice());
            purchasedTickets.add(pgt);
        }
        purchasedGaTicketRepository.saveAll(purchasedTickets);
    }
}
