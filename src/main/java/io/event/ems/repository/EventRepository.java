package io.event.ems.repository;

import io.event.ems.model.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID>, JpaSpecificationExecutor<Event> {

    Optional<Event> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @Query("SELECT e FROM Event e " +
            "JOIN FETCH e.venue v " +
            "JOIN FETCH e.creator c " +
            "LEFT JOIN FETCH e.status s " +
            "LEFT JOIN FETCH e.seatMap sm " +
            "WHERE e.id = :id")
    Optional<Event> findByIdWithDetails(@Param("id") UUID id);

    @Query("SELECT e FROM Event e " +
            "JOIN FETCH e.venue v " +
            "JOIN FETCH e.creator c " +
            "LEFT JOIN FETCH e.status s " +
            "LEFT JOIN FETCH e.seatMap sm " +
            "WHERE e.slug = :slug")
    Optional<Event> findBySlugWithDetails(@Param("slug") String slug);

    @Query("SELECT e FROM Event e WHERE lower(e.title) LIKE lower(concat('%', :keyword, '%')) AND e.isPublic = true AND e.status.status = 'APPROVED'")
    Page<Event> findPublishedByTitleContainingIgnoreCase(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT e FROM Event e WHERE lower(e.title) LIKE lower(:title) AND e.isPublic = true")
    Optional<Event> findByTitleIgnoreCase(@Param("title") String title);

    Page<Event> findByCreatorId(UUID id, Pageable pageable);

    Page<Event> findByCategories_Id(UUID id, Pageable pageable);

    Page<Event> findByStatusId(Integer id, Pageable pageable);

    Page<Event> findByStartDateBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    Page<Event> findByIsPublicTrueAndStatusId(Integer statusId, Pageable pageable);

    // Simple search
    @Query(
            value = """
                    SELECT * FROM public.events e
                    WHERE
                        e.status_id = :statusId AND
                        e.is_public = true AND
                        e.fts_document @@ websearch_to_tsquery('english', :query)
                    ORDER BY ts_rank_cd(e.fts_document, websearch_to_tsquery('english', :query)) DESC
                    """,
            countQuery = """
                    SELECT count(*) FROM public.events e
                    WHERE
                        e.status_id = :statusId AND
                        e.is_public = true AND
                        e.fts_document @@ websearch_to_tsquery('english', :query)
                    """,
            nativeQuery = true // Đánh dấu đây là một native query
    )
    Page<Event> searchByFullText(
            @Param("query") String query,
            @Param("statusId") Integer statusId,
            Pageable pageable
    );

    boolean existsByVenueId(UUID venueId);
}
