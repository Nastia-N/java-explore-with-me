package ru.practicum.explorewithme.service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.explorewithme.service.dto.EventRequestStatusUpdateRequest;
import ru.practicum.explorewithme.service.dto.EventRequestStatusUpdateRequest.StatusAction;
import ru.practicum.explorewithme.service.dto.EventRequestStatusUpdateResult;
import ru.practicum.explorewithme.service.dto.ParticipationRequestDto;
import ru.practicum.explorewithme.service.exception.*;
import ru.practicum.explorewithme.service.mapper.ParticipationRequestMapper;
import ru.practicum.explorewithme.service.model.*;
import ru.practicum.explorewithme.service.repository.EventRepository;
import ru.practicum.explorewithme.service.repository.ParticipationRequestRepository;
import ru.practicum.explorewithme.service.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParticipationRequestServiceImpl implements ParticipationRequestService {

    private final ParticipationRequestRepository requestRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final ParticipationRequestMapper requestMapper;

    @Override
    @Transactional
    public ParticipationRequestDto createRequest(Long userId, Long eventId) {
        log.info("Создание запроса на участие пользователем ID: {} в событии ID: {}", userId, eventId);

        User requester = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User", userId));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event", eventId));

        if (event.getInitiator().getId().equals(userId)) {
            throw new BusinessConflictException("Инициатор события не может добавить заявку на участие в своём событии");
        }

        if (event.getState() != EventState.PUBLISHED) {
            throw new BusinessConflictException("Нельзя участвовать в неопубликованном событии");
        }

        if (requestRepository.existsByEventIdAndRequesterId(eventId, userId)) {
            throw new BusinessConflictException("Нельзя добавить повторный запрос");
        }

        if (event.getParticipantLimit() > 0 &&
                event.getConfirmedRequests() >= event.getParticipantLimit()) {
            throw new BusinessConflictException("Достигнут лимит запросов на участие");
        }

        ParticipationRequest request = ParticipationRequest.builder()
                .event(event)
                .requester(requester)
                .status(RequestStatus.PENDING)
                .created(LocalDateTime.now())
                .build();

        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            request.setStatus(RequestStatus.CONFIRMED);
            event.setConfirmedRequests(event.getConfirmedRequests() + 1);
            eventRepository.save(event);
        }

        ParticipationRequest savedRequest = requestRepository.save(request);
        log.info("Создан запрос на участие с ID: {}", savedRequest.getId());

        return requestMapper.toDto(savedRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        log.info("Получение запросов на участие пользователя ID: {}", userId);

        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User", userId);
        }

        return requestRepository.findAllByRequesterId(userId)
                .stream()
                .map(requestMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        log.info("Отмена запроса на участие ID: {} пользователем ID: {}", requestId, userId);

        ParticipationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Request", requestId));

        if (!request.getRequester().getId().equals(userId)) {
            throw new RequestValidationException("Пользователь может отменять только свои запросы");
        }

        if (request.getStatus() == RequestStatus.CANCELED) {
            throw new RequestValidationException("Запрос уже отменен");
        }

        if (request.getStatus() == RequestStatus.CONFIRMED) {
            Event event = request.getEvent();
            event.setConfirmedRequests(event.getConfirmedRequests() - 1);
            eventRepository.save(event);
        }

        request.setStatus(RequestStatus.CANCELED);
        ParticipationRequest updatedRequest = requestRepository.save(request);

        log.info("Запрос на участие ID: {} отменен", requestId);
        return requestMapper.toDto(updatedRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        log.info("Получение запросов на участие в событии ID: {} пользователем ID: {}", eventId, userId);

        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event", eventId));

        return requestRepository.findAllByEventId(eventId)
                .stream()
                .map(requestMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateRequestStatus(Long userId, Long eventId,
                                                              EventRequestStatusUpdateRequest updateRequest) {
        log.info("Обновление статуса запросов для события ID: {} пользователем ID: {}", eventId, userId);

        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event", eventId));

        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            throw new BusinessConflictException("Для этого события не требуется модерация запросов");
        }

        List<ParticipationRequest> requests = requestRepository.findAllByIdIn(updateRequest.getRequestIds());

        if (requests.size() != updateRequest.getRequestIds().size()) {
            throw new NotFoundException("Некоторые запросы не найдены");
        }

        for (ParticipationRequest request : requests) {
            if (request.getStatus() != RequestStatus.PENDING) {
                throw new BusinessConflictException("Можно изменить статус только у заявок, находящихся в состоянии ожидания");
            }
            if (!request.getEvent().getId().equals(eventId)) {
                throw new BusinessConflictException("Запрос не относится к указанному событию");
            }
        }

        List<ParticipationRequestDto> confirmedRequests = new ArrayList<>();
        List<ParticipationRequestDto> rejectedRequests = new ArrayList<>();

        if (updateRequest.getStatus() == StatusAction.CONFIRMED) {
            processConfirmedRequests(event, requests, confirmedRequests, rejectedRequests);
        } else {
            processRejectedRequests(requests, rejectedRequests);
        }

        return new EventRequestStatusUpdateResult(confirmedRequests, rejectedRequests);
    }

    private void processConfirmedRequests(Event event, List<ParticipationRequest> requests,
                                          List<ParticipationRequestDto> confirmedRequests,
                                          List<ParticipationRequestDto> rejectedRequests) {
        int confirmedCount = event.getConfirmedRequests();
        int participantLimit = event.getParticipantLimit();

        for (ParticipationRequest request : requests) {
            if (participantLimit == 0 || confirmedCount < participantLimit) {
                request.setStatus(RequestStatus.CONFIRMED);
                confirmedCount++;
                confirmedRequests.add(requestMapper.toDto(request));
            } else {
                throw new BusinessConflictException("Достигнут лимит одобренных заявок");
            }
        }

        event.setConfirmedRequests(confirmedCount);
        eventRepository.save(event);
        requestRepository.saveAll(requests);

        if (participantLimit > 0 && confirmedCount >= participantLimit) {
            rejectPendingRequests(event.getId());
        }
    }

    private void processRejectedRequests(List<ParticipationRequest> requests,
                                         List<ParticipationRequestDto> rejectedRequests) {
        for (ParticipationRequest request : requests) {
            request.setStatus(RequestStatus.REJECTED);
            rejectedRequests.add(requestMapper.toDto(request));
        }
        requestRepository.saveAll(requests);
    }

    private void rejectPendingRequests(Long eventId) {
        List<ParticipationRequest> pendingRequests = requestRepository
                .findAllByEventIdAndStatus(eventId, RequestStatus.PENDING);

        for (ParticipationRequest request : pendingRequests) {
            request.setStatus(RequestStatus.REJECTED);
        }

        if (!pendingRequests.isEmpty()) {
            requestRepository.saveAll(pendingRequests);
        }
    }
}