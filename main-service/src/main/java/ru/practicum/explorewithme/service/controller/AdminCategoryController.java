package ru.practicum.explorewithme.service.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.explorewithme.service.dto.CategoryDto;
import ru.practicum.explorewithme.service.dto.NewCategoryDto;
import ru.practicum.explorewithme.service.service.CategoryService;

@RestController
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
@Slf4j
@Validated
public class AdminCategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDto createCategory(@Valid @RequestBody NewCategoryDto newCategoryDto) {
        log.info("Admin: Создание категории с названием: {}", newCategoryDto.getName());
        return categoryService.createCategory(newCategoryDto);
    }

    @PatchMapping("/{catId}")
    public CategoryDto updateCategory(
            @PathVariable @Positive Long catId,
            @Valid @RequestBody CategoryDto categoryDto) {

        log.info("Admin: Обновление категории с id: {}", catId);
        return categoryService.updateCategory(catId, categoryDto);
    }

    @DeleteMapping("/{catId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable @Positive Long catId) {
        log.info("Admin: Удаление категории с id: {}", catId);
        categoryService.deleteCategory(catId);
    }
}
