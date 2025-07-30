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

    /**
     * Lấy một trang các đơn hàng của một người dùng cụ thể.
     * Sử dụng JOIN FETCH để tải đồng thời User, Event, và Status trong một query duy nhất,
     * giải quyết triệt để vấn đề N+1 select và đảm bảo hiệu suất cao.
     *
     * @param userId   ID của người dùng.
     * @param pageable Thông tin phân trang.
     * @return Một trang (Page) các đối tượng TicketPurchase đã được tải đầy đủ thông tin ManyToOne.
     */
    @Query(value = "SELECT tp FROM TicketPurchase tp " +
            "LEFT JOIN FETCH tp.user u " +
            "LEFT JOIN FETCH tp.event e " +
            "LEFT JOIN FETCH tp.status s " +
            "WHERE tp.user.id = :userId",
            countQuery = "SELECT COUNT(tp) FROM TicketPurchase tp WHERE tp.user.id = :userId")
    Page<TicketPurchase> findByUserIdWithDetails(UUID userId, Pageable pageable);

    /**
     * Lấy một trang các đơn hàng của một sự kiện cụ thể.
     * Tối ưu hóa bằng JOIN FETCH để tránh N+1 query.
     *
     * @param eventId  ID của sự kiện.
     * @param pageable Thông tin phân trang.
     * @return Một trang (Page) các đối tượng TicketPurchase.
     */
    @Query(value = "SELECT tp FROM TicketPurchase tp " +
            "LEFT JOIN FETCH tp.user u " +
            "LEFT JOIN FETCH tp.event e " +
            "LEFT JOIN FETCH tp.status s " +
            "WHERE tp.event.id = :eventId",
            countQuery = "SELECT COUNT(tp) FROM TicketPurchase tp WHERE tp.event.id = :eventId")
    Page<TicketPurchase> findByEventIdWithDetails(UUID eventId, Pageable pageable);

    /**
     * Lấy chi tiết một đơn hàng cụ thể, đảm bảo nó thuộc về người dùng đang yêu cầu.
     * Tải các thông tin ManyToOne liên quan.
     *
     * @param id     ID của đơn hàng.
     * @param userId ID của người dùng.
     * @return Optional chứa TicketPurchase với chi tiết ManyToOne nếu tìm thấy.
     */
    @Query("SELECT tp FROM TicketPurchase tp " +
            "LEFT JOIN FETCH tp.user " +
            "LEFT JOIN FETCH tp.event " +
            "LEFT JOIN FETCH tp.status " +
            "WHERE tp.id = :id AND tp.user.id = :userId")
    Optional<TicketPurchase> findByIdAndUserIdWithDetails(UUID id, UUID userId);

    /**
     * Lấy chi tiết một đơn hàng cụ thể (dành cho Admin).
     * Tải các thông tin ManyToOne liên quan.
     *
     * @param id ID của đơn hàng.
     * @return Optional chứa TicketPurchase với chi tiết ManyToOne nếu tìm thấy.
     */
    @Query("SELECT tp FROM TicketPurchase tp " +
            "LEFT JOIN FETCH tp.user " +
            "LEFT JOIN FETCH tp.event " +
            "LEFT JOIN FETCH tp.status " +
            "WHERE tp.id = :id")
    Optional<TicketPurchase> findByIdWithDetails(UUID id);
}
