package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.CategoryDto;
import ru.practicum.ewm.dto.NewCategoryDto;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.CategoryMapper;
import ru.practicum.ewm.model.Category;
import ru.practicum.ewm.repository.CategoryRepository;
import ru.practicum.ewm.repository.EventRepository;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;

    @Override
    @Transactional
    public CategoryDto createCategory(NewCategoryDto newCategoryDto) {
        Category category = CategoryMapper.toCategory(newCategoryDto);
        Category savedCategory = categoryRepository.save(category);
        return CategoryMapper.toCategoryDto(savedCategory);
    }

    @Override
    public List<CategoryDto> getCategories(Integer from, Integer size) {
        if (from == null) from = 0;
        if (size == null) size = 10;

        if (from < 0) {
            throw new ru.practicum.ewm.exception.ValidationException("Parameter 'from' must be greater than or equal to 0");
        }
        if (size <= 0) {
            throw new ru.practicum.ewm.exception.ValidationException("Parameter 'size' must be greater than 0");
        }

        int pageNumber = from / size;
        PageRequest page = PageRequest.of(pageNumber, size);
        List<Category> categories = categoryRepository.findAll(page).getContent();

        if (categories.isEmpty()) {
            return Collections.emptyList();
        }

        return categories.stream()
                .map(CategoryMapper::toCategoryDto)
                .collect(Collectors.toList());
    }

    @Override
    public CategoryDto getCategory(Long catId) {
        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Category with id=" + catId + " was not found"));
        return CategoryMapper.toCategoryDto(category);
    }

    @Override
    @Transactional
    public CategoryDto updateCategory(Long catId, CategoryDto categoryDto) {
        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Category with id=" + catId + " was not found"));

        category.setName(categoryDto.getName());
        Category updatedCategory = categoryRepository.save(category);
        return CategoryMapper.toCategoryDto(updatedCategory);
    }

    @Override
    @Transactional
    public void deleteCategory(Long catId) {
        if (!categoryRepository.existsById(catId)) {
            throw new NotFoundException("Category with id=" + catId + " was not found");
        }

        if (eventRepository.countByCategoryId(catId) > 0) {
            throw new ConflictException("The category is not empty");
        }

        categoryRepository.deleteById(catId);
    }
}