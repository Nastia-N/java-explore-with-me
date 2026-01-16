package ru.practicum.explorewithme.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewCompilationDto {

    private List<Long> events;
    private Boolean pinned;

    @NotBlank(message = "Заголовок подборки не может быть пустым")
    @Size(min = 1, max = 50, message = "Заголовок должен быть от 1 до 50 символов")
    private String title;
}
