package ru.practicum.explorewithme.service.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.explorewithme.service.model.Event;
import ru.practicum.explorewithme.service.model.EventState;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long>, EventRepositoryCustom {

    Page<Event> findAllByInitiatorId(Long initiatorId, Pageable pageable);

    Optional<Event> findByIdAndInitiatorId(Long eventId, Long initiatorId);

    boolean existsByCategoryId(Long categoryId);

    List<Event> findAllByIdIn(List<Long> ids);

    List<Event> findAllByIdInAndState(List<Long> ids, EventState state);
}