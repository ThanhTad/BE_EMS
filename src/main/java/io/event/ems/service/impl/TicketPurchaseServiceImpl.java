package io.event.ems.service.impl;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.event.ems.config.VNPayConfig;
import io.event.ems.dto.PaymentResponseDTO;
import io.event.ems.dto.TicketPurchaseDTO;
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

    @Override
    public Page<TicketPurchaseDTO> getAllTicketPurchases(Pageable pageable) {
        return ticketPurchaseRepository.findAll(pageable)
                .map(ticketPurchaseMapper::toDTO);
    }

    @Override
    public Optional<TicketPurchaseDTO> getTicketPurchaseById(UUID id) throws ResourceNotFoundException {
        return ticketPurchaseRepository.findById(id)
                .map(ticketPurchaseMapper::toDTO);
    }

    @Override
    public PaymentResponseDTO initiateTicketPurchase(TicketPurchaseDTO ticketPurchaseDTO, String clientIpAddress)
            throws ResourceNotFoundException {
        log.info("Initiating ticket purchase for user {} and ticket {}", ticketPurchaseDTO.getUserId(),
                ticketPurchaseDTO.getTicketId());

        User user = userRepository.findById(ticketPurchaseDTO.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + ticketPurchaseDTO.getUserId()));
        Ticket ticket = ticketRepository.findById(ticketPurchaseDTO.getTicketId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ticket not found with id: " + ticketPurchaseDTO.getTicketId()));
        StatusCode pendingStatus = statusCodeRepository.findByEntityTypeAndStatus("TICKET_PURCHASE", "PENDING")
                .orElseThrow(
                        () -> new ResourceNotFoundException("Status 'PENDING' for 'TICKET_PURCHASE' not cofigured"));

        if (ticket.getAvailableQuantity() < ticketPurchaseDTO.getQuantity()) {
            throw new IllegalArgumentException("Not enough tickets available for ticket ID: " + ticket.getId());
        }

        if (ticketPurchaseRepository.existsByUserAndTicket(user, ticket)) {
            throw new IllegalArgumentException("User has already purchased this ticket");
        }

        TicketPurchase purchase = new TicketPurchase();
        purchase.setUser(user);
        purchase.setTicket(ticket);
        purchase.setQuantity(ticketPurchaseDTO.getQuantity());
        purchase.setStatus(pendingStatus);

        BigDecimal totalPrice = ticket.getPrice().multiply(BigDecimal.valueOf(ticketPurchaseDTO.getQuantity()));
        purchase.setTotalPrice(totalPrice);

        ticket.setAvailableQuantity(ticket.getAvailableQuantity() - ticketPurchaseDTO.getQuantity());
        ticketRepository.save(ticket);

        TicketPurchase savedPurchase = ticketPurchaseRepository.save(purchase);
        log.info("Created pending TicketPurchase with ID: " + savedPurchase.getId());

        String paymentUrl = createVnPayPaymentUrl(savedPurchase, clientIpAddress);

        return new PaymentResponseDTO(savedPurchase.getId(), paymentUrl, "Please proceed with VNPay payment");

    }

    private String createVnPayPaymentUrl(TicketPurchase purchase, String ipAddress) {
        long amount = purchase.getTotalPrice().multiply(new BigDecimal(100)).longValueExact();
        String orderInfo = "Thanh toan ve su kien " + purchase.getTicket().getEvent().getId() + " mua hang "
                + purchase.getId();
        String txnRef = purchase.getId().toString();

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
        log.info("Generated VnPay URL for purchase {}: {}", purchase.getId(), paymentUrl);

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
        try {
            Map<String, String> fields = VnPayUtil.extractParamsFromRequest(request);
            String vnp_SecureHash = fields.remove("vnp_SecureHash");

            if (vnp_SecureHash == null || vnp_SecureHash.isEmpty()) {
                log.error("IPN Error: Missing SecureHash");
                response.put("RspCode", "97");
                response.put("Message", "Invalid Checksum");
                return response;
            }

            String signValue = VnPayUtil.hashAllFields(fields, vnPayConfig.getVnpHashSecret());
            if (signValue.equals(vnp_SecureHash)) {
                UUID purchaseId = UUID.fromString(fields.get("vnp_TxnRef"));
                Optional<TicketPurchase> purchaseOpt = ticketPurchaseRepository.findById(purchaseId);

                if (purchaseOpt.isEmpty()) {
                    log.error("IPN Error: Order not found for TxnRef: {}", purchaseId);
                    response.put("RspCode", "01");
                    response.put("Message", "Order not found");
                    return response;
                }

                TicketPurchase purchase = purchaseOpt.get();
                StatusCode pendingStatus = statusCodeRepository.findByEntityTypeAndStatus("TICKET_PURCHASE", "PENDING")
                        .orElseThrow(() -> new IllegalArgumentException("Status 'PENDING' not configured."));
                StatusCode successStatus = statusCodeRepository.findByEntityTypeAndStatus("TICKET_PURCHASE", "SUCCESS")
                        .orElseThrow(() -> new IllegalArgumentException("Status 'SUCCESS' not configured."));
                StatusCode failedStatus = statusCodeRepository.findByEntityTypeAndStatus("TICKET_PURCHASE", "FAILED")
                        .orElseThrow(() -> new IllegalArgumentException("Status 'FAILED' not configured."));

                if (!purchase.getStatus().getId().equals(pendingStatus.getId())) {
                    log.warn("IPN Infor: Order {} already processed (status {}). Replying success to VNPay.",
                            purchaseId, purchase.getStatus().getStatus());
                    response.put("RspCode", "00");
                    response.put("Message", "Order already confirmed");
                    return response;
                }

                boolean checkAmount = true;
                long vnpAmount = Long.parseLong(fields.get("vnp_Amount"));
                long orderAmount = purchase.getTotalPrice().multiply(new BigDecimal(100)).longValueExact();
                if (checkAmount && vnpAmount != orderAmount) {
                    log.error("IPN Error: Invalid amount for TxnRef: {}. Expected: {}, Received: {}", purchaseId,
                            orderAmount, vnpAmount);
                    response.put("RspCode", "04");
                    response.put("Message", "Invalid amount");
                    return response;
                }

                String vnp_ResponseCode = fields.get("vnp_ResponseCode");
                String vnp_TransactionNo = fields.get("vnp_TransactionNo");
                String vnp_BankCode = fields.getOrDefault("vnp_BankCode", "");
                String vnp_CardType = fields.getOrDefault("vnp_CardType", "");

                if ("00".equals(vnp_ResponseCode)) {
                    log.info("IPN Success: Payment successful for TxnRef: {}", purchaseId);
                    processSuccessfulPaymentInternal(purchase, vnp_TransactionNo,
                            "VNPay - " + vnp_BankCode + " " + vnp_CardType, successStatus);
                    response.put("RspCode", "00");
                    response.put("Message", "Confirm Success");
                    return response;
                } else {
                    log.warn("IPN Failed: Payment failed for TxnRef: {} with ResponseCode: {}", purchaseId,
                            vnp_ResponseCode);
                    processFailedPaymentInternal(purchase, "VNPay Response Code: " + vnp_ResponseCode, failedStatus);
                    response.put("RspCode", "00");
                    response.put("Message", "Confirm Success (but transaction failed)");
                }
            } else {
                log.error("IPN Error: Invalid SecureHash for TxnRef: {}", fields.get("vnp_TxnRef"));
                response.put("RspCode", "97");
                response.put("Message", "Invalid Checksum");
            }
        } catch (Exception e) {
            log.error("IPN Error: Exception processing IPN", e);
            response.put("RspCode", "99");
            response.put("Message", "Unknown error");
        }
        return response;
    }

    private void processSuccessfulPaymentInternal(TicketPurchase purchase, String transactionId, String paymentMethod,
            StatusCode successStatus) {
        purchase.setStatus(successStatus);
        purchase.setTransactionId(transactionId);
        purchase.setPaymentMethod(paymentMethod);
        ticketPurchaseRepository.save(purchase);
        log.info("Processed successful payment for purchase {}", purchase.getId());

        // gui mail va tao qr
        byte[] qrCode = qrCodeService.genarateQrCodeForPurchase(purchase.getId());
        emailService.sendQrCode(purchase.getUser().getEmail(), purchase.getUser().getFullName(), qrCode);

    }

    private void processFailedPaymentInternal(TicketPurchase purchase, String failureReason, StatusCode failedStatus) {
        purchase.setStatus(failedStatus);

        ticketPurchaseRepository.save(purchase);
        log.warn("Processed failed payment for purchase {}. Reason: {}", purchase.getId(), failureReason);

        Ticket ticket = purchase.getTicket();
        ticket.setAvailableQuantity(ticket.getAvailableQuantity() + purchase.getQuantity());
        ticketRepository.save(ticket);
        log.info("Returned {} tickets for failed purchase {} on ticket {}", purchase.getQuantity(), purchase.getId(),
                ticket.getId());
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

}
