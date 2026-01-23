package ru.practicum.explorewithme.service.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.explorewithme.service.dto.EventRatingStatsDto;
import ru.practicum.explorewithme.service.service.RatingService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/events/{eventId}/rating")
@Slf4j
public class EventRatingController {

    private final RatingService ratingService;

    @GetMapping
    public EventRatingStatsDto getEventRating(@PathVariable Long eventId,
                                              @RequestParam(required = false) Long userId) {
        log.info("GET /events/{}/rating - получение рейтинга события", eventId);
        return ratingService.getEventRatingStats(eventId, userId);
    }
}
