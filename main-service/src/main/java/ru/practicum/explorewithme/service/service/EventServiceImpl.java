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
import java.util.ArrayList;
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

        // 1. Проверка существования пользователя
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));

        // 2. Проверка существования категории
        Category category = categoryRepository.findById(newEventDto.getCategory())
                .orElseThrow(() -> new NotFoundException("Category with id=" + newEventDto.getCategory() + " was not found"));

        // 3. Валидация даты события
        LocalDateTime eventDate = newEventDto.getEventDate();
        if (eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
            throw new EventValidationException(
                    String.format("Field: eventDate. Error: должно содержать дату, которая еще не наступила. Value: %s",
                            eventDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        }

        // 4. Создание события
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

        return dto;
    }

    @Override
    @Transactional
    public EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest updateRequest) {
        log.info("Обновление события с ID: {} пользователем с ID: {}", eventId, userId);

        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        // Проверка состояния события
        if (event.getState() == EventState.PUBLISHED) {
            throw new BusinessConflictException("Only pending or canceled events can be changed");
        }

        // Проверка даты события
        LocalDateTime newEventDate = updateRequest.getEventDate();
        if (newEventDate != null && newEventDate.isBefore(LocalDateTime.now().plusHours(2))) {
            throw new EventValidationException(
                    String.format("Field: eventDate. Error: должно содержать дату, которая еще не наступила. Value: %s",
                            newEventDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        }

        // Обновление полей события
        updateEventFieldsFromUserRequest(event, updateRequest);

        if (updateRequest.getCategory() != null) {
            Category category = categoryRepository.findById(updateRequest.getCategory())
                    .orElseThrow(() -> new NotFoundException("Category with id=" + updateRequest.getCategory() + " was not found"));
            event.setCategory(category);
        }

        if (updateRequest.getStateAction() != null) {
            updateUserEventState(event, updateRequest.getStateAction());
        }

        Event updatedEvent = eventRepository.save(event);
        log.info("Событие с ID: {} обновлено пользователем", eventId);

        EventFullDto dto = eventMapper.toFullDto(updatedEvent);
        dto.setViews(getViewsFromStats(eventId));

        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventFullDto> searchEvents(List<Long> users, List<String> states, List<Long> categories,
                                           LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                           Integer from, Integer size) {
        log.info("Поиск событий администратором. Users: {}, states: {}, categories: {}",
                users, states, categories);

        validatePaginationParams(from, size);
        Pageable pageable = createPageable(from, size, Sort.by("id").ascending());

        List<EventState> eventStates = null;
        if (states != null && !states.isEmpty()) {
            eventStates = new ArrayList<>();
            for (String state : states) {
                try {
                    eventStates.add(EventState.valueOf(state.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Unknown state: " + state);
                }
            }
        }

        Page<Event> eventsPage = eventRepository.findAllByAdminFilters(
                users, eventStates, categories, rangeStart, rangeEnd, pageable);

        return eventsPage.stream()
                .map(event -> {
                    EventFullDto dto = eventMapper.toFullDto(event);
                    dto.setViews(getViewsFromStats(event.getId()));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto updateAdminEvent(Long eventId, UpdateEventAdminRequest updateRequest) {
        log.info("Обновление события с ID: {} администратором", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        // Проверка даты для публикации события
        if (updateRequest.getEventDate() != null && event.getPublishedOn() != null) {
            LocalDateTime minAllowedDate = event.getPublishedOn().plusHours(1);
            if (updateRequest.getEventDate().isBefore(minAllowedDate)) {
                throw new BusinessConflictException(
                        "Дата начала изменяемого события должна быть не ранее чем за час от даты публикации");
            }
        }

        // Проверка даты события перед публикацией
        if (updateRequest.getStateAction() == StateActionAdmin.PUBLISH_EVENT) {
            LocalDateTime eventDate = updateRequest.getEventDate() != null
                    ? updateRequest.getEventDate()
                    : event.getEventDate();
            if (eventDate.isBefore(LocalDateTime.now().plusHours(1))) {
                throw new BusinessConflictException(
                        "Дата начала события должна быть не ранее чем через час от текущего момента");
            }
        }

        // Обновление полей события
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

        // Используем значения по умолчанию
        from = (from == null) ? 0 : from;
        size = (size == null) ? 10 : size;

        validatePaginationParams(from, size);

        // Согласно спецификации: если не указан диапазон дат, показываем события после текущей даты
        LocalDateTime finalRangeStart = rangeStart;
        LocalDateTime finalRangeEnd = rangeEnd;

        if (rangeStart == null && rangeEnd == null) {
            finalRangeStart = LocalDateTime.now();
        }

        // Проверяем, что rangeStart не позже rangeEnd
        if (finalRangeStart != null && finalRangeEnd != null && finalRangeStart.isAfter(finalRangeEnd)) {
            throw new IllegalArgumentException("rangeStart cannot be after rangeEnd");
        }

        // Определяем сортировку
        Sort sortBy = getSortForPublicEvents(sort);

        // Валидация: size не может быть 0
        if (size <= 0) {
            throw new IllegalArgumentException("Parameter 'size' must be positive");
        }

        int pageNumber = from / size;
        Pageable pageable = PageRequest.of(pageNumber, size, sortBy);

        Boolean onlyAvailableFlag = (onlyAvailable != null) ? onlyAvailable : false;

        Page<Event> eventsPage = eventRepository.findPublicEvents(
                text,
                categories,
                paid,
                finalRangeStart,
                finalRangeEnd,
                onlyAvailableFlag,
                pageable);

        List<Event> filteredEvents = eventsPage.getContent();

        // Дополнительная фильтрация: если rangeStart = текущее время, показываем только будущие события
        if (finalRangeStart != null && finalRangeStart.isEqual(LocalDateTime.now())) {
            filteredEvents = filteredEvents.stream()
                    .filter(event -> event.getEventDate().isAfter(LocalDateTime.now()))
                    .collect(Collectors.toList());
        }

        // Для сортировки по просмотрам - отдельная обработка
        if ("VIEWS".equals(sort)) {
            return getEventsSortedByViews(filteredEvents, from, size);
        }

        // Отправляем статистику о поиске
        sendStatsHitForSearch(request);

        return filteredEvents.stream()
                .map(event -> {
                    EventShortDto dto = eventMapper.toShortDto(event);
                    dto.setViews(getViewsFromStats(event.getId()));
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

        // Проверка, что событие опубликовано
        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Event with id=" + eventId + " was not found");
        }

        // Отправляем статистику о просмотре
        sendStatsHit(eventId, request);

        // Получаем количество просмотров
        Long views = getViewsFromStats(eventId);

        EventFullDto dto = eventMapper.toFullDto(event);
        dto.setViews(views);

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

        return dto;
    }

    // ============ ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ============

    private Pageable createPageable(Integer from, Integer size, Sort sort) {
        if (from == null) from = 0;
        if (size == null) size = 10;

        // Валидация
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

            List<ViewStats> stats = statsClient.getStats(start, end, uris, true);

            return stats != null && !stats.isEmpty() ? stats.get(0).getHits() : 0L;
        } catch (Exception e) {
            log.error("Ошибка при получении статистики для события {}: {}", eventId, e.getMessage());
            return 0L;
        }
    }

    private List<EventShortDto> getEventsSortedByViews(List<Event> events, Integer from, Integer size) {
        List<EventShortDto> eventsWithViews = events.stream()
                .map(event -> {
                    EventShortDto dto = eventMapper.toShortDto(event);
                    dto.setViews(getViewsFromStats(event.getId()));
                    return dto;
                })
                .sorted((e1, e2) -> Long.compare(e2.getViews(), e1.getViews()))
                .collect(Collectors.toList());

        int start = Math.min(from, eventsWithViews.size());
        int end = Math.min(start + size, eventsWithViews.size());

        return eventsWithViews.subList(start, end);
    }

    private Sort getSortForPublicEvents(String sort) {
        if ("EVENT_DATE".equals(sort)) {
            return Sort.by("eventDate").ascending();
        } else if ("VIEWS".equals(sort)) {
            return Sort.unsorted(); // Сортировка будет выполнена отдельно
        }
        return Sort.unsorted();
    }

    // ============ МЕТОДЫ ДЛЯ ОБНОВЛЕНИЯ ПОЛЕЙ СОБЫТИЙ ============

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

    // ============ МЕТОДЫ ДЛЯ ОБНОВЛЕНИЯ СТАТУСОВ ============

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

    // ============ МЕТОДЫ ДЛЯ СТАТИСТИКИ ============

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
        return (remoteAddr != null && !remoteAddr.isEmpty()) ? remoteAddr : "unknown";
    }

    private void sendStatsHitForSearch(HttpServletRequest request) {
        try {
            EndpointHit endpointHit = new EndpointHit();
            endpointHit.setApp("main-service");
            endpointHit.setUri(request.getRequestURI());
            endpointHit.setIp(getClientIp(request));
            endpointHit.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            statsClient.hit(endpointHit);
            log.debug("Статистика отправлена для поиска: {}", endpointHit.getUri());
        } catch (Exception e) {
            log.error("Ошибка при отправке статистики поиска: {}", e.getMessage());
        }
    }
}