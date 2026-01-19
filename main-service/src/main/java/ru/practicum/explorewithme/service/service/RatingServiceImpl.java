package ru.practicum.explorewithme.service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.explorewithme.service.dto.EventRatingStatsDto;
import ru.practicum.explorewithme.service.dto.RatingDto;
import ru.practicum.explorewithme.service.exception.ConflictException;
import ru.practicum.explorewithme.service.exception.NotFoundException;
import ru.practicum.explorewithme.service.mapper.RatingMapper;
import ru.practicum.explorewithme.service.model.*;
import ru.practicum.explorewithme.service.repository.EventRepository;
import ru.practicum.explorewithme.service.repository.ParticipationRequestRepository;
import ru.practicum.explorewithme.service.repository.RatingRepository;
import ru.practicum.explorewithme.service.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RatingServiceImpl implements RatingService {

    private final RatingRepository ratingRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final ParticipationRequestRepository participationRequestRepository;
    private final RatingMapper ratingMapper;

    @Override
    @Transactional
    public RatingDto rateEvent(Long userId, RatingRequest ratingRequest) {
        log.info("Пользователь {} оценивает событие {}", userId, ratingRequest.getEventId());

        validateRatingRequest(ratingRequest);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));

        Event event = eventRepository.findById(ratingRequest.getEventId())
                .orElseThrow(() -> new NotFoundException("Событие с id=" + ratingRequest.getEventId() + " не найдено"));

        validateEventCompletion(event);
        validateUserParticipation(event.getId(), userId);

        Optional<Rating> existingRating = ratingRepository
                .findByEventIdAndUserId(event.getId(), userId);

        if (existingRating.isPresent()) {
            return updateExistingRating(existingRating.get(), ratingRequest.getValue(), event.getId());
        }

        return createNewRating(user, event, ratingRequest.getValue());
    }

    @Override
    @Transactional
    public void deleteRating(Long userId, Long ratingId) {
        log.info("Пользователь {} удаляет оценку {}", userId, ratingId);

        Rating rating = ratingRepository.findById(ratingId)
                .orElseThrow(() -> new NotFoundException("Оценка с id=" + ratingId + " не найдена"));

        if (!rating.getUser().getId().equals(userId)) {
            throw new ConflictException("Пользователь может удалять только свои оценки");
        }

        Long eventId = rating.getEvent().getId();
        Integer ratingValue = rating.getValue();

        ratingRepository.delete(rating);
        updateEventRatingScore(eventId);

        log.info("Оценка {} удалена. Событие {} потеряло {} баллов", ratingId, eventId, ratingValue);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RatingDto> getUserRatings(Long userId) {
        log.info("Получение оценок пользователя {}", userId);

        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }

        return ratingRepository.findByUserId(userId)
                .stream()
                .map(ratingMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public EventRatingStatsDto getEventRatingStats(Long eventId, Long currentUserId) {
        log.info("Получение статистики рейтинга для события {}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        long likes = ratingRepository.countByEventIdAndValue(eventId, 1);
        long dislikes = ratingRepository.countByEventIdAndValue(eventId, -1);
        long totalRating = ratingRepository.sumByEventId(eventId);

        Integer userVote = 0;
        if (currentUserId != null) {
            Optional<Rating> userRating = ratingRepository.findByEventIdAndUserId(eventId, currentUserId);
            userVote = userRating.map(Rating::getValue).orElse(0);
        }

        return EventRatingStatsDto.builder()
                .eventId(eventId)
                .eventTitle(event.getTitle())
                .likes(likes)
                .dislikes(dislikes)
                .totalRating(totalRating)
                .userVote(userVote)
                .build();
    }

    @Override
    @Transactional
    public void updateEventRatingScore(Long eventId) {
        long totalRating = ratingRepository.sumByEventId(eventId);

        eventRepository.findById(eventId).ifPresent(event -> {
            event.setRatingScore(totalRating);
            eventRepository.save(event);
            log.debug("Обновлен рейтинг события {}: {}", eventId, totalRating);
        });
    }

    private void validateRatingRequest(RatingRequest ratingRequest) {
        if (ratingRequest.getValue() == null) {
            throw new IllegalArgumentException("Оценка не может быть пустой");
        }
        if (ratingRequest.getValue() != 1 && ratingRequest.getValue() != -1) {
            throw new IllegalArgumentException("Оценка должна быть 1 (лайк) или -1 (дизлайк)");
        }
    }

    private void validateEventCompletion(Event event) {
        if (event.getEventDate().isAfter(LocalDateTime.now())) {
            throw new ConflictException("Нельзя оценить событие, которое еще не завершилось");
        }
    }

    private void validateUserParticipation(Long eventId, Long userId) {
        List<ParticipationRequest> allUserRequests = participationRequestRepository
                .findAllByRequesterId(userId);

        boolean hasParticipated = allUserRequests.stream()
                .filter(request -> request.getEvent().getId().equals(eventId))
                .anyMatch(request -> request.getStatus() == RequestStatus.CONFIRMED);

        if (!hasParticipated) {
            throw new ConflictException("Могут оценивать только участники события");
        }
    }

    private RatingDto updateExistingRating(Rating existingRating, Integer newValue, Long eventId) {
        Integer oldValue = existingRating.getValue();

        if (oldValue.equals(newValue)) {
            log.debug("Оценка не изменилась: пользователь {}, событие {}, оценка {}",
                    existingRating.getUser().getId(), eventId, oldValue);
            return ratingMapper.toDto(existingRating);
        }

        existingRating.setValue(newValue);
        Rating updatedRating = ratingRepository.save(existingRating);

        updateEventRatingScore(eventId);

        log.info("Оценка обновлена: пользователь {}, событие {}, оценка {} -> {}",
                existingRating.getUser().getId(), eventId, oldValue, newValue);

        return ratingMapper.toDto(updatedRating);
    }

    private RatingDto createNewRating(User user, Event event, Integer value) {
        Rating rating = Rating.builder()
                .event(event)
                .user(user)
                .value(value)
                .build();

        Rating savedRating = ratingRepository.save(rating);
        updateEventRatingScore(event.getId());

        log.info("Оценка создана: пользователь {}, событие {}, оценка {}",
                user.getId(), event.getId(), value);

        return ratingMapper.toDto(savedRating);
    }

    @Transactional(readOnly = true)
    public boolean canUserRateEvent(Long userId, Long eventId) {
        if (!participationRequestRepository.existsByEventIdAndRequesterId(eventId, userId)) {
            return false;
        }

        List<ParticipationRequest> userRequests = participationRequestRepository
                .findAllByRequesterId(userId);

        return userRequests.stream()
                .filter(request -> request.getEvent().getId().equals(eventId))
                .anyMatch(request -> request.getStatus() == RequestStatus.CONFIRMED);
    }
}