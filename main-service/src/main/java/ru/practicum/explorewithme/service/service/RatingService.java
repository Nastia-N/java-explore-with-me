package ru.practicum.explorewithme.service.service;

import ru.practicum.explorewithme.service.dto.EventRatingStatsDto;
import ru.practicum.explorewithme.service.dto.RatingDto;
import ru.practicum.explorewithme.service.model.RatingRequest;

import java.util.List;
import java.util.Optional;

public interface RatingService {

    RatingDto rateEvent(Long userId, RatingRequest ratingRequest);

    void deleteRating(Long userId, Long ratingId);

    List<RatingDto> getUserRatings(Long userId);

    EventRatingStatsDto getEventRatingStats(Long eventId, Long currentUserId);

    void updateEventRatingScore(Long eventId);

    RatingDto createRating(Long userId, RatingRequest ratingRequest);

    RatingDto updateRating(Long userId, Long ratingId, Integer value);

    Optional<RatingDto> getUserRatingForEvent(Long userId, Long eventId);

    void deleteRatingByEvent(Long userId, Long eventId);

    boolean existsByEventIdAndUserId(Long eventId, Long userId);

}
