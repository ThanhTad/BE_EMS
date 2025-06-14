package io.event.ems.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import io.event.ems.dto.MultiItemPurchaseRequestDTO;
import io.event.ems.dto.PaymentResponseDTO;
import io.event.ems.dto.TicketPurchaseDTO;
import io.event.ems.dto.TicketPurchaseDetailDTO;
import io.event.ems.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;

public interface TicketPurchaseService {

        Page<TicketPurchaseDTO> getAllTicketPurchases(Pageable pageable);

        Optional<TicketPurchaseDTO> getTicketPurchaseById(UUID id) throws ResourceNotFoundException;

        PaymentResponseDTO initiateTicketPurchase(MultiItemPurchaseRequestDTO request, String clientIpAddress)
                        throws ResourceNotFoundException;

        TicketPurchaseDTO updateTicketPurchaseStatus(UUID id, TicketPurchaseDTO ticketPurchaseDTO)
                        throws ResourceNotFoundException;

        void deleteTicketPurchase(UUID id) throws ResourceNotFoundException;

        Page<TicketPurchaseDTO> getTicketPurchasesByUserId(UUID userId, Pageable pageable);

        Page<TicketPurchaseDTO> getTicketPurchasesByTicketId(UUID ticketId, Pageable pageable);

        Page<TicketPurchaseDTO> getTicketPurchasesByStatusId(Integer statusId, Pageable pageable);

        List<TicketPurchaseDTO> getTicketPurchasesByTransactionId(String transactionId);

        Map<String, String> processVnPayIpn(HttpServletRequest request);

        boolean verifyVnPayReturn(HttpServletRequest request);

        List<TicketPurchaseDTO> confirmPurchaseByGroup(String transactionId) throws ResourceNotFoundException;

        Optional<TicketPurchaseDetailDTO> getTicketPurchaseDetailById(UUID id);

        Page<TicketPurchaseDetailDTO> getTicketPurchaseDetailsByUserId(UUID userId, Pageable pageable);

}
