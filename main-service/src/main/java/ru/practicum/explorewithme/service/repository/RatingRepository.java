package ru.practicum.explorewithme.service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.explorewithme.service.model.Rating;

import java.util.List;
import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    Optional<Rating> findByEventIdAndUserId(Long eventId, Long userId);

    boolean existsByEventIdAndUserId(Long eventId, Long userId);

    List<Rating> findByUserId(Long userId);

    List<Rating> findByEventId(Long eventId);

    long countByEventIdAndValue(Long eventId, Integer value);

    @Query("SELECT COALESCE(SUM(r.value), 0) FROM Rating r WHERE r.event.id = :eventId")
    Integer sumByEventId(@Param("eventId") Long eventId);

}
