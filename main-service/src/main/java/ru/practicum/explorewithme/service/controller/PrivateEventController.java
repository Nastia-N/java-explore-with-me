package ru.practicum.explorewithme.service.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.explorewithme.service.dto.EventFullDto;
import ru.practicum.explorewithme.service.dto.EventShortDto;
import ru.practicum.explorewithme.service.dto.NewEventDto;
import ru.practicum.explorewithme.service.dto.UpdateEventUserRequest;
import ru.practicum.explorewithme.service.service.EventService;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}/events")
@RequiredArgsConstructor
@Slf4j
@Validated
public class PrivateEventController {

    private final EventService eventService;

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
}
