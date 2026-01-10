package ru.practicum.explorewithme.service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.explorewithme.service.dto.CategoryDto;
import ru.practicum.explorewithme.service.dto.NewCategoryDto;
import ru.practicum.explorewithme.service.exception.ConflictException;
import ru.practicum.explorewithme.service.exception.NotFoundException;
import ru.practicum.explorewithme.service.mapper.CategoryMapper;
import ru.practicum.explorewithme.service.model.Category;
import ru.practicum.explorewithme.service.repository.CategoryRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional
    public CategoryDto createCategory(NewCategoryDto newCategoryDto) {
        log.info("Создание новой категории с названием: {}", newCategoryDto.getName());

        if (categoryRepository.existsByName(newCategoryDto.getName())) {
            throw new ConflictException("Категория с name=" + newCategoryDto.getName() + " уже существует");
        }

        Category category = categoryMapper.toEntity(newCategoryDto);
        Category savedCategory = categoryRepository.save(category);

        log.info("Создана категория с id: {}", savedCategory.getId());
        return categoryMapper.toDto(savedCategory);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDto> getCategories(int from, int size) {
        log.info("Получение категорий from: {}, size: {}", from, size);

        if (size <= 0) {
            throw new IllegalArgumentException("Параметр size должен быть больше 0");
        }
        if (from < 0) {
            throw new IllegalArgumentException("Параметр from должен быть неотрицательным");
        }

        PageRequest pageRequest = PageRequest.of(from / size, size);
        return categoryRepository.findAll(pageRequest)
                .stream()
                .map(categoryMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryDto getCategoryById(Long catId) {
        log.info("Получение категории по id: {}", catId);

        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Category", catId));

        return categoryMapper.toDto(category);
    }

    @Override
    @Transactional
    public CategoryDto updateCategory(Long catId, CategoryDto categoryDto) {
        log.info("Обновление категории с id: {}", catId);

        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Category", catId));

        if (categoryRepository.existsByNameAndIdNot(categoryDto.getName(), catId)) {
            throw new ConflictException("Категория с name=" + categoryDto.getName() + " уже существует");
        }

        category.setName(categoryDto.getName());
        Category updatedCategory = categoryRepository.save(category);

        log.info("Категория с id {} успешно обновлена", catId);
        return categoryMapper.toDto(updatedCategory);
    }

    @Override
    @Transactional
    public void deleteCategory(Long catId) {
        log.info("Удаление категории с id: {}", catId);

        if (!categoryRepository.existsById(catId)) {
            throw new NotFoundException("Category", catId);
        }

        categoryRepository.deleteById(catId);
        log.info("Категория с id {} успешно удалена", catId);
    }
}
