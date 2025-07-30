package io.event.ems.repository;

import io.event.ems.model.Category;
import io.event.ems.model.Event;
import io.event.ems.model.Venue;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class EventSpecification {

    public static Specification<Event> withDynamicQuery(
            String keyword,
            List<UUID> categoryIds, // <-- ĐÃ THAY ĐỔI: Từ UUID sang List<UUID>
            Integer statusId,
            Boolean isPublic,
            LocalDateTime startDate,
            LocalDateTime endDate) {

        return (root, query, criteriaBuilder) -> {
            // Bắt đầu với một predicate luôn đúng, các điều kiện sau sẽ được AND vào
            Predicate predicate = criteriaBuilder.conjunction();

            // 1. Lọc theo Keyword (title, description, venue name/address)
            if (StringUtils.hasText(keyword)) {
                Join<Event, Venue> venueJoin = root.join("venue", JoinType.LEFT);
                String pattern = "%" + keyword.toLowerCase() + "%";

                Predicate keywordPredicate = criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(venueJoin.get("name")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(venueJoin.get("address")), pattern)
                );
                predicate = criteriaBuilder.and(predicate, keywordPredicate);
            }

            // 2. Lọc theo Danh sách Category
            // =================================================================
            // === PHẦN THAY ĐỔI CHÍNH NẰM Ở ĐÂY ===
            // =================================================================
            if (!CollectionUtils.isEmpty(categoryIds)) {
                // Join vào bảng quan hệ nhiều-nhiều 'categories'
                Join<Event, Category> categoryJoin = root.join("categories");
                // Sử dụng điều kiện "IN" để kiểm tra xem ID của category có nằm trong danh sách được cung cấp không
                predicate = criteriaBuilder.and(predicate, categoryJoin.get("id").in(categoryIds));

                // QUAN TRỌNG: Thêm DISTINCT để tránh một sự kiện bị lặp lại
                // nếu nó thuộc nhiều hơn một category được người dùng chọn.
                query.distinct(true);
            }
            // =================================================================

            // 3. Lọc theo Status
            if (statusId != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("status").get("id"), statusId));
            }

            // 4. Lọc theo isPublic
            if (isPublic != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("isPublic"), isPublic));
            }

            // 5. Lọc theo khoảng thời gian
            if (startDate != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.greaterThanOrEqualTo(root.get("startDate"), startDate));
            }

            if (endDate != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.lessThanOrEqualTo(root.get("endDate"), endDate));
            }

            return predicate;
        };
    }
}