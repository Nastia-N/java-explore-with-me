package ru.practicum.explorewithme.service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.explorewithme.service.dto.RatingDto;
import ru.practicum.explorewithme.service.model.RatingRequest;
import ru.practicum.explorewithme.service.service.RatingService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users/{userId}/ratings")
@Slf4j
public class UserRatingController {

    private final RatingService ratingService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RatingDto rateEvent(@PathVariable Long userId,
                               @RequestBody @Valid RatingRequest ratingRequest) {
        log.info("POST /users/{}/ratings - оценка события {}", userId, ratingRequest.getEventId());
        return ratingService.rateEvent(userId, ratingRequest);
    }

    @DeleteMapping("/{ratingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRating(@PathVariable Long userId,
                             @PathVariable Long ratingId) {
        log.info("DELETE /users/{}/ratings/{} - удаление оценки", userId, ratingId);
        ratingService.deleteRating(userId, ratingId);
    }

    @GetMapping
    public List<RatingDto> getUserRatings(@PathVariable Long userId) {
        log.info("GET /users/{}/ratings - получение оценок пользователя", userId);
        return ratingService.getUserRatings(userId);
    }
}
