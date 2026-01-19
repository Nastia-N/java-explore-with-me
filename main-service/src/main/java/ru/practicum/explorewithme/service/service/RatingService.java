package ru.practicum.explorewithme.service.service;

import ru.practicum.explorewithme.service.dto.EventRatingStatsDto;
import ru.practicum.explorewithme.service.dto.RatingDto;
import ru.practicum.explorewithme.service.model.RatingRequest;

import java.util.List;

public interface RatingService {

    RatingDto rateEvent(Long userId, RatingRequest ratingRequest);

    void deleteRating(Long userId, Long ratingId);

    List<RatingDto> getUserRatings(Long userId);

    EventRatingStatsDto getEventRatingStats(Long eventId, Long currentUserId);

    void updateEventRatingScore(Long eventId);

}
