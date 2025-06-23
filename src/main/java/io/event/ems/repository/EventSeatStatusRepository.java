package io.event.ems.repository;

import io.event.ems.model.EventSeatStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface EventSeatStatusRepository extends JpaRepository<EventSeatStatus, UUID> {

    List<EventSeatStatus> findByTicketPurchaseId(UUID ticketPurchaseId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ess FROM EventSeatStatus ess " +
            "JOIN FETCH ess.seat s " +
            "JOIN FETCH s.section sc " +
            "JOIN FETCH ess.ticket t " +
            "WHERE ess.id IN :ids")
    List<EventSeatStatus> findAllByIdInWithDetailsAndLock(@Param("ids") List<UUID> ids);

    // Phương thức mới để cập nhật trạng thái sau khi thanh toán
    @Modifying
    @Query("UPDATE EventSeatStatus ess SET ess.status = 'sold', ess.ticketPurchase.id = :purchaseId, ess.ticket.id = :ticketId, ess.priceAtPurchase = :price " +
            "WHERE ess.seat.id IN :seatIds")
    int confirmSeatsAsSold(@Param("seatIds") List<UUID> seatIds,
                           @Param("purchaseId") UUID purchaseId,
                           @Param("ticketId") UUID ticketId,
                           @Param("price") BigDecimal price);
}
