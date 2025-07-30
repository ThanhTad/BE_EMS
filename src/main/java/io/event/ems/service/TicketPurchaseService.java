package io.event.ems.service;

import io.event.ems.dto.PurchaseDetailDTO;
import io.event.ems.dto.PurchaseListItemDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface TicketPurchaseService {

    /**
     * Lấy danh sách tất cả các đơn hàng (cho Admin).
     *
     * @param pageable Thông tin phân trang.
     * @return Một trang các đơn hàng tóm tắt.
     */
    Page<PurchaseListItemDTO> getAllPurchases(Pageable pageable);

    /**
     * Lấy danh sách các đơn hàng của một người dùng cụ thể.
     *
     * @param userId   ID của người dùng.
     * @param pageable Thông tin phân trang.
     * @return Một trang các đơn hàng tóm tắt của người dùng.
     */
    Page<PurchaseListItemDTO> getPurchasesByUserId(UUID userId, Pageable pageable);

    /**
     * Lấy thông tin chi tiết của một đơn hàng.
     *
     * @param purchaseId ID của đơn hàng.
     * @return DTO chứa thông tin chi tiết đơn hàng.
     */
    PurchaseDetailDTO getPurchaseDetailsById(UUID purchaseId);

    /**
     * (Nâng cao) Lấy chi tiết đơn hàng nhưng có kiểm tra quyền sở hữu.
     *
     * @param purchaseId ID của đơn hàng.
     * @param userId     ID của người dùng đang yêu cầu.
     * @return DTO chi tiết nếu người dùng có quyền.
     */
    PurchaseDetailDTO getPurchaseDetailsByIdForUser(UUID purchaseId, UUID userId);

}
