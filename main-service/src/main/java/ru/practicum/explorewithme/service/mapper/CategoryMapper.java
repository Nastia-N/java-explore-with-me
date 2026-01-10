package ru.practicum.explorewithme.service.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.explorewithme.service.dto.CategoryDto;
import ru.practicum.explorewithme.service.dto.NewCategoryDto;
import ru.practicum.explorewithme.service.model.Category;

@Component
public class CategoryMapper {

    public CategoryDto toDto(Category category) {
        if (category == null) {
            return null;
        }

        CategoryDto dto = new CategoryDto();
        dto.setId(category.getId());
        dto.setName(category.getName());
        return dto;
    }

    public Category toEntity(NewCategoryDto newCategoryDto) {
        if (newCategoryDto == null) {
            return null;
        }

        return Category.builder()
                .name(newCategoryDto.getName())
                .build();
    }
}