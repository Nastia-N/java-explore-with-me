package ru.practicum.explorewithme.service.service;

import ru.practicum.explorewithme.service.dto.CategoryDto;
import ru.practicum.explorewithme.service.dto.NewCategoryDto;

import java.util.List;

public interface CategoryService {

    CategoryDto createCategory(NewCategoryDto newCategoryDto);

    List<CategoryDto> getCategories(int from, int size);

    CategoryDto getCategoryById(Long catId);

    CategoryDto updateCategory(Long catId, CategoryDto categoryDto);

    void deleteCategory(Long catId);

}
