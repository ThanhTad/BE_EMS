package io.event.ems.service.impl;

import io.event.ems.dto.*;
import io.event.ems.exception.ResourceNotFoundException;
import io.event.ems.model.*;
import io.event.ems.repository.*;
import io.event.ems.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

    private final PurchasedGATicketRepository purchasedGaTicketRepository;

    private final PaymentGatewayService paymentGatewayService;

    private final EmailService emailService;

    private final QrCodeService qrCodeService;

    private final TransactionTemplate transactionTemplate;
    private final TicketHoldService ticketHoldService;

    private static final BigDecimal SERVICE_FEE_PERCENTAGE = new BigDecimal("0.05");

    @Override
    public TicketPurchaseConfirmationDTO finalizeDirectPurchase(UUID holdId, UUID userId, PaymentDetailsDTO paymentDetails) {
        log.info("Finalizing direct purchase for hold [ID={}]", holdId);
        HoldData holdData = ticketHoldService.getAndFinalizeHold(holdId, userId);

        try {
            // --- BƯỚC 1: TÍNH TOÁN GIÁ ---
            BigDecimal subtotal = calculateSubtotalFromHoldData(holdData);
            BigDecimal serviceFee = calculateServiceFee(subtotal);
            BigDecimal totalPrice = subtotal.add(serviceFee);

            // --- BƯỚC 2: XỬ LÝ THANH TOÁN ---
            // String transactionId = stripeGateway.processPayment(paymentDetails.getPaymentToken(), totalPrice);
            String transactionId = "STRIPE_TXN_" + UUID.randomUUID().toString().substring(0, 12);

            // --- BƯỚC 3 & 4: GHI DB VÀ GỬI EMAIL ---
            return processDatabaseAndSendEmail(holdData, totalPrice, subtotal, serviceFee, transactionId, paymentDetails.getPaymentMethod());

        } catch (Exception e) {
            log.error("Error during direct purchase for hold [ID={}]. Releasing resources.", holdId, e);
            ticketHoldService.releaseResourcesForFailedCheckout(holdData);
            throw new IllegalArgumentException("Failed to finalize direct purchase", e);
        }
    }

    @Override
    public TicketPurchaseConfirmationDTO mockFinalizePurchase(UUID holdId, UUID userId) {
        log.warn("Executing MOCK payment finalization for hold [ID={}]", holdId);
        HoldData holdData = ticketHoldService.getAndFinalizeHold(holdId, userId);
        try {
            BigDecimal subtotal = calculateSubtotalFromHoldData(holdData);
            BigDecimal serviceFee = calculateServiceFee(subtotal);
            BigDecimal totalPrice = subtotal.add(serviceFee);
            String mockTransactionId = "MOCK_TXN_" + UUID.randomUUID().toString().substring(0, 12);
            return processDatabaseAndSendEmail(holdData, totalPrice, subtotal, serviceFee, mockTransactionId, "MOCK_PAYMENT");
        } catch (Exception e) {
            log.error("Error during mock purchase for hold [ID={}]. Releasing resources.", holdId, e);
            ticketHoldService.releaseResourcesForFailedCheckout(holdData);
            throw new IllegalArgumentException("Failed to finalize mock purchase", e);
        }
    }


    // === LUỒNG THANH TOÁN CHUYỂN HƯỚNG (MOMO, VNPAY) ===

    @Override
    @Transactional
    public PaymentCreationResultDTO initiateRedirectPayment(UUID holdId, UUID userId, String paymentMethod, String ipAddress) {
        log.info("Initiating redirect payment for hold [ID={}] via [{}]", holdId, paymentMethod);
        HoldData holdData = ticketHoldService.getAndFinalizeHold(holdId, userId);

        try {
            // Tạo đơn hàng PENDING trước
            TicketPurchase purchase = createPurchaseWithStatus(holdData, paymentMethod, "PENDING");
            // Cập nhật giá vào đơn hàng PENDING
            BigDecimal totalPrice = calculateTotalPrice(holdData);
            BigDecimal subTotal = calculateSubtotalFromHoldData(holdData);
            purchase.setTotalPrice(totalPrice);
            purchase.setSubTotal(subTotal);
            purchase.setServiceFee(totalPrice.subtract(subTotal));
            ticketPurchaseRepository.save(purchase);

            // Lấy URL thanh toán
            return paymentGatewayService.createPayment(paymentMethod, purchase, ipAddress);
        } catch (Exception e) {
            log.error("Error initiating redirect payment for hold [ID={}]. Releasing resources.", holdId, e);
            ticketHoldService.releaseResourcesForFailedCheckout(holdData);
            throw new IllegalArgumentException("Failed to initiate payment with " + paymentMethod, e);
        }
    }

    @Override
    @Transactional
    public TicketPurchaseConfirmationDTO verifyAndFinalizeRedirectedPurchase(String provider, String orderId, UUID userId, Map<String, String> params) {
        log.info("Verifying payment return for provider [{}] and order [ID={}]", provider, orderId);

        if (!paymentGatewayService.verifyPayment(provider, params)) {
            throw new SecurityException("Invalid payment signature from " + provider);
        }

        TicketPurchase purchase = ticketPurchaseRepository.findByIdAndUserIdWithDetails(UUID.fromString(orderId), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase not found or does not belong to user."));

        if (!"PENDING".equals(purchase.getStatus().getStatus())) {
            log.warn("Purchase [ID={}] already processed with status [{}]", purchase.getId(), purchase.getStatus().getStatus());
            return new TicketPurchaseConfirmationDTO(purchase.getId(), "Purchase already processed.", purchase.getPurchaseDate());
        }

        // --- HOÀN TẤT GHI VÀO DB ---
        StatusCode successStatus = getStatusCode("TICKET_PURCHASE", "SUCCESS");
        purchase.setStatus(successStatus);
        purchase.setTransactionId(params.getOrDefault("vnp_TransactionNo", params.get("transId")));

        // Dựa vào `paymentMethod` và cấu trúc `purchase` để biết cần cập nhật `SEATED` hay `GA`
        // Cần một cách để lấy lại `HoldData` hoặc `TicketHoldRequestDTO`
        // Giải pháp: Lưu `TicketHoldRequestDTO` vào một cột JSON trong `TicketPurchase` khi tạo PENDING.
        // Giả sử ta đã có: TicketHoldRequestDTO request = purchase.getHoldRequestData();
        // updateResourcesForPurchase(purchase, request); // Cần có request data

        TicketPurchase savedPurchase = ticketPurchaseRepository.save(purchase);

        // --- GỬI EMAIL ---
        prepareAndSendConfirmationEmail(savedPurchase);

        return new TicketPurchaseConfirmationDTO(savedPurchase.getId(), "Purchase confirmed.", savedPurchase.getPurchaseDate());
    }

    // ========================================================================
    // === CÁC HÀM HELPER CHÍNH, TÁI SỬ DỤNG LOGIC TỪ PHIÊN BẢN CŨ CỦA BẠN ===
    // ========================================================================

    /**
     * Tính toán tổng giá trị của các vé (subtotal) từ HoldData.
     * Hàm này sẽ điều phối đến các hàm tính toán chi tiết hơn dựa trên selectionMode.
     *
     * @param holdData Dữ liệu phiên giữ chỗ.
     * @return BigDecimal là tổng giá trị vé trước khi tính phí.
     */
    private BigDecimal calculateSubtotalFromHoldData(HoldData holdData) {
        TicketHoldRequestDTO request = holdData.getRequest();
        log.debug("Calculating subtotal for hold [ID={}] with mode [{}]", holdData.getHoldId(), request.getSelectionMode());

        return switch (request.getSelectionMode()) {
            case RESERVED_SEATING -> calculateSubTotalForReserved(holdData.getEventId(), request.getSeatIds());
            case GENERAL_ADMISSION, ZONED_ADMISSION -> calculateSubTotalForGa(request.getGaItems());
        };
    }


    /**
     * Tạo và lưu một bản ghi TicketPurchase ban đầu với một trạng thái cụ thể.
     * Hàm này không điền thông tin về giá, việc đó sẽ được thực hiện sau.
     *
     * @param holdData      Dữ liệu phiên giữ chỗ chứa thông tin về user và event.
     * @param paymentMethod Tên phương thức thanh toán (e.g., "MOMO", "STRIPE_CARD").
     * @param statusName    Tên của trạng thái ban đầu (e.g., "PENDING").
     * @return Đối tượng TicketPurchase đã được lưu vào DB (với ID đã được tạo).
     */
    private TicketPurchase createPurchaseWithStatus(HoldData holdData, String paymentMethod, String statusName) {
        // Lấy các entity liên quan
        User user = userRepository.findById(holdData.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + holdData.getUserId()));
        Event event = eventRepository.findById(holdData.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + holdData.getEventId()));
        StatusCode status = getStatusCode("TICKET_PURCHASE", statusName);

        // Tạo đối tượng purchase
        TicketPurchase purchase = new TicketPurchase();
        purchase.setUser(user);
        purchase.setEvent(event);
        purchase.setPaymentMethod(paymentMethod);
        purchase.setStatus(status);
        // Các trường khác như totalPrice, transactionId sẽ được cập nhật sau

        // Lưu vào DB để lấy ID (quan trọng cho MoMo/VNPAY)
        TicketPurchase savedPurchase = ticketPurchaseRepository.save(purchase);
        log.info("Created new TicketPurchase [ID={}] with status [{}] for user [ID={}]",
                savedPurchase.getId(), statusName, user.getId());

        return savedPurchase;
    }

    private StatusCode getStatusCode(String entityType, String statusName) {
        log.debug("Fetching status code for entity [{}] with status [{}]", entityType, statusName);
        return statusCodeRepository.findByEntityTypeAndStatus(entityType, statusName)
                .orElseThrow(() -> {
                    log.error("CRITICAL: Status code not found for entity '{}' and status '{}'. Please check the status_codes table.", entityType, statusName);
                    return new IllegalStateException("Status '" + statusName + "' for entity '" + entityType + "' is not configured.");
                });
    }

    /**
     * Hàm trung tâm để xử lý Bước 3 (Ghi DB) và Bước 4 (Gửi Email).
     */
    private TicketPurchaseConfirmationDTO processDatabaseAndSendEmail(HoldData holdData, BigDecimal totalPrice, BigDecimal subTotal, BigDecimal serviceFee, String transactionId, String paymentMethod) {
        // --- BƯỚC 3: GHI DỮ LIỆU VÀO DATABASE (TRONG TRANSACTION) ---
        TicketPurchase savedPurchase = transactionTemplate.execute(status -> {
            TicketPurchase purchase = createAndSaveTicketPurchase(holdData, totalPrice, subTotal, serviceFee, transactionId, paymentMethod);

            // ** LOGIC XỬ LÝ RIÊNG CHO TỪNG LOẠI VÉ MÀ BẠN ĐÃ LÀM RẤT TỐT **
            TicketHoldRequestDTO request = holdData.getRequest();
            if (request.getSelectionMode() == TicketSelectionModeEnum.RESERVED_SEATING) {
                updateSeatStatuses(purchase, request.getSeatIds());
            } else { // GA và Zoned
                updateGaTickets(purchase, request.getGaItems());
            }
            return purchase;
        });

        if (savedPurchase == null) {
            // paymentGatewayService.refund(transactionId);
            throw new RuntimeException("Failed to save purchase details after successful payment.");
        }

        // --- BƯỚC 4: GỬI EMAIL XÁC NHẬN (BÊN NGOÀI TRANSACTION) ---
        prepareAndSendConfirmationEmail(savedPurchase);

        return new TicketPurchaseConfirmationDTO(savedPurchase.getId(), "Purchase successful.", savedPurchase.getPurchaseDate());
    }

    /**
     * Hàm chuẩn bị và gửi email, tái sử dụng cấu trúc rõ ràng từ phiên bản cũ của bạn.
     */
    private void prepareAndSendConfirmationEmail(TicketPurchase purchase) {
        log.info("Preparing confirmation email for purchase [ID={}]", purchase.getId());
        // Lấy dữ liệu vé từ database
        List<EventSeatStatus> soldSeats = eventSeatStatusRepository.findByTicketPurchaseId(purchase.getId());
        List<PurchasedGATicket> purchasedGATickets = purchasedGaTicketRepository.findByTicketPurchaseId(purchase.getId());

        Map<String, byte[]> inlineQrImages = new HashMap<>();
        List<ProcessingTicketDTO> allTicketsForProcessing = new ArrayList<>();

        // Xử lý từng loại vé
        allTicketsForProcessing.addAll(processReservedSeats(soldSeats, inlineQrImages));
        allTicketsForProcessing.addAll(processGATickets(purchasedGATickets, inlineQrImages));

        // Gom nhóm các vé đã xử lý
        List<EmailDetails.TicketGroupInfo> ticketGroupsForEmail = groupTickets(allTicketsForProcessing);

        // Xây dựng và gửi
        EmailDetails emailDetails = buildEmailDetails(purchase, ticketGroupsForEmail, inlineQrImages);
        emailService.sendPurchaseConfirmationEmail(emailDetails);
    }

    /**
     * Xử lý danh sách vé ghế ngồi đã bán, tạo QR code và chuyển đổi thành DTO trung gian.
     *
     * @param soldSeats      Danh sách các bản ghi EventSeatStatus.
     * @param inlineQrImages Map để lưu trữ dữ liệu QR code (sẽ được điền vào trong hàm này).
     * @return Danh sách các DTO trung gian đã được xử lý.
     */
    private List<ProcessingTicketDTO> processReservedSeats(List<EventSeatStatus> soldSeats, Map<String, byte[]> inlineQrImages) {
        if (soldSeats == null || soldSeats.isEmpty()) {
            return Collections.emptyList();
        }

        log.info("Processing {} reserved seats...", soldSeats.size());
        return soldSeats.stream()
                .map(seat -> {
                    byte[] qrImage = qrCodeService.generateQrCodeForReservedSeat(seat);
                    String cid = "qr_seat_" + seat.getId();
                    inlineQrImages.put(cid, qrImage);

                    EmailDetails.TicketInfo info = new EmailDetails.TicketInfo(
                            seat.getPriceAtPurchase(),
                            String.format("Khu %s - Hàng %s - Ghế %s",
                                    seat.getSeat().getSection().getName(),
                                    seat.getSeat().getRowLabel(),
                                    seat.getSeat().getSeatNumber()),
                            cid
                    );
                    return new ProcessingTicketDTO(seat.getTicket().getName(), info);
                })
                .collect(Collectors.toList());
    }

    /**
     * Xử lý danh sách vé GA đã mua, tạo QR code cho từng vé và chuyển đổi thành DTO trung gian.
     *
     * @param purchasedGATickets Danh sách các bản ghi PurchasedGATicket (mỗi bản ghi có thể đại diện cho nhiều vé).
     * @param inlineQrImages     Map để lưu trữ dữ liệu QR code.
     * @return Danh sách các DTO trung gian đã được xử lý.
     */
    private List<ProcessingTicketDTO> processGATickets(List<PurchasedGATicket> purchasedGATickets, Map<String, byte[]> inlineQrImages) {
        if (purchasedGATickets == null || purchasedGATickets.isEmpty()) {
            return Collections.emptyList();
        }

        log.info("Processing {} GA ticket groups...", purchasedGATickets.size());
        return purchasedGATickets.stream()
                .flatMap(gaTicketGroup -> {
                    List<byte[]> qrImages = qrCodeService.generateQrCodeForGaTicket(gaTicketGroup);
                    // Dùng IntStream để tạo index cho mỗi vé trong nhóm
                    return IntStream.range(0, qrImages.size())
                            .mapToObj(i -> {
                                byte[] qrImage = qrImages.get(i);
                                String cid = "qr_ga_" + gaTicketGroup.getId() + "_" + i;
                                inlineQrImages.put(cid, qrImage);

                                EmailDetails.TicketInfo info = new EmailDetails.TicketInfo(
                                        gaTicketGroup.getPricePerTicket(),
                                        "Vé vào cửa tự do", // Hoặc gaTicketGroup.getTicket().getZone().getName() nếu có
                                        cid
                                );
                                return new ProcessingTicketDTO(gaTicketGroup.getTicket().getName(), info);
                            });
                })
                .collect(Collectors.toList());
    }

    /**
     * Gom nhóm danh sách các DTO vé trung gian thành các nhóm theo tên vé.
     */
    private List<EmailDetails.TicketGroupInfo> groupTickets(List<ProcessingTicketDTO> allTickets) {
        if (allTickets.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, List<EmailDetails.TicketInfo>> groupedMap = allTickets.stream()
                .collect(Collectors.groupingBy(
                        ProcessingTicketDTO::getTicketName,
                        Collectors.mapping(ProcessingTicketDTO::getTicketInfo, Collectors.toList())
                ));

        return groupedMap.entrySet().stream()
                .map(entry -> new EmailDetails.TicketGroupInfo(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * Xây dựng đối tượng EmailDetails cuối cùng từ các dữ liệu đã xử lý.
     */
    private EmailDetails buildEmailDetails(TicketPurchase purchase,
                                           List<EmailDetails.TicketGroupInfo> ticketGroups,
                                           Map<String, byte[]> inlineQrImages) {
        return EmailDetails.builder()
                .toEmail(purchase.getUser().getEmail())
                .customerName(purchase.getUser().getFullName())
                .eventName(purchase.getEvent().getTitle())
                .transactionId(purchase.getTransactionId())
                .ticketGroups(ticketGroups)
                .inlineQrImages(inlineQrImages)
                .totalAmount(purchase.getTotalPrice())
                .eventTime(String.valueOf(purchase.getEvent().getStartDate()))
                .venue(purchase.getEvent().getVenue().getName())
                .build();
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
        BigDecimal subTotal = BigDecimal.ZERO;

        // 1. Lấy trạng thái ghế
        List<EventSeatStatus> statuses = eventSeatStatusRepository.findAllByEventIdAndSeatIdIn(eventId, seatIds);

        if (statuses.size() != seatIds.size()) {
            throw new ResourceNotFoundException("Could not find all requested seat statuses for price calculation.");
        }

        // 2. Chuẩn bị cache: sectionId -> List<Ticket>
        Map<UUID, List<Ticket>> ticketsBySection = new HashMap<>();

        for (EventSeatStatus status : statuses) {
            if (!"available".equalsIgnoreCase(status.getStatus())) {
                throw new IllegalArgumentException("Seat " + status.getSeat().getId() + " is no longer available.");
            }

            Seat seat = status.getSeat();
            UUID sectionId = seat.getSection().getId();

            // Lấy danh sách vé theo section từ cache hoặc repo
            List<Ticket> tickets = ticketsBySection.computeIfAbsent(
                    sectionId,
                    id -> ticketRepository.findByEventIdAndSectionId(eventId, id)
            );

            Ticket applicableTicket = findApplicableTicketForSeat(seat, tickets);

            if (applicableTicket == null) {
                throw new IllegalArgumentException("Could not determine ticket price for seat " + seat.getId());
            }

            log.info("Using ticket [{}] (price: {}) for seat [{}]", applicableTicket.getId(), applicableTicket.getPrice(), seat.getId());

            subTotal = subTotal.add(applicableTicket.getPrice());
        }

        return subTotal;
    }

    /**
     * Nâng cấp: Tìm loại vé phù hợp nhất cho một ghế cụ thể dựa trên hệ thống quy tắc ưu tiên.
     *
     * @param seat           Ghế cần tìm vé.
     * @param sectionTickets Danh sách tất cả các loại vé có sẵn cho khu vực chứa ghế đó.
     * @return Ticket phù hợp nhất, hoặc null nếu không tìm thấy.
     */
    private Ticket findApplicableTicketForSeat(Seat seat, List<Ticket> sectionTickets) {
        if (sectionTickets == null || sectionTickets.isEmpty()) {
            return null; // Không có vé nào được định nghĩa cho khu vực này
        }

        // Tối ưu: Nếu chỉ có 1 loại vé, không cần matching phức tạp
        if (sectionTickets.size() == 1) {
            return sectionTickets.get(0);
        }

        // --- BẮT ĐẦU LOGIC MATCHING ƯU TIÊN ---

        // Ưu tiên #1: Tìm vé khớp chính xác với SEAT_TYPE (ví dụ: "VIP", "Standard")
        // Quy ước: Tên vé phải chứa tên loại ghế. Ví dụ: "Vé VIP", "Vé Standard Hàng A"
        Optional<Ticket> ticketByType = sectionTickets.stream()
                .filter(ticket -> ticket.getName().toLowerCase().contains(seat.getSeatType().toLowerCase()))
                .findFirst();

        if (ticketByType.isPresent()) {
            log.debug("Seat [{}]: Matched by SEAT_TYPE to ticket [{}]",
                    seat.getRowLabel() + seat.getSeatNumber(), ticketByType.get().getName());
            return ticketByType.get();
        }

        // Ưu tiên #2: Tìm vé khớp với ROW_LABEL (ví dụ: "Hàng A", "Hàng B")
        // Quy ước: Tên vé phải chứa "Hàng X". Ví dụ: "Vé Hàng A-C"
        String rowTarget = "hàng " + seat.getRowLabel().toLowerCase();
        Optional<Ticket> ticketByRow = sectionTickets.stream()
                .filter(ticket -> ticket.getName().toLowerCase().contains(rowTarget))
                .findFirst();

        if (ticketByRow.isPresent()) {
            log.debug("Seat [{}]: Matched by ROW_LABEL to ticket [{}]",
                    seat.getRowLabel() + seat.getSeatNumber(), ticketByRow.get());
            return ticketByRow.get();
        }

        // Ưu tiên #3 (Dự phòng): Nếu không có quy tắc nào ở trên khớp,
        // hãy tìm một vé "chung chung" không chứa các từ khóa quy tắc.
        // Điều này tránh việc ghế "Standard" bị gán nhầm vé "VIP" nếu vé VIP được định nghĩa trước.
        Optional<Ticket> defaultTicket = sectionTickets.stream()
                .filter(ticket -> !ticket.getName().toLowerCase().contains("vip") &&
                        !ticket.getName().toLowerCase().contains("hàng ")) // Thêm các từ khóa khác nếu cần
                .findFirst();

        if (defaultTicket.isPresent()) {
            log.debug("Seat [{}]: No specific rule matched. Using default ticket [{}]",
                    seat.getRowLabel() + seat.getSeatNumber(), defaultTicket.get().getName());
            return defaultTicket.get();
        }

        // Trường hợp cuối cùng: Không tìm thấy vé "chung chung" nào, trả về vé đầu tiên trong danh sách.
        log.warn("Seat [{}]: Could not find a suitable ticket. Falling back to the first available ticket in the section.",
                seat.getRowLabel() + seat.getSeatNumber());
        return sectionTickets.get(0);
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

    private TicketPurchase createAndSaveTicketPurchase(HoldData holdData, BigDecimal totalPrice, BigDecimal subtotal, BigDecimal serviceFee, String transactionId, String paymentMethod) {
        User user = userRepository.findById(holdData.getUserId()).orElseThrow(() -> new ResourceNotFoundException("User not found: " + holdData.getUserId()));
        Event event = eventRepository.findById(holdData.getEventId()).orElseThrow(() -> new ResourceNotFoundException("Event not found: " + holdData.getEventId()));
        StatusCode successStatus = statusCodeRepository.findByEntityTypeAndStatus("TICKET_PURCHASE", "COMPLETED")
                .orElseThrow(() -> new IllegalStateException("Status 'COMPLETED' for TICKET_PURCHASE not configured."));

        TicketPurchase purchase = new TicketPurchase();
        purchase.setUser(user);
        purchase.setEvent(event);
        purchase.setTotalPrice(totalPrice);
        purchase.setSubTotal(subtotal);
        purchase.setServiceFee(serviceFee);
        purchase.setTransactionId(transactionId);
        purchase.setPaymentMethod(paymentMethod);
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
        List<PurchasedGATicket> purchasedTickets = new ArrayList<>();
        for (var item : gaItems) {
            PurchasedGATicket pgt = new PurchasedGATicket();
            pgt.setTicketPurchase(purchase);
            pgt.setTicket(ticketMap.get(item.getTicketId()));
            pgt.setQuantity(item.getQuantity());
            pgt.setPricePerTicket(ticketMap.get(item.getTicketId()).getPrice());
            purchasedTickets.add(pgt);
        }
        purchasedGaTicketRepository.saveAll(purchasedTickets);
    }

    private BigDecimal calculateServiceFee(BigDecimal subtotal) {
        return subtotal.multiply(SERVICE_FEE_PERCENTAGE).setScale(2, RoundingMode.HALF_UP);
    }
}
