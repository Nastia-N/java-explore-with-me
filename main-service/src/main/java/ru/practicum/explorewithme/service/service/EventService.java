package ru.practicum.explorewithme.service.service;

import jakarta.servlet.http.HttpServletRequest;
import ru.practicum.explorewithme.service.dto.*;

import java.time.LocalDateTime;
import java.util.List;

public interface EventService {

    EventFullDto createEvent(Long userId, NewEventDto newEventDto);
    List<EventShortDto> getUserEvents(Long userId, Integer from, Integer size);
    EventFullDto getUserEvent(Long userId, Long eventId);
    EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest updateRequest);

    List<EventFullDto> searchEvents(List<Long> users, List<String> states, List<Long> categories,
                                    LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                    Integer from, Integer size);
    EventFullDto updateAdminEvent(Long eventId, UpdateEventAdminRequest updateRequest);

    List<EventShortDto> searchPublicEvents(String text, List<Long> categories, Boolean paid,
                                           LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                           Boolean onlyAvailable, String sort,
                                           Integer from, Integer size,
                                           HttpServletRequest request);

    EventFullDto getPublicEvent(Long eventId, HttpServletRequest request);

    EventFullDto getEventById(Long eventId);

}
