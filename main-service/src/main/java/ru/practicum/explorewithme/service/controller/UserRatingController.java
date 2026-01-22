package ru.practicum.explorewithme.service.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.explorewithme.service.dto.RatingDto;
import ru.practicum.explorewithme.service.exception.NotFoundException;
import ru.practicum.explorewithme.service.model.RatingRequest;
import ru.practicum.explorewithme.service.service.RatingService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users/{userId}")
@Slf4j
public class UserRatingController {

    private final RatingService ratingService;

    @PostMapping("/events/{eventId}/rating")
    @ResponseStatus(HttpStatus.CREATED)
    public RatingDto createRating(@PathVariable Long userId,
                                  @PathVariable Long eventId,
                                  @RequestParam Integer value) {
        log.info("POST /users/{}/events/{}/rating - создание оценки", userId, eventId);
        validateRatingValue(value);
        RatingRequest ratingRequest = RatingRequest.builder()
                .eventId(eventId)
                .value(value)
                .build();
        return ratingService.createRating(userId, ratingRequest);
    }

    @PatchMapping("/ratings/{ratingId}")
    public RatingDto updateRating(@PathVariable Long userId,
                                  @PathVariable Long ratingId,
                                  @RequestParam Integer value) {
        log.info("PATCH /users/{}/ratings/{} - обновление оценки", userId, ratingId);
        validateRatingValue(value);
        return ratingService.updateRating(userId, ratingId, value);
    }

    @GetMapping("/events/{eventId}/rating")
    public RatingDto getUserRatingForEvent(@PathVariable Long userId,
                                           @PathVariable Long eventId) {
        log.info("GET /users/{}/events/{}/rating - получение оценки", userId, eventId);
        return ratingService.getUserRatingForEvent(userId, eventId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Оценка пользователя %d для события %d не найдена", userId, eventId)
                ));
    }

    @DeleteMapping("/events/{eventId}/rating")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRatingByEvent(@PathVariable Long userId,
                                    @PathVariable Long eventId) {
        log.info("DELETE /users/{}/events/{}/rating - удаление оценки", userId, eventId);
        ratingService.deleteRatingByEvent(userId, eventId);
    }

    @DeleteMapping("/ratings/{ratingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRating(@PathVariable Long userId,
                             @PathVariable Long ratingId) {
        log.info("DELETE /users/{}/ratings/{} - удаление оценки по ID", userId, ratingId);
        ratingService.deleteRating(userId, ratingId);
    }

    @GetMapping("/ratings")
    public List<RatingDto> getUserRatings(@PathVariable Long userId) {
        log.info("GET /users/{}/ratings - получение всех оценок пользователя", userId);
        return ratingService.getUserRatings(userId);
    }

    private void validateRatingValue(Integer value) {
        if (value == null) {
            throw new IllegalArgumentException("Параметр value обязателен");
        }
        if (value != 1 && value != -1) {
            throw new IllegalArgumentException("Оценка должна быть 1 (лайк) или -1 (дизлайк)");
        }
    }
}