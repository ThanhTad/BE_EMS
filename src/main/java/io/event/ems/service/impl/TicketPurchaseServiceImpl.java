package io.event.ems.service.impl;

import io.event.ems.dto.PurchaseDetailDTO;
import io.event.ems.dto.PurchaseListItemDTO;
import io.event.ems.exception.ResourceNotFoundException;
import io.event.ems.mapper.PurchaseMapper;
import io.event.ems.model.EventSeatStatus;
import io.event.ems.model.PurchasedGATicket;
import io.event.ems.model.TicketPurchase;
import io.event.ems.repository.EventSeatStatusRepository;
import io.event.ems.repository.PurchasedGATicketRepository;
import io.event.ems.repository.TicketPurchaseRepository;
import io.event.ems.service.TicketPurchaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TicketPurchaseServiceImpl implements TicketPurchaseService {

    private final TicketPurchaseRepository ticketPurchaseRepository;

    private final EventSeatStatusRepository eventSeatStatusRepository;

    private final PurchasedGATicketRepository purchasedGaTicketRepository;

    private final PurchaseMapper mapper;

    @Override
    public Page<PurchaseListItemDTO> getAllPurchases(Pageable pageable) {
        log.info("Fetching all purchases");
        Page<TicketPurchase> purchases = ticketPurchaseRepository.findAll(pageable);
        return purchases.map(mapper::toListItemDTO);
    }

    @Override
    public Page<PurchaseListItemDTO> getPurchasesByUserId(UUID userId, Pageable pageable) {
        log.info("Fetching all purchases for user ID: {}", userId);
        Page<TicketPurchase> purchases = ticketPurchaseRepository.findByUserIdWithDetails(userId, pageable);
        return purchases.map(mapper::toListItemDTO);
    }

    @Override
    public PurchaseDetailDTO getPurchaseDetailsById(UUID purchaseId) {
        log.info("Fetching purchase details for ID: {}", purchaseId);
        TicketPurchase purchase = ticketPurchaseRepository.findByIdWithDetails(purchaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase not found with id: " + purchaseId));
        return buildPurchaseDetailDTO(purchase);
    }

    @Override
    public PurchaseDetailDTO getPurchaseDetailsByIdForUser(UUID purchaseId, UUID userId) {
        log.info("Fetching purchase details for ID: {} for user ID: {}", purchaseId, userId);
        TicketPurchase purchase = ticketPurchaseRepository.findByIdAndUserIdWithDetails(purchaseId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase not found with id: " + purchaseId));
        return buildPurchaseDetailDTO(purchase);
    }

    private PurchaseDetailDTO buildPurchaseDetailDTO(TicketPurchase purchase) {
        // Sử dụng mapper để chuyển đổi các trường cơ bản
        PurchaseDetailDTO dto = mapper.toDetailDTO(purchase);

        // **THAY ĐỔI Ở ĐÂY**
        // 2. Truy vấn riêng để lấy vé GA và map chúng
        List<PurchasedGATicket> gaTickets = purchasedGaTicketRepository.findByTicketPurchaseId(purchase.getId());
        dto.setGeneralAdmissionTickets(mapper.purchasedGaTicketsToDTOs(gaTickets));

        // 3. Truy vấn riêng để lấy vé ngồi và map chúng
        List<EventSeatStatus> seatedTickets = eventSeatStatusRepository.findByTicketPurchaseId(purchase.getId());
        dto.setSeatedTickets(mapper.toPurchasedSeatedTicketDTOs(seatedTickets));

        return dto;
    }
}
