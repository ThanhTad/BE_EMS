package io.event.ems.repository;

import io.event.ems.model.Venue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface VenueRepository extends JpaRepository<Venue, UUID> {


    /**
     * Tìm kiếm địa điểm theo tên (không phân biệt chữ hoa/thường).
     *
     * @param name     Tên địa điểm hoặc một phần của tên.
     * @param pageable Thông tin phân trang.
     * @return Một trang các địa điểm khớp.
     */
    Page<Venue> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * Kiểm tra xem một địa điểm với tên cho trước đã tồn tại hay chưa.
     *
     * @param name Tên địa điểm.
     * @return true nếu tồn tại, ngược lại false.
     */
    boolean existsByNameIgnoreCase(String name);

    @Query("SELECT v FROM Venue v WHERE LOWER(v.name) = LOWER(:name) AND v.id <> :id")
    Optional<Venue> findByNameIgnoreCaseAndIdNot(@Param("name") String name, @Param("id") UUID id);
}
