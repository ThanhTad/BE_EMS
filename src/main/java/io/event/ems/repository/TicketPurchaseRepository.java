package io.event.ems.repository;

import io.event.ems.model.TicketPurchase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TicketPurchaseRepository extends JpaRepository<TicketPurchase, UUID> {

    Page<TicketPurchase> findByUserId(UUID userId, Pageable pageable);

    Page<TicketPurchase> findByEventId(UUID eventId, Pageable pageable);

    // Lấy chi tiết một hóa đơn cụ thể của một người dùng
    // Dùng @Query để fetch các entity liên quan, tránh N+1 query
    @Query("SELECT tp FROM TicketPurchase tp JOIN FETCH tp.event JOIN FETCH tp.user JOIN FETCH tp.status WHERE tp.id = :id AND tp.user.id = :userId")
    Optional<TicketPurchase> findByIdAndUserId(UUID id, UUID userId);

    @Query("SELECT tp FROM TicketPurchase tp JOIN FETCH tp.event JOIN FETCH tp.user JOIN FETCH tp.status WHERE tp.id = :id")
    Optional<TicketPurchase> findByIdWithDetails(UUID id);
}
