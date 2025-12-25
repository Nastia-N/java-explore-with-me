package ru.practicum.explorewithme.server;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.explorewithme.dto.EndpointHit;
import ru.practicum.explorewithme.dto.ViewStats;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatsService {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final StatsRepository statsRepository;

    @Transactional
    public void saveHit(EndpointHit endpointHit) {
        validateEndpointHit(endpointHit);

        EndpointHitEntity entity = EndpointHitEntity.builder()
                .app(endpointHit.getApp().trim())
                .uri(endpointHit.getUri())
                .ip(endpointHit.getIp())
                .timestamp(parseDateTime(endpointHit.getTimestamp()))
                .build();

        statsRepository.save(entity);
        log.debug("Запрос сохранён: app={}, uri={}", entity.getApp(), entity.getUri());
    }

    public List<ViewStats> getStats(String start, String end,
                                    List<String> uris, Boolean unique) {

        String decodedStart = decodeDateTime(start);
        String decodedEnd = decodeDateTime(end);

        LocalDateTime startTime = parseDateTime(decodedStart);
        LocalDateTime endTime = parseDateTime(decodedEnd);

        validateTimeRange(startTime, endTime);

        List<Object[]> rawResults;
        if (Boolean.TRUE.equals(unique)) {
            rawResults = getUniqueStats(startTime, endTime, uris);
        } else {
            rawResults = getAllStats(startTime, endTime, uris);
        }

        return mapToViewStats(rawResults);
    }

    private List<Object[]> getUniqueStats(LocalDateTime start, LocalDateTime end, List<String> uris) {
        if (uris == null || uris.isEmpty()) {
            return statsRepository.findUniqueStatsAll(start, end);
        } else {
            return statsRepository.findUniqueStatsByUris(start, end, uris);
        }
    }

    private List<Object[]> getAllStats(LocalDateTime start, LocalDateTime end, List<String> uris) {
        if (uris == null || uris.isEmpty()) {
            return statsRepository.findStatsAll(start, end);
        } else {
            return statsRepository.findStatsByUris(start, end, uris);
        }
    }

    private void validateEndpointHit(EndpointHit endpointHit) {
        if (endpointHit.getApp() == null || endpointHit.getApp().trim().isEmpty()) {
            throw new IllegalArgumentException("Название приложения не может быть пустым");
        }
        if (endpointHit.getUri() == null || endpointHit.getUri().isEmpty()) {
            throw new IllegalArgumentException("URI не может быть пустым");
        }
        if (endpointHit.getIp() == null || endpointHit.getIp().isEmpty()) {
            throw new IllegalArgumentException("IP не может быть пустым");
        }
    }

    private void validateTimeRange(LocalDateTime start, LocalDateTime end) {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Время начала должно быть раньше времени окончания");
        }
    }

    private String decodeDateTime(String dateTime) {
        try {
            return URLDecoder.decode(dateTime, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return dateTime;
        }
    }

    private LocalDateTime parseDateTime(String dateTime) {
        try {
            return LocalDateTime.parse(dateTime, FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Некорректный формат даты. Ожидается: yyyy-MM-dd HH:mm:ss. Получено: " + dateTime, e);
        }
    }

    private List<ViewStats> mapToViewStats(List<Object[]> rawResults) {
        List<ViewStats> viewStatsList = new ArrayList<>();
        for (Object[] row : rawResults) {
            viewStatsList.add(new ViewStats(
                    (String) row[0],
                    (String) row[1],
                    (Long) row[2]
            ));
        }
        return viewStatsList;
    }
}