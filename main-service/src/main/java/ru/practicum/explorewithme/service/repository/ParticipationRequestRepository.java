package ru.practicum.explorewithme.service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.explorewithme.service.model.ParticipationRequest;
import ru.practicum.explorewithme.service.model.RequestStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {

    List<ParticipationRequest> findAllByRequesterId(Long requesterId);

    List<ParticipationRequest> findAllByEventId(Long eventId);

    List<ParticipationRequest> findAllByEventIdAndStatus(Long eventId, RequestStatus status);

    List<ParticipationRequest> findAllByEventIdAndStatusIn(Long eventId, List<RequestStatus> statuses);

    Optional<ParticipationRequest> findByEventIdAndRequesterId(Long eventId, Long requesterId);

    @Query("SELECT COUNT(pr) FROM ParticipationRequest pr " +
            "WHERE pr.event.id = :eventId AND pr.status = 'CONFIRMED'")
    long countConfirmedRequestsByEventId(@Param("eventId") Long eventId);

    boolean existsByEventIdAndRequesterId(Long eventId, Long requesterId);

    List<ParticipationRequest> findAllByIdIn(List<Long> requestIds);

}
