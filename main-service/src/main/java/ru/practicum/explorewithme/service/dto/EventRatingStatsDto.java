package ru.practicum.explorewithme.service.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EventRatingStatsDto {
    private Long eventId;
    private String eventTitle;
    private long likes;
    private long dislikes;
    private long totalRating;
    private Integer userVote;
}
