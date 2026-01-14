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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
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
    private final ConcurrentHashMap<Long, Long> viewsCache = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto newEventDto) {
        log.info("Создание события пользователем с ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));

        Category category = categoryRepository.findById(newEventDto.getCategory())
                .orElseThrow(() -> new NotFoundException("Категория с id=" + newEventDto.getCategory() + " не найдена"));

        LocalDateTime eventDate = newEventDto.getEventDate();
        if (eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
            throw new EventValidationException(
                    String.format("Field: eventDate. Error: должно содержать дату, которая еще не наступила. Value: %s",
                            eventDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
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
        Pageable pageable = createPageable(from, size, Sort.by("eventDate").descending());

        Page<Event> eventsPage = eventRepository.findAllByInitiatorId(userId, pageable);

        return eventsPage.stream()
                .map(event -> {
                    EventShortDto dto = eventMapper.toShortDto(event);
                    dto.setViews(getViewsFromStats(event.getId()));
                    if (dto.getConfirmedRequests() == null) {
                        dto.setConfirmedRequests(event.getConfirmedRequests() != null ?
                                event.getConfirmedRequests() : 0);
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public EventFullDto getUserEvent(Long userId, Long eventId) {
        log.info("Получение события с ID: {} пользователя с ID: {}", eventId, userId);

        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        EventFullDto dto = eventMapper.toFullDto(event);
        dto.setViews(getViewsFromStats(eventId));
        if (dto.getConfirmedRequests() == null) {
            dto.setConfirmedRequests(event.getConfirmedRequests() != null ?
                    event.getConfirmedRequests() : 0);
        }

        return dto;
    }

    @Override
    @Transactional
    public EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest updateRequest) {
        log.info("Обновление события с ID: {} пользователем с ID: {}", eventId, userId);

        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Только события в состоянии ожидания или отмены можно изменить");
        }

        LocalDateTime newEventDate = updateRequest.getEventDate();
        if (newEventDate != null && newEventDate.isBefore(LocalDateTime.now().plusHours(2))) {
            throw new EventValidationException(
                    String.format("Field: eventDate. Error: должно содержать дату, которая еще не наступила. Value: %s",
                            newEventDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        }

        updateEventFieldsFromUserRequest(event, updateRequest);

        if (updateRequest.getCategory() != null) {
            Category category = categoryRepository.findById(updateRequest.getCategory())
                    .orElseThrow(() -> new NotFoundException("Категория с id=" + updateRequest.getCategory() + " не найдена"));
            event.setCategory(category);
        }

        if (updateRequest.getStateAction() != null) {
            updateUserEventState(event, updateRequest.getStateAction());
        }

        Event updatedEvent = eventRepository.save(event);
        log.info("Событие с ID: {} обновлено пользователем", eventId);

        EventFullDto dto = eventMapper.toFullDto(updatedEvent);
        dto.setViews(getViewsFromStats(eventId));
        if (dto.getConfirmedRequests() == null) {
            dto.setConfirmedRequests(event.getConfirmedRequests() != null ?
                    event.getConfirmedRequests() : 0);
        }

        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventFullDto> searchEvents(List<Long> users, List<String> states, List<Long> categories,
                                           LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                           Integer from, Integer size) {
        log.info("Поиск событий администратором. Users: {}, states: {}, categories: {}",
                users, states, categories);

        from = (from == null) ? 0 : from;
        size = (size == null) ? 10 : size;

        if (size <= 0) {
            throw new IllegalArgumentException("size must be positive");
        }
        if (from < 0) {
            throw new IllegalArgumentException("from must be non-negative");
        }

        List<EventState> eventStates = null;
        if (states != null && !states.isEmpty()) {
            eventStates = states.stream()
                    .map(EventState::valueOf)
                    .collect(Collectors.toList());
        }

        List<Event> events = eventRepository.findAllByAdminFilters(
                users != null && !users.isEmpty() ? users : null,
                eventStates,
                categories != null && !categories.isEmpty() ? categories : null,
                rangeStart,
                rangeEnd,
                from,
                size
        );

        return events.stream()
                .map(event -> {
                    EventFullDto dto = eventMapper.toFullDto(event);
                    dto.setViews(getViewsFromStats(event.getId()));
                    if (dto.getConfirmedRequests() == null) {
                        dto.setConfirmedRequests(event.getConfirmedRequests() != null ?
                                event.getConfirmedRequests() : 0);
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private List<Event> sortEventsByViews(List<Event> events) {
        return events.stream()
                .sorted((e1, e2) -> {
                    Long views1 = getViewsFromStats(e1.getId());
                    Long views2 = getViewsFromStats(e2.getId());
                    return Long.compare(views2, views1);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto updateAdminEvent(Long eventId, UpdateEventAdminRequest updateRequest) {
        log.info("Обновление события с ID: {} администратором", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        if (updateRequest.getEventDate() != null) {
            LocalDateTime newEventDate = updateRequest.getEventDate();

            if (newEventDate.isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("Дата события не может быть в прошлом");
            }

            if (event.getPublishedOn() != null) {
                LocalDateTime minAllowedDate = event.getPublishedOn().plusHours(1);
                if (newEventDate.isBefore(minAllowedDate)) {
                    throw new ConflictException("Дата начала изменяемого события должна быть не ранее чем за час от даты публикации");
                }
            }
        }

        if (updateRequest.getEventDate() != null && event.getPublishedOn() != null) {
            LocalDateTime minAllowedDate = event.getPublishedOn().plusHours(1);
            if (updateRequest.getEventDate().isBefore(minAllowedDate)) {
                throw new ConflictException("Дата начала изменяемого события должна быть не ранее чем за час от даты публикации");
            }
        }

        if (updateRequest.getStateAction() == StateActionAdmin.PUBLISH_EVENT) {
            if (event.getState() != EventState.PENDING) {
                throw new ConflictException("Событие можно публиковать только в состоянии ожидания публикации");
            }

            LocalDateTime eventDate = updateRequest.getEventDate() != null
                    ? updateRequest.getEventDate()
                    : event.getEventDate();
            if (eventDate.isBefore(LocalDateTime.now().plusHours(1))) {
                throw new ConflictException("Дата начала события должна быть не ранее чем через час от текущего момента");
            }
        }

        if (updateRequest.getStateAction() == StateActionAdmin.REJECT_EVENT) {
            if (event.getState() == EventState.PUBLISHED) {
                throw new ConflictException("Событие нельзя отклонить, если оно уже опубликовано");
            }
        }

        updateEventFieldsFromAdminRequest(event, updateRequest);

        if (updateRequest.getCategory() != null) {
            Category category = categoryRepository.findById(updateRequest.getCategory())
                    .orElseThrow(() -> new NotFoundException("Category with id=" + updateRequest.getCategory() + " was not found"));
            event.setCategory(category);
        }

        if (updateRequest.getStateAction() != null) {
            updateAdminEventState(event, updateRequest.getStateAction());
        }

        Event updatedEvent = eventRepository.save(event);
        log.info("Событие с ID: {} обновлено администратором", eventId);

        EventFullDto dto = eventMapper.toFullDto(updatedEvent);
        dto.setViews(getViewsFromStats(eventId));
        if (dto.getConfirmedRequests() == null) {
            dto.setConfirmedRequests(event.getConfirmedRequests() != null ?
                    event.getConfirmedRequests() : 0);
        }

        return dto;
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

        from = (from == null) ? 0 : from;
        size = (size == null) ? 10 : size;

        if (size <= 0) {
            throw new IllegalArgumentException("size must be positive");
        }
        if (from < 0) {
            throw new IllegalArgumentException("from must be non-negative");
        }

        LocalDateTime finalRangeStart = rangeStart;
        LocalDateTime finalRangeEnd = rangeEnd;

        if (rangeStart == null && rangeEnd == null) {
            finalRangeStart = LocalDateTime.now();
        }

        if (finalRangeStart != null && finalRangeEnd != null && finalRangeStart.isAfter(finalRangeEnd)) {
            throw new IllegalArgumentException("rangeStart cannot be after rangeEnd");
        }

        Boolean onlyAvailableFlag = (onlyAvailable != null) ? onlyAvailable : false;

        List<Event> events = eventRepository.findPublicEvents(
                text,
                categories,
                paid,
                finalRangeStart,
                finalRangeEnd,
                onlyAvailableFlag,
                from,
                size
        );

        if (finalRangeStart != null && finalRangeStart.isEqual(LocalDateTime.now())) {
            events = events.stream()
                    .filter(event -> event.getEventDate().isAfter(LocalDateTime.now()))
                    .collect(Collectors.toList());
        }

        List<Event> sortedEvents = events;
        if ("VIEWS".equals(sort)) {
            sortedEvents = sortEventsByViews(events);
            if (from < sortedEvents.size()) {
                int toIndex = Math.min(from + size, sortedEvents.size());
                sortedEvents = sortedEvents.subList(from, toIndex);
            } else {
                sortedEvents = Collections.emptyList();
            }
        }

        try {
            sendStatsHitForSearch(request);
        } catch (Exception e) {
            log.warn("Не удалось отправить статистику поиска: {}", e.getMessage());
        }

        return sortedEvents.stream()
                .map(event -> {
                    EventShortDto dto = eventMapper.toShortDto(event);
                    dto.setViews(getViewsFromStats(event.getId()));
                    if (dto.getConfirmedRequests() == null) {
                        dto.setConfirmedRequests(event.getConfirmedRequests() != null ?
                                event.getConfirmedRequests() : 0);
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public EventFullDto getPublicEvent(Long eventId, HttpServletRequest request) {
        log.info("Публичный запрос события с ID: {}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Event with id=" + eventId + " was not found");
        }

        sendStatsHit(eventId, request);
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Long views = getViewsFromStats(eventId);

        log.info("Событие {}: текущее количество просмотров: {}", eventId, views);

        EventFullDto dto = eventMapper.toFullDto(event);
        dto.setViews(views != null ? views : 0L);
        if (dto.getConfirmedRequests() == null) {
            dto.setConfirmedRequests(event.getConfirmedRequests() != null ?
                    event.getConfirmedRequests() : 0);
        }

        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public EventFullDto getEventById(Long eventId) {
        log.info("Получение события с ID: {}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        EventFullDto dto = eventMapper.toFullDto(event);
        dto.setViews(getViewsFromStats(eventId));
        if (dto.getConfirmedRequests() == null) {
            dto.setConfirmedRequests(event.getConfirmedRequests() != null ?
                    event.getConfirmedRequests() : 0);
        }

        return dto;
    }

    private Pageable createPageable(Integer from, Integer size, Sort sort) {
        if (from == null) from = 0;
        if (size == null) size = 10;

        if (size <= 0) {
            throw new IllegalArgumentException("Parameter 'size' must be positive");
        }

        int page = from / size;
        return PageRequest.of(page, size, sort);
    }

    private void validatePaginationParams(Integer from, Integer size) {
        if (from != null && from < 0) {
            throw new IllegalArgumentException("Parameter 'from' must be non-negative");
        }
        if (size != null && size <= 0) {
            throw new IllegalArgumentException("Parameter 'size' must be positive");
        }
    }

    private Long getViewsFromStats(Long eventId) {
        try {
            LocalDateTime start = LocalDateTime.now().minusYears(100);
            LocalDateTime end = LocalDateTime.now().plusYears(100);
            List<String> uris = List.of("/events/" + eventId);

            String startStr = start.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String endStr = end.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            log.debug("Запрос статистики для события {}: uris={}, start={}, end={}, unique=true",
                    eventId, uris, startStr, endStr);

            List<ViewStats> stats = statsClient.getStats(startStr, endStr, uris, true);

            if (stats != null && !stats.isEmpty()) {
                Long hits = stats.getFirst().getHits();
                log.debug("Событие {} имеет {} просмотров", eventId, hits);

                viewsCache.put(eventId, hits);
                return hits;
            } else {
                log.debug("Статистика для события {} не найдена", eventId);
                return viewsCache.getOrDefault(eventId, 0L);
            }
        } catch (Exception e) {
            log.error("Ошибка при получении статистики для события {}: {}", eventId, e.getMessage());
            return viewsCache.getOrDefault(eventId, 0L);
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
                    throw new BusinessConflictException(
                            "Cannot publish the event because it's not in the right state: " + event.getState());
                }
                event.setState(EventState.PUBLISHED);
                event.setPublishedOn(LocalDateTime.now());
                break;
            case REJECT_EVENT:
                if (event.getState() == EventState.PUBLISHED) {
                    throw new BusinessConflictException(
                            "Cannot reject the event because it's already published");
                }
                event.setState(EventState.CANCELED);
                break;
        }
    }

    private void sendStatsHit(Long eventId, HttpServletRequest request) {
        try {
            String ipAddress = getClientIp(request);

            EndpointHit endpointHit = new EndpointHit();
            endpointHit.setApp("ewm-main-service");
            endpointHit.setUri("/events/" + eventId);
            endpointHit.setIp(ipAddress);
            endpointHit.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            log.debug("Отправка статистики: app={}, uri={}, ip={}",
                    endpointHit.getApp(), endpointHit.getUri(), endpointHit.getIp());

            statsClient.hit(endpointHit);
            log.debug("Статистика отправлена для события: {}", eventId);
            viewsCache.compute(eventId, (key, value) -> value == null ? 1L : value + 1);

        } catch (Exception e) {
            log.error("Ошибка при отправке статистики для события {}: {}", eventId, e.getMessage());
        }
    }

    private void sendStatsHitForSearch(HttpServletRequest request) {
        try {
            String ipAddress = getClientIp(request);

            EndpointHit endpointHit = new EndpointHit();
            endpointHit.setApp("ewm-main-service");
            endpointHit.setUri(request.getRequestURI() + (request.getQueryString() != null ? "?" + request.getQueryString() : ""));
            endpointHit.setIp(ipAddress);
            endpointHit.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            log.debug("Отправка статистики поиска: app={}, uri={}, ip={}",
                    endpointHit.getApp(), endpointHit.getUri(), endpointHit.getIp());

            statsClient.hit(endpointHit);
            log.debug("Статистика отправлена для поиска: {}", endpointHit.getUri());

        } catch (Exception e) {
            log.error("Ошибка при отправке статистики поиска: {}", e.getMessage());
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");

        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        if (ipAddress != null && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }

        return ipAddress != null ? ipAddress : "127.0.0.1";
    }
}