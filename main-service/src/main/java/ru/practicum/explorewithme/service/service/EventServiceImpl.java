package ru.practicum.explorewithme.service.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.explorewithme.client.StatsClient;
import ru.practicum.explorewithme.dto.EndpointHit;
import ru.practicum.explorewithme.dto.ViewStats;
import ru.practicum.explorewithme.service.dto.*;
import ru.practicum.explorewithme.service.dto.UpdateEventAdminRequest.StateActionAdmin;
import ru.practicum.explorewithme.service.dto.UpdateEventUserRequest.StateActionUser;
import ru.practicum.explorewithme.service.exception.*;
import ru.practicum.explorewithme.service.mapper.EventMapper;
import ru.practicum.explorewithme.service.model.*;
import ru.practicum.explorewithme.service.repository.CategoryRepository;
import ru.practicum.explorewithme.service.repository.EventRepository;
import ru.practicum.explorewithme.service.repository.UserRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final EventMapper eventMapper;
    private final StatsClient statsClient;

    @Override
    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto newEventDto) {
        log.info("Создание события пользователем с ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User", userId));

        Category category = categoryRepository.findById(newEventDto.getCategory())
                .orElseThrow(() -> new NotFoundException("Category", newEventDto.getCategory()));

        LocalDateTime eventDate = newEventDto.getEventDate();
        if (eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
            throw new EventValidationException("Дата события должна быть не раньше чем через 2 часа от текущего момента");
        }

        Event event = eventMapper.toEntity(newEventDto, category, user);
        Event savedEvent = eventRepository.save(event);

        log.info("Событие создано с ID: {}", savedEvent.getId());
        return eventMapper.toFullDto(savedEvent);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getUserEvents(Long userId, Integer from, Integer size) {
        log.info("Получение событий пользователя с ID: {}, from: {}, size: {}", userId, from, size);

        validatePaginationParams(from, size);
        Pageable pageable = PageRequest.of(from / size, size, Sort.by("eventDate").descending());

        return eventRepository.findAllByInitiatorId(userId, pageable)
                .stream()
                .map(eventMapper::toShortDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public EventFullDto getUserEvent(Long userId, Long eventId) {
        log.info("Получение события с ID: {} пользователя с ID: {}", eventId, userId);

        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event", eventId));

        return eventMapper.toFullDto(event);
    }

    @Override
    @Transactional
    public EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest updateRequest) {
        log.info("Обновление события с ID: {} пользователем с ID: {}", eventId, userId);

        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event", eventId));

        if (event.getState() == EventState.PUBLISHED) {
            throw new EventValidationException("Нельзя изменить опубликованное событие");
        }

        updateEventFieldsFromUserRequest(event, updateRequest);

        if (updateRequest.getCategory() != null) {
            Category category = categoryRepository.findById(updateRequest.getCategory())
                    .orElseThrow(() -> new NotFoundException("Category", updateRequest.getCategory()));
            event.setCategory(category);
        }

        if (updateRequest.getStateAction() != null) {
            updateUserEventState(event, updateRequest.getStateAction());
        }

        if (updateRequest.getEventDate() != null &&
                updateRequest.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new EventValidationException("Дата события должна быть не раньше чем через 2 часа от текущего момента");
        }

        Event updatedEvent = eventRepository.save(event);
        log.info("Событие с ID: {} обновлено пользователем", eventId);
        return eventMapper.toFullDto(updatedEvent);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventFullDto> searchEvents(List<Long> users, List<String> states, List<Long> categories,
                                           LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                           Integer from, Integer size) {
        log.info("Поиск событий администратором. Users: {}, states: {}, categories: {}",
                users, states, categories);

        validatePaginationParams(from, size);
        Pageable pageable = PageRequest.of(from / size, size, Sort.by("id").ascending());

        List<EventState> eventStates = null;
        if (states != null && !states.isEmpty()) {
            eventStates = states.stream()
                    .map(String::toUpperCase)
                    .filter(this::isValidEventState)
                    .map(EventState::valueOf)
                    .collect(Collectors.toList());
        }

        return eventRepository.findAllByAdminFilters(users, eventStates, categories,
                        rangeStart, rangeEnd, pageable)
                .stream()
                .map(eventMapper::toFullDto)
                .collect(Collectors.toList());
    }

    private boolean isValidEventState(String state) {
        try {
            EventState.valueOf(state);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    @Transactional
    public EventFullDto updateAdminEvent(Long eventId, UpdateEventAdminRequest updateRequest) {
        log.info("Обновление события с ID: {} администратором", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event", eventId));

        updateEventFieldsFromAdminRequest(event, updateRequest);

        if (updateRequest.getCategory() != null) {
            Category category = categoryRepository.findById(updateRequest.getCategory())
                    .orElseThrow(() -> new NotFoundException("Category", updateRequest.getCategory()));
            event.setCategory(category);
        }

        if (updateRequest.getStateAction() != null) {
            updateAdminEventState(event, updateRequest.getStateAction());
        }

        if (updateRequest.getEventDate() != null) {
            validateEventDateForAdmin(event, updateRequest.getEventDate());
        }

        Event updatedEvent = eventRepository.save(event);
        log.info("Событие с ID: {} обновлено администратором", eventId);
        return eventMapper.toFullDto(updatedEvent);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> searchPublicEvents(String text, List<Long> categories, Boolean paid,
                                                  LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                                  Boolean onlyAvailable, String sort,
                                                  Integer from, Integer size,
                                                  HttpServletRequest request) {
        log.info("Публичный поиск событий. Text: {}, categories: {}, paid: {}",
                text, categories, paid);

        if (from == null) from = 0;
        if (size == null) size = 10;

        if (size <= 0) {
            throw new IllegalArgumentException("Параметр size должен быть больше 0");
        }
        if (from < 0) {
            throw new IllegalArgumentException("Параметр from должен быть неотрицательным");
        }

        Sort sortBy = Sort.unsorted();
        if ("EVENT_DATE".equals(sort)) {
            sortBy = Sort.by("eventDate").ascending();
        } else if ("VIEWS".equals(sort)) {
            sortBy = Sort.by("id").ascending(); // временно, потом добавим views
        }

        Pageable pageable = PageRequest.of(from / size, size, sortBy);

        Page<Event> eventsPage;

        if (text != null && !text.trim().isEmpty()) {
            eventsPage = eventRepository.findPublishedEventsWithText(text.trim(), pageable);
        } else {
            eventsPage = eventRepository.findPublishedEvents(pageable);
        }

        // Фильтрация в памяти (для тестов)
        List<Event> filteredEvents = eventsPage.getContent().stream()
                .filter(event -> {
                    if (categories != null && !categories.isEmpty()) {
                        return categories.contains(event.getCategory().getId());
                    }
                    return true;
                })
                .filter(event -> {
                    if (paid != null) {
                        return event.getPaid().equals(paid);
                    }
                    return true;
                })
                .filter(event -> {
                    if (rangeStart != null) {
                        return !event.getEventDate().isBefore(rangeStart);
                    }
                    return true;
                })
                .filter(event -> {
                    if (rangeEnd != null) {
                        return !event.getEventDate().isAfter(rangeEnd);
                    }
                    return true;
                })
                .filter(event -> {
                    if (onlyAvailable != null && onlyAvailable) {
                        return event.getParticipantLimit() == 0 ||
                                event.getConfirmedRequests() < event.getParticipantLimit();
                    }
                    return true;
                })
                .toList();

        try {
            EndpointHit endpointHit = new EndpointHit();
            endpointHit.setApp("main-service");
            endpointHit.setUri(request.getRequestURI());
            endpointHit.setIp(request.getRemoteAddr());
            endpointHit.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            statsClient.hit(endpointHit);
        } catch (Exception e) {
            log.error("Ошибка отправки статистики: {}", e.getMessage());
        }

        return filteredEvents.stream()
                .map(eventMapper::toShortDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public EventFullDto getPublicEvent(Long eventId, HttpServletRequest request) {
        log.info("Получение публичного события с ID: {}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event", eventId));

        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Event", eventId);
        }

        sendStatsHit(eventId, request);
        Long views = getViewsFromStats(eventId);

        EventFullDto eventDto = eventMapper.toFullDto(event);
        eventDto.setViews(views);

        return eventDto;
    }

    @Override
    @Transactional(readOnly = true)
    public EventFullDto getEventById(Long eventId) {
        log.info("Получение события с ID: {}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event", eventId));

        return eventMapper.toFullDto(event);
    }

    private void validatePaginationParams(Integer from, Integer size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Параметр size должен быть больше 0");
        }
        if (from < 0) {
            throw new IllegalArgumentException("Параметр from должен быть неотрицательным");
        }
    }

    private Sort getSortForPublicEvents(String sort) {
        if ("EVENT_DATE".equals(sort)) {
            return Sort.by("eventDate").ascending();
        } else if ("VIEWS".equals(sort)) {
            return Sort.by("views").descending();
        } else {
            return Sort.unsorted();
        }
    }

    private void updateEventFieldsFromUserRequest(Event event, UpdateEventUserRequest updateRequest) {
        if (updateRequest.getAnnotation() != null) {
            event.setAnnotation(updateRequest.getAnnotation());
        }
        if (updateRequest.getDescription() != null) {
            event.setDescription(updateRequest.getDescription());
        }
        if (updateRequest.getEventDate() != null) {
            event.setEventDate(updateRequest.getEventDate());
        }
        if (updateRequest.getLocation() != null) {
            event.setLocation(updateRequest.getLocation().toEntity());
        }
        if (updateRequest.getPaid() != null) {
            event.setPaid(updateRequest.getPaid());
        }
        if (updateRequest.getParticipantLimit() != null) {
            event.setParticipantLimit(updateRequest.getParticipantLimit());
        }
        if (updateRequest.getRequestModeration() != null) {
            event.setRequestModeration(updateRequest.getRequestModeration());
        }
        if (updateRequest.getTitle() != null) {
            event.setTitle(updateRequest.getTitle());
        }
    }

    private void updateEventFieldsFromAdminRequest(Event event, UpdateEventAdminRequest updateRequest) {
        if (updateRequest.getAnnotation() != null) {
            event.setAnnotation(updateRequest.getAnnotation());
        }
        if (updateRequest.getDescription() != null) {
            event.setDescription(updateRequest.getDescription());
        }
        if (updateRequest.getEventDate() != null) {
            event.setEventDate(updateRequest.getEventDate());
        }
        if (updateRequest.getLocation() != null) {
            event.setLocation(updateRequest.getLocation().toEntity());
        }
        if (updateRequest.getPaid() != null) {
            event.setPaid(updateRequest.getPaid());
        }
        if (updateRequest.getParticipantLimit() != null) {
            event.setParticipantLimit(updateRequest.getParticipantLimit());
        }
        if (updateRequest.getRequestModeration() != null) {
            event.setRequestModeration(updateRequest.getRequestModeration());
        }
        if (updateRequest.getTitle() != null) {
            event.setTitle(updateRequest.getTitle());
        }
    }

    private void updateUserEventState(Event event, StateActionUser stateAction) {
        switch (stateAction) {
            case SEND_TO_REVIEW:
                event.setState(EventState.PENDING);
                break;
            case CANCEL_REVIEW:
                event.setState(EventState.CANCELED);
                break;
        }
    }

    private void updateAdminEventState(Event event, StateActionAdmin stateAction) {
        switch (stateAction) {
            case PUBLISH_EVENT:
                if (event.getState() != EventState.PENDING) {
                    throw new EventValidationException(
                            "Событие можно опубликовать, только если оно в состоянии ожидания публикации");
                }
                event.setState(EventState.PUBLISHED);
                event.setPublishedOn(LocalDateTime.now());
                break;
            case REJECT_EVENT:
                if (event.getState() == EventState.PUBLISHED) {
                    throw new EventValidationException(
                            "Событие можно отклонить, только если оно еще не опубликовано");
                }
                event.setState(EventState.CANCELED);
                break;
        }
    }

    private void validateEventDateForAdmin(Event event, LocalDateTime newEventDate) {
        if (event.getPublishedOn() != null &&
                newEventDate.isBefore(event.getPublishedOn().plusHours(1))) {
            throw new EventValidationException(
                    "Дата начала изменяемого события должна быть не ранее чем за час от даты публикации");
        }
    }

    private void sendStatsHit(Long eventId, HttpServletRequest request) {
        try {
            EndpointHit endpointHit = new EndpointHit();
            endpointHit.setApp("main-service");
            endpointHit.setUri("/events/" + eventId);
            endpointHit.setIp(getClientIp(request));
            endpointHit.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            statsClient.hit(endpointHit);
            log.debug("Статистика отправлена для события: {}", eventId);
        } catch (Exception e) {
            log.error("Ошибка при отправке статистики для события {}: {}", eventId, e.getMessage());
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (remoteAddr != null && !remoteAddr.isEmpty()) {
            return remoteAddr;
        }
        return "unknown";
    }

    private Long getViewsFromStats(Long eventId) {
        try {
            LocalDateTime start = LocalDateTime.now().minusYears(100); // Все время
            LocalDateTime end = LocalDateTime.now().plusYears(100);
            List<String> uris = List.of("/events/" + eventId);

            List<ViewStats> stats = statsClient.getStats(start, end, uris, true);

            if (stats != null && !stats.isEmpty()) {
                return stats.getFirst().getHits();
            }
            return 0L;
        } catch (Exception e) {
            log.error("Ошибка при получении статистики для события {}: {}", eventId, e.getMessage());
            return 0L;
        }
    }

    private void sendStatsHitForSearch(HttpServletRequest request) {
        try {
            EndpointHit endpointHit = new EndpointHit();
            endpointHit.setApp("main-service");
            endpointHit.setUri(request.getRequestURI() + (request.getQueryString() != null ? "?" + request.getQueryString() : ""));
            endpointHit.setIp(getClientIp(request));
            endpointHit.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            statsClient.hit(endpointHit);
            log.debug("Статистика отправлена для поиска: {}", endpointHit.getUri());
        } catch (Exception e) {
            log.error("Ошибка при отправке статистики поиска: {}", e.getMessage());
        }
    }
}