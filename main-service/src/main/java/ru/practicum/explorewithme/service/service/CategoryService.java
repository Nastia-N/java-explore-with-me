package ru.practicum.explorewithme.service.service;

import ru.practicum.explorewithme.service.dto.CategoryDto;
import ru.practicum.explorewithme.service.dto.NewCategoryDto;
import ru.practicum.explorewithme.service.dto.UpdateCategoryDto;

import java.util.List;

public interface CategoryService {

    CategoryDto createCategory(NewCategoryDto newCategoryDto);

    List<CategoryDto> getCategories(int from, int size);

    CategoryDto getCategoryById(Long catId);

    CategoryDto updateCategory(Long catId, UpdateCategoryDto categoryDto);

    void deleteCategory(Long catId);

}
