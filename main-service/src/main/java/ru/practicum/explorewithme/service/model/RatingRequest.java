package ru.practicum.explorewithme.service.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatingRequest {
    @NotNull(message = "Event ID не может быть пустым")
    private Long eventId;

    @NotNull(message = "Оценка не может быть пустой")
    @Min(value = -1, message = "Оценка должна быть -1 или 1")
    @Max(value = 1, message = "Оценка должна быть -1 или 1")
    private Integer value;
}
