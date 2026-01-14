package ru.practicum.explorewithme.service.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import ru.practicum.explorewithme.service.model.Event;
import ru.practicum.explorewithme.service.model.EventState;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class EventRepositoryCustomImpl implements EventRepositoryCustom {

    private final EntityManager entityManager;

    @Override
    public List<Event> findPublicEvents(String text,
                                        List<Long> categories,
                                        Boolean paid,
                                        LocalDateTime rangeStart,
                                        LocalDateTime rangeEnd,
                                        Boolean onlyAvailable,
                                        int from,
                                        int size) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Event> cq = cb.createQuery(Event.class);
        Root<Event> event = cq.from(Event.class);

        List<Predicate> predicates = new ArrayList<>();

        predicates.add(cb.equal(event.get("state"), EventState.PUBLISHED));

        if (text != null && !text.trim().isEmpty()) {
            String searchText = "%" + text.toLowerCase() + "%";
            Predicate inAnnotation = cb.like(cb.lower(event.get("annotation")), searchText);
            Predicate inDescription = cb.like(cb.lower(event.get("description")), searchText);
            predicates.add(cb.or(inAnnotation, inDescription));
        }

        if (categories != null && !categories.isEmpty()) {
            predicates.add(event.get("category").get("id").in(categories));
        }

        if (paid != null) {
            predicates.add(cb.equal(event.get("paid"), paid));
        }

        if (rangeStart != null) {
            predicates.add(cb.greaterThanOrEqualTo(event.get("eventDate"), rangeStart));
        }

        if (rangeEnd != null) {
            predicates.add(cb.lessThanOrEqualTo(event.get("eventDate"), rangeEnd));
        }

        if (onlyAvailable != null && onlyAvailable) {
            Predicate noLimit = cb.equal(event.get("participantLimit"), 0);
            Predicate limitNotReached = cb.lessThan(
                    event.get("confirmedRequests"),
                    event.get("participantLimit")
            );
            predicates.add(cb.or(noLimit, limitNotReached));
        }

        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        cq.orderBy(cb.asc(event.get("eventDate")));

        TypedQuery<Event> query = entityManager.createQuery(cq);
        query.setFirstResult(from);
        query.setMaxResults(size);

        return query.getResultList();
    }

    @Override
    public List<Event> findAllByAdminFilters(List<Long> users,
                                             List<EventState> states,
                                             List<Long> categories,
                                             LocalDateTime rangeStart,
                                             LocalDateTime rangeEnd,
                                             int from,
                                             int size) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Event> cq = cb.createQuery(Event.class);
        Root<Event> event = cq.from(Event.class);

        List<Predicate> predicates = new ArrayList<>();

        if (users != null && !users.isEmpty()) {
            predicates.add(event.get("initiator").get("id").in(users));
        }

        if (states != null && !states.isEmpty()) {
            predicates.add(event.get("state").in(states));
        }

        if (categories != null && !categories.isEmpty()) {
            predicates.add(event.get("category").get("id").in(categories));
        }

        if (rangeStart != null) {
            predicates.add(cb.greaterThanOrEqualTo(event.get("eventDate"), rangeStart));
        }

        if (rangeEnd != null) {
            predicates.add(cb.lessThanOrEqualTo(event.get("eventDate"), rangeEnd));
        }

        if (!predicates.isEmpty()) {
            cq.where(cb.and(predicates.toArray(new Predicate[0])));
        }

        cq.orderBy(cb.asc(event.get("id")));

        TypedQuery<Event> query = entityManager.createQuery(cq);
        query.setFirstResult(from);
        query.setMaxResults(size);

        return query.getResultList();
    }
}
