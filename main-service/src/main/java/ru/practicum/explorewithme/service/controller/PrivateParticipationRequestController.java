package ru.practicum.explorewithme.service.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.explorewithme.service.dto.EventRequestStatusUpdateRequest;
import ru.practicum.explorewithme.service.dto.EventRequestStatusUpdateResult;
import ru.practicum.explorewithme.service.dto.ParticipationRequestDto;
import ru.practicum.explorewithme.service.service.ParticipationRequestService;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}")
@RequiredArgsConstructor
@Slf4j
@Validated
public class PrivateParticipationRequestController {

    private final ParticipationRequestService participationRequestService;

    @GetMapping("/requests")
    public List<ParticipationRequestDto> getUserRequests(@PathVariable @Positive Long userId) {
        log.info("Получение запросов на участие пользователя {}", userId);
        return participationRequestService.getUserRequests(userId);
    }

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationRequestDto createRequest(@PathVariable @Positive Long userId,
                                                 @RequestParam @Positive Long eventId) {
        log.info("Пользователь {} создает запрос на участие в событии {}", userId, eventId);
        return participationRequestService.createRequest(userId, eventId);
    }

    @PatchMapping("/requests/{requestId}/cancel")
    public ParticipationRequestDto cancelRequest(@PathVariable @Positive Long userId,
                                                 @PathVariable @Positive Long requestId) {
        log.info("Пользователь {} отменяет запрос {}", userId, requestId);
        return participationRequestService.cancelRequest(userId, requestId);
    }

    @GetMapping("/events/{eventId}/requests")
    public List<ParticipationRequestDto> getEventRequests(@PathVariable @Positive Long userId,
                                                          @PathVariable @Positive Long eventId) {
        log.info("Получение запросов на участие в событии {} пользователя {}", eventId, userId);
        return participationRequestService.getEventRequests(userId, eventId);
    }

    @PatchMapping("/events/{eventId}/requests")
    public EventRequestStatusUpdateResult updateRequestStatus(
            @PathVariable @Positive Long userId,
            @PathVariable @Positive Long eventId,
            @Valid @RequestBody EventRequestStatusUpdateRequest updateRequest) {
        log.info("Пользователь {} обновляет статус запросов для события {}", userId, eventId);
        return participationRequestService.updateRequestStatus(userId, eventId, updateRequest);
    }
}
