package ru.practicum.explorewithme.service.service;

import ru.practicum.explorewithme.service.dto.EventRequestStatusUpdateRequest;
import ru.practicum.explorewithme.service.dto.EventRequestStatusUpdateResult;
import ru.practicum.explorewithme.service.dto.ParticipationRequestDto;

import java.util.List;

public interface ParticipationRequestService {

    ParticipationRequestDto createRequest(Long userId, Long eventId);
    List<ParticipationRequestDto> getUserRequests(Long userId);
    ParticipationRequestDto cancelRequest(Long userId, Long requestId);

    List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId);
    EventRequestStatusUpdateResult updateRequestStatus(Long userId, Long eventId,
                                                       EventRequestStatusUpdateRequest updateRequest);
}
