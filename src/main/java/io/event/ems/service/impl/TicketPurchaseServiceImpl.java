package io.event.ems.service.impl;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import io.event.ems.config.VNPayConfig;
import io.event.ems.dto.MultiItemPurchaseRequestDTO;
import io.event.ems.dto.PaymentResponseDTO;
import io.event.ems.dto.PurchaseItemDTO;
import io.event.ems.dto.TicketPurchaseDTO;
import io.event.ems.dto.TicketPurchaseDetailDTO;
import io.event.ems.exception.ResourceNotFoundException;
import io.event.ems.mapper.TicketPurchaseMapper;
import io.event.ems.model.StatusCode;
import io.event.ems.model.Ticket;
import io.event.ems.model.TicketPurchase;
import io.event.ems.model.User;
import io.event.ems.repository.StatusCodeRepository;
import io.event.ems.repository.TicketPurchaseRepository;
import io.event.ems.repository.TicketRepository;
import io.event.ems.repository.UserRepository;
import io.event.ems.service.EmailService;
import io.event.ems.service.QrCodeService;
import io.event.ems.service.TicketPurchaseService;
import io.event.ems.util.VnPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TicketPurchaseServiceImpl implements TicketPurchaseService {

    private final TicketPurchaseRepository ticketPurchaseRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final StatusCodeRepository statusCodeRepository;
    private final TicketPurchaseMapper ticketPurchaseMapper;
    private final VNPayConfig vnPayConfig;
    private final EmailService emailService;
    private final QrCodeService qrCodeService;

    private static final int MAX_TICKETS_PER_USER = 10;

    @Override
    public Page<TicketPurchaseDTO> getAllTicketPurchases(Pageable pageable) {
        return ticketPurchaseRepository.findAll(pageable)
                .map(ticketPurchaseMapper::toDTO);
    }

    @Override
    public List<TicketPurchaseDTO> getTicketPurchasesByTransactionId(String transactionId) {
        List<TicketPurchase> purchases = ticketPurchaseRepository.findByTransactionId(transactionId);
        return purchases.stream()
                .map(ticketPurchaseMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<TicketPurchaseDTO> getTicketPurchaseById(UUID id) throws ResourceNotFoundException {
        return ticketPurchaseRepository.findById(id)
                .map(ticketPurchaseMapper::toDTO);
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public PaymentResponseDTO initiateTicketPurchase(MultiItemPurchaseRequestDTO request, String clientIpAddress)
            throws ResourceNotFoundException {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + request.getUserId()));
        StatusCode pendingStatus = statusCodeRepository.findByEntityTypeAndStatus("TICKET_PURCHASE", "PENDING")
                .orElseThrow(
                        () -> new ResourceNotFoundException("Status 'PENDING' for 'TICKET_PURCHASE' not cofigured"));

        String transactionId = UUID.randomUUID().toString();
        log.info("Initiating multi-item purchase with transactionId: {}", transactionId);

        List<TicketPurchase> purchasesToSave = new ArrayList<>();
        BigDecimal grandTotal = BigDecimal.ZERO;
        Map<UUID, Integer> eventTicketCount = new HashMap<>();

        for (PurchaseItemDTO item : request.getItems()) {
            Ticket ticket = ticketRepository.findById(item.getTicketId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + item.getTicketId()));
            if (ticket.getAvailableQuantity() < item.getQuantity()) {
                throw new IllegalArgumentException("Not enough tickets available for ticket ID: " + ticket.getId());
            }
            UUID eventId = ticket.getEvent().getId();
            eventTicketCount.put(eventId, eventTicketCount.getOrDefault(eventId, 0) + item.getQuantity());

            TicketPurchase ticketPurchase = new TicketPurchase();
            ticketPurchase.setUser(user);
            ticketPurchase.setTicket(ticket);
            ticketPurchase.setQuantity(item.getQuantity());
            ticketPurchase.setStatus(pendingStatus);
            ticketPurchase.setTransactionId(transactionId);
            ticketPurchase.setPurchaseDate(LocalDateTime.now());
            ticketPurchase.setPaymentMethod("VNPAY");

            BigDecimal itemTotalPrice = ticket.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            ticketPurchase.setTotalPrice(itemTotalPrice);
            grandTotal = grandTotal.add(itemTotalPrice);

            ticket.setAvailableQuantity(ticket.getAvailableQuantity() - item.getQuantity());
            purchasesToSave.add(ticketPurchase);
        }

        for (Map.Entry<UUID, Integer> entry : eventTicketCount.entrySet()) {
            int totalAlreadyPurchased = ticketPurchaseRepository.countTotalTicketsByUserAndEvent(user.getId(),
                    entry.getKey());
            if (totalAlreadyPurchased + entry.getValue() > MAX_TICKETS_PER_USER) {
                throw new IllegalArgumentException(
                        "Exceeded maximun tickets per user limit for event " + entry.getKey());
            }
        }

        ticketPurchaseRepository.saveAll(purchasesToSave);
        log.info("Created {} pending TicketPurchase records for transactionId: {}", purchasesToSave.size(),
                transactionId);

        String paymentUrl = createVnPayPaymentUrl(transactionId, grandTotal, clientIpAddress);

        return new PaymentResponseDTO(UUID.fromString(transactionId), paymentUrl, "Please proceed with VNPay payment");

    }

    private String createVnPayPaymentUrl(String transactionId, BigDecimal grandTotal, String ipAddress) {
        long amount = grandTotal.multiply(new BigDecimal(100)).longValueExact();

        String orderInfo = "Thanh toan don hang ve su kien. Ma don hang: " + transactionId;

        String txnRef = transactionId;

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnPayConfig.getVnpVersion());
        vnp_Params.put("vnp_Command", VNPayConfig.VNP_COMMAND_PAY);
        vnp_Params.put("vnp_TmnCode", vnPayConfig.getVnpTmnCode());
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", VNPayConfig.VNP_CURR_CODE);
        vnp_Params.put("vnp_TxnRef", txnRef);
        vnp_Params.put("vnp_OrderInfo", orderInfo);
        vnp_Params.put("vnp_Locale", VNPayConfig.VNP_LOCALE);
        vnp_Params.put("vnp_ReturnUrl", vnPayConfig.getVpnReturnUrl());
        vnp_Params.put("vnp_IpAddr", ipAddress != null ? ipAddress : "127.0.0.1");

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        String queryUrl = VnPayUtil.buildQueryUrl(vnp_Params);
        String hashData = VnPayUtil.buildHashData(vnp_Params);
        String vnp_SecureHash = VnPayUtil.hmacSHA512(vnPayConfig.getVnpHashSecret(), hashData);

        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
        String paymentUrl = vnPayConfig.getVnpUrl() + "?" + queryUrl;
        log.info("Generated VnPay URL for purchase {}: {}", transactionId, paymentUrl);

        return paymentUrl;
    }

    @Override
    public TicketPurchaseDTO updateTicketPurchaseStatus(UUID id, TicketPurchaseDTO ticketPurchaseDTO)
            throws ResourceNotFoundException {

        TicketPurchase ticketPurchase = ticketPurchaseRepository.findById(ticketPurchaseDTO.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Ticket purchase not found with id: " + id));

        StatusCode statusCode = statusCodeRepository.findById(ticketPurchaseDTO.getStatusId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Status not found with id: " + ticketPurchaseDTO.getStatusId()));
        ticketPurchase.setStatus(statusCode);

        TicketPurchase updated = ticketPurchaseRepository.save(ticketPurchase);
        return ticketPurchaseMapper.toDTO(updated);
    }

    @Override
    public void deleteTicketPurchase(UUID id) throws ResourceNotFoundException {
        TicketPurchase ticketPurchase = ticketPurchaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket purchase not found with id: " + id));

        if (ticketPurchase.getStatus().getStatus().equals("PENDING")
                || ticketPurchase.getStatus().getStatus().equals("FAILED")) {
            Ticket ticket = ticketPurchase.getTicket();
            ticket.setAvailableQuantity(ticket.getAvailableQuantity() + ticketPurchase.getQuantity());
            ticketRepository.save(ticket);
            ticketPurchaseRepository.delete(ticketPurchase);
            log.info("Deleted pending/failed purchase {} and return {} tickets for ticket {}", id,
                    ticketPurchase.getQuantity(), ticket.getId());
        } else {
            log.warn("Cannot delete purchase {} with status {}", id, ticketPurchase.getStatus().getStatus());
            throw new IllegalArgumentException(
                    "Cannot delete purchase in status: " + ticketPurchase.getStatus().getStatus());
        }

    }

    @Override
    public Page<TicketPurchaseDTO> getTicketPurchasesByUserId(UUID userId, Pageable pageable) {
        return ticketPurchaseRepository.findByUserId(userId, pageable)
                .map(ticketPurchaseMapper::toDTO);
    }

    @Override
    public Page<TicketPurchaseDTO> getTicketPurchasesByTicketId(UUID ticketId, Pageable pageable) {
        return ticketPurchaseRepository.findByTicketId(ticketId, pageable)
                .map(ticketPurchaseMapper::toDTO);
    }

    @Override
    public Page<TicketPurchaseDTO> getTicketPurchasesByStatusId(Integer statusId, Pageable pageable) {
        return ticketPurchaseRepository.findByStatusId(statusId, pageable)
                .map(ticketPurchaseMapper::toDTO);
    }

    @Override
    @Transactional
    public Map<String, String> processVnPayIpn(HttpServletRequest request) {
        Map<String, String> response = new HashMap<>();
        String transactionId = "UNKNOWN";
        try {
            Map<String, String> fields = VnPayUtil.extractParamsFromRequest(request);
            String vnp_SecureHash = fields.remove("vnp_SecureHash");

            transactionId = fields.get("vnp_SecureHash");

            if (vnp_SecureHash == null || vnp_SecureHash.isEmpty()) {
                log.error("IPN Error [TxnRef: {}]: Missing SecureHash", transactionId);
                response.put("RspCode", "97");
                response.put("Message", "Invalid Checksum");
                return response;
            }

            String signValue = VnPayUtil.hashAllFields(fields, vnPayConfig.getVnpHashSecret());
            if (signValue.equals(vnp_SecureHash)) {
                List<TicketPurchase> purchasesInGroup = ticketPurchaseRepository.findByTransactionId(transactionId);

                if (purchasesInGroup.isEmpty()) {
                    log.error("IPN Error: Order not found for TxnRef: {}", transactionId);
                    response.put("RspCode", "01");
                    response.put("Message", "Order not found");
                    return response;
                }

                TicketPurchase firstPurchase = purchasesInGroup.get(0);
                if (!"PENDING".equals(firstPurchase.getStatus().getStatus())) {
                    log.warn("IPN Info [TxnRef: {}]: Order group already processed (status {}).",
                            transactionId, firstPurchase.getStatus().getStatus());
                    response.put("RspCode", "00");
                    response.put("Message", "Order already confirmed");
                    return response;
                }

                BigDecimal totalOrderAmount = purchasesInGroup.stream()
                        .map(TicketPurchase::getTotalPrice)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                long vnpAmount = Long.parseLong(fields.get("vnp_Amount"));
                long orderAmountInCents = totalOrderAmount.multiply(new BigDecimal(100)).longValueExact();

                if (vnpAmount != orderAmountInCents) {
                    log.error("IPN Error [TxnRef: {}]: Invalid amount. Expected: {}, Received: {}", transactionId,
                            orderAmountInCents, vnpAmount);
                    response.put("RspCode", "04");
                    response.put("Message", "Invalid amount");
                    return response;
                }

                String vnp_ResponseCode = fields.get("vnp_ResponseCode");

                if ("00".equals(vnp_ResponseCode)) {
                    log.info("IPN Success [TxnRef: {}]: Payment successful.", transactionId);
                    // Gọi hàm xử lý thành công cho cả nhóm
                    processSuccessfulPaymentInternal(purchasesInGroup);
                    response.put("RspCode", "00");
                    response.put("Message", "Confirm Success");
                } else {
                    log.warn("IPN Failed [TxnRef: {}]: Payment failed with ResponseCode: {}", transactionId,
                            vnp_ResponseCode);
                    // Gọi hàm xử lý thất bại cho cả nhóm
                    processFailedPaymentInternal(purchasesInGroup);
                    // Vẫn trả về "00" cho VNPay để họ không gửi lại IPN
                    response.put("RspCode", "00");
                    response.put("Message", "Confirm Success (but transaction failed)");
                }
            } else {
                log.error("IPN Error [TxnRef: {}]: Invalid SecureHash", transactionId);
                response.put("RspCode", "97");
                response.put("Message", "Invalid Checksum");
            }
        } catch (Exception e) {
            log.error("IPN Error [TxnRef: {}]: Exception processing IPN", transactionId, e);
            response.put("RspCode", "99");
            response.put("Message", "Unknown error");
        }
        return response;
    }

    private void processSuccessfulPaymentInternal(List<TicketPurchase> purchasesInGroup) {
        StatusCode successStatus = statusCodeRepository.findByEntityTypeAndStatus("TICKET_PURCHASE", "SUCCESS")
                .orElseThrow(() -> new IllegalStateException("SUCCESS status not configured."));

        purchasesInGroup.forEach(purchase -> purchase.setStatus(successStatus));
        List<TicketPurchase> updatedPurchases = ticketPurchaseRepository.saveAll(purchasesInGroup);

        log.info("Processed successful payment for transaction group {}", updatedPurchases.get(0).getTransactionId());

        try {
            sendConfirmationEmailWithAllTickets(updatedPurchases);
        } catch (Exception e) {
            log.error("CRITICAL: Transaction {} confirmed but failed to send email. Manual action required.",
                    updatedPurchases.get(0).getTransactionId(), e);
        }

    }

    private void processFailedPaymentInternal(List<TicketPurchase> purchasesInGroup) {
        StatusCode failedStatus = statusCodeRepository.findByEntityTypeAndStatus("TICKET_PURCHASE", "FAILED")
                .orElseThrow(() -> new IllegalStateException("FAILED status not configured."));

        purchasesInGroup.forEach(purchase -> {
            purchase.setStatus(failedStatus);

            // Hoàn trả lại số lượng vé đã tạm giữ
            Ticket ticket = purchase.getTicket();
            ticket.setAvailableQuantity(ticket.getAvailableQuantity() + purchase.getQuantity());
            // Không cần save ticket ở đây, transaction sẽ quản lý
            log.info("Returned {} tickets for failed purchase {} on ticket {}",
                    purchase.getQuantity(), purchase.getId(), ticket.getId());
        });

        ticketPurchaseRepository.saveAll(purchasesInGroup);
        log.warn("Processed failed payment for transaction group {}", purchasesInGroup.get(0).getTransactionId());
    }

    @Override
    public boolean verifyVnPayReturn(HttpServletRequest request) {
        try {
            Map<String, String> fields = VnPayUtil.extractParamsFromRequest(request);
            String vnp_SecureHash = fields.remove("vnp_SecureHash");

            if (vnp_SecureHash == null || vnp_SecureHash.isEmpty()) {
                log.warn("Return URL Verification Failed: Missing SecureHash.");
                return false;
            }
            String signValue = VnPayUtil.hashAllFields(fields, vnPayConfig.getVnpHashSecret());
            return signValue.equals(vnp_SecureHash);

        } catch (Exception e) {
            log.error("Error verifying VNPay return URL", e);
            return false;
        }
    }

    @Override
    @Transactional
    public List<TicketPurchaseDTO> confirmPurchaseByGroup(String transactionId) throws ResourceNotFoundException {
        List<TicketPurchase> purchasesInGroup = ticketPurchaseRepository.findByTransactionId(transactionId);
        if (purchasesInGroup.isEmpty()) {
            throw new ResourceNotFoundException("Purchase group not found with tracsactionId: " + transactionId);
        }

        boolean allSuccess = purchasesInGroup.stream()
                .allMatch(p -> "SUCCESS".equals(p.getStatus().getStatus()));
        if (allSuccess) {
            log.warn("All purchases in transaction group {} already confirmed.", transactionId);
            return purchasesInGroup.stream().map(ticketPurchaseMapper::toDTO).collect(Collectors.toList());
        }

        StatusCode successStatus = statusCodeRepository.findByEntityTypeAndStatus("TICKET_PURCHASE", "SUCCESS")
                .orElseThrow(() -> new ResourceNotFoundException("SUCCESS status not configured"));

        purchasesInGroup.forEach(purchase -> purchase.setStatus(successStatus));

        List<TicketPurchase> updatedPurchases = ticketPurchaseRepository.saveAll(purchasesInGroup);
        log.info("Updated status to SUCCESS for {} purchases in transaction group {}", updatedPurchases.size(),
                transactionId);

        // Generate QR code for confirmed purchase
        try {
            sendConfirmationEmailWithAllTickets(updatedPurchases);
        } catch (Exception e) {
            log.error("CRITICAL: Transaction {} confirmed but failed to send email. Manual action required.",
                    transactionId, e);
        }

        log.info("Successfully confirmed transaction group {}", transactionId);
        return updatedPurchases.stream().map(ticketPurchaseMapper::toDTO).collect(Collectors.toList());
    }

    private void sendConfirmationEmailWithAllTickets(List<TicketPurchase> confirmedPurchases) {

        if (confirmedPurchases == null || confirmedPurchases.isEmpty()) {
            return;
        }

        User user = confirmedPurchases.get(0).getUser();
        String transactionId = confirmedPurchases.get(0).getTransactionId();

        // Chuẩn bị dữ liệu cho email
        List<Map<String, Object>> ticketsDataForEmail = new ArrayList<>();
        Map<String, byte[]> qrCodeImages = new HashMap<>();

        for (TicketPurchase purchase : confirmedPurchases) {
            String cid = "qrcode_" + purchase.getId().toString();
            byte[] qrCodeBytes = qrCodeService.genarateQrCodeForPurchase(purchase.getId());

            Map<String, Object> ticketInfo = new HashMap<>();
            ticketInfo.put("purchaseId", purchase.getId());
            ticketInfo.put("eventName", purchase.getTicket().getEvent().getTitle());
            ticketInfo.put("eventStartDate", purchase.getTicket().getEvent().getStartDate());
            ticketInfo.put("eventEndDate", purchase.getTicket().getEvent().getEndDate());
            ticketInfo.put("ticketType", purchase.getTicket().getTicketType());
            ticketInfo.put("quantity", purchase.getQuantity());
            ticketInfo.put("qrImageCid", cid);

            ticketsDataForEmail.add(ticketInfo);
            qrCodeImages.put(cid, qrCodeBytes);
        }

        emailService.sendGroupTicketConfirmation(user.getEmail(), user.getFullName(), transactionId,
                ticketsDataForEmail, qrCodeImages);

    }

    @Override
    public Optional<TicketPurchaseDetailDTO> getTicketPurchaseDetailById(UUID id) {
        return ticketPurchaseRepository.findById(id)
                .map(ticketPurchaseMapper::toDetailDTO);
    }

    @Override
    public Page<TicketPurchaseDetailDTO> getTicketPurchaseDetailsByUserId(UUID userId, Pageable pageable) {
        return ticketPurchaseRepository.findByUserId(userId, pageable)
                .map(ticketPurchaseMapper::toDetailDTO);
    }

}
