package ru.practicum.explorewithme.service.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.explorewithme.service.dto.RatingDto;
import ru.practicum.explorewithme.service.model.Event;
import ru.practicum.explorewithme.service.model.Rating;
import ru.practicum.explorewithme.service.model.RatingRequest;
import ru.practicum.explorewithme.service.model.User;

@Component
public class RatingMapper {

    public RatingDto toDto(Rating rating) {
        return RatingDto.builder()
                .id(rating.getId())
                .eventId(rating.getEvent().getId())
                .userId(rating.getUser().getId())
                .value(rating.getValue())
                .createdAt(rating.getCreatedAt())
                .build();
    }

    public Rating toEntity(RatingRequest ratingRequest, Event event, User user) {
        return Rating.builder()
                .event(event)
                .user(user)
                .value(ratingRequest.getValue())
                .build();
    }
}
