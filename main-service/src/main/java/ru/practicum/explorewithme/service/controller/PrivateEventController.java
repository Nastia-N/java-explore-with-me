package ru.practicum.explorewithme.service.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.explorewithme.service.dto.*;
import ru.practicum.explorewithme.service.service.EventService;
import ru.practicum.explorewithme.service.service.ParticipationRequestService;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}/events")
@RequiredArgsConstructor
@Slf4j
@Validated
public class PrivateEventController {

    private final EventService eventService;
    private final ParticipationRequestService participationRequestService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventFullDto createEvent(@PathVariable @Positive Long userId,
                                    @Valid @RequestBody NewEventDto newEventDto) {
        log.info("Пользователь {} создает новое событие: {}", userId, newEventDto.getTitle());
        return eventService.createEvent(userId, newEventDto);
    }

    @GetMapping
    public List<EventShortDto> getUserEvents(@PathVariable @Positive Long userId,
                                             @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
                                             @RequestParam(defaultValue = "10") @Positive Integer size) {
        log.info("Получение событий пользователя {} (from={}, size={})", userId, from, size);
        return eventService.getUserEvents(userId, from, size);
    }

    @GetMapping("/{eventId}")
    public EventFullDto getUserEvent(@PathVariable @Positive Long userId,
                                     @PathVariable @Positive Long eventId) {
        log.info("Получение события {} пользователя {}", eventId, userId);
        return eventService.getUserEvent(userId, eventId);
    }

    @PatchMapping("/{eventId}")
    public EventFullDto updateUserEvent(@PathVariable @Positive Long userId,
                                        @PathVariable @Positive Long eventId,
                                        @Valid @RequestBody UpdateEventUserRequest updateRequest) {
        log.info("Пользователь {} обновляет событие {}", userId, eventId);
        return eventService.updateUserEvent(userId, eventId, updateRequest);
    }

    @GetMapping("/{eventId}/requests")
    public List<ParticipationRequestDto> getEventRequests(
            @PathVariable @Positive Long userId,
            @PathVariable @Positive Long eventId) {
        log.info("Получение запросов на участие в событии {} пользователя {}", eventId, userId);
        return participationRequestService.getEventRequests(userId, eventId);
    }

    @PatchMapping("/{eventId}/requests")
    public EventRequestStatusUpdateResult updateRequestStatus(
            @PathVariable @Positive Long userId,
            @PathVariable @Positive Long eventId,
            @Valid @RequestBody EventRequestStatusUpdateRequest updateRequest) {
        log.info("Пользователь {} обновляет статус запросов для события {}", userId, eventId);
        return participationRequestService.updateRequestStatus(userId, eventId, updateRequest);
    }
}
