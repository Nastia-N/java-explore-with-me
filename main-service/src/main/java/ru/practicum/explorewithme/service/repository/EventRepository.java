package ru.practicum.explorewithme.service.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.explorewithme.service.model.Event;
import ru.practicum.explorewithme.service.model.EventState;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    Page<Event> findAllByInitiatorId(Long initiatorId, Pageable pageable);

    Optional<Event> findByIdAndInitiatorId(Long eventId, Long initiatorId);

    @Query("SELECT e FROM Event e " +
            "WHERE (:users IS NULL OR e.initiator.id IN :users) " +
            "AND (:states IS NULL OR e.state IN :states) " +
            "AND (:categories IS NULL OR e.category.id IN :categories) " +
            "AND (:rangeStart IS NULL OR e.eventDate >= :rangeStart) " +
            "AND (:rangeEnd IS NULL OR e.eventDate <= :rangeEnd)")
    Page<Event> findAllByAdminFilters(
            @Param("users") List<Long> users,
            @Param("states") List<EventState> states,
            @Param("categories") List<Long> categories,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd,
            Pageable pageable);

    @Query(value = """
    SELECT e.* FROM events e\s
    WHERE (:users IS NULL OR e.initiator_id IN (:users))\s
    AND (:states IS NULL OR e.state IN (:states))\s
    AND (:categories IS NULL OR e.category_id IN (:categories))\s
    AND (:rangeStart IS NULL OR e.event_date >= :rangeStart)\s
    AND (:rangeEnd IS NULL OR e.event_date <= :rangeEnd)\s
    ORDER BY e.id\s
    OFFSET :offset ROWS FETCH NEXT :size ROWS ONLY
   \s""",
            nativeQuery = true)
    List<Event> findAllByAdminFiltersNative(
            @Param("users") List<Long> users,
            @Param("states") List<String> states,
            @Param("categories") List<Long> categories,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd,
            @Param("offset") int offset,
            @Param("size") int size);

    boolean existsByCategoryId(Long categoryId);

    List<Event> findAllByIdIn(List<Long> ids);

    List<Event> findAllByIdInAndState(List<Long> ids, EventState state);

    @Query("SELECT e FROM Event e WHERE e.state = 'PUBLISHED'")
    Page<Event> findPublishedEvents(Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.state = 'PUBLISHED' AND " +
            "(LOWER(e.annotation) LIKE LOWER(CONCAT('%', :text, '%')) OR " +
            "LOWER(e.description) LIKE LOWER(CONCAT('%', :text, '%')))")
    Page<Event> findPublishedEventsWithText(@Param("text") String text, Pageable pageable);

    @Query(value = """
    SELECT e.* FROM events e\s
    WHERE e.state = 'PUBLISHED'\s
    AND (:text IS NULL OR :text = '' OR\s
         LOWER(e.annotation) LIKE LOWER(CONCAT('%', :text, '%')) OR\s
         LOWER(e.description) LIKE LOWER(CONCAT('%', :text, '%')))\s
    AND (:categories IS NULL OR e.category_id IN (:categories))\s
    AND (:paid IS NULL OR e.paid = CAST(:paid AS BOOLEAN))\s
    AND (:rangeStart IS NULL OR e.event_date >= :rangeStart)\s
    AND (:rangeEnd IS NULL OR e.event_date <= :rangeEnd)\s
    AND (:onlyAvailable = false OR e.participant_limit = 0 OR e.confirmed_requests < e.participant_limit)
    ORDER BY e.event_date\s
    OFFSET :offset ROWS FETCH NEXT :size ROWS ONLY
   \s""",
            nativeQuery = true)
    List<Event> findPublicEvents(
            @Param("text") String text,
            @Param("categories") List<Long> categories,
            @Param("paid") Boolean paid,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd,
            @Param("onlyAvailable") Boolean onlyAvailable,
            @Param("offset") int offset,
            @Param("size") int size);
}
