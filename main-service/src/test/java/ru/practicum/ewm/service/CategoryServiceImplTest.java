package ru.practicum.ewm.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.ewm.dto.CategoryDto;
import ru.practicum.ewm.dto.NewCategoryDto;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.CategoryMapper;
import ru.practicum.ewm.model.Category;
import ru.practicum.ewm.repository.CategoryRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    @Test
    void createCategory_ValidData_ReturnsCategoryDto() {
        // Given
        NewCategoryDto request = new NewCategoryDto("Concerts");
        Category category = CategoryMapper.toCategory(request);
        category.setId(1L);

        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        // When
        CategoryDto result = categoryService.createCategory(request);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Concerts", result.getName());
        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    @Test
    void getCategories_WithPagination_ReturnsCategories() {
        // Given
        Category category1 = Category.builder().id(1L).name("Concerts").build();
        Category category2 = Category.builder().id(2L).name("Theater").build();
        Page<Category> page = new PageImpl<>(List.of(category1, category2));

        when(categoryRepository.findAll(PageRequest.of(0, 10))).thenReturn(page);

        // When
        List<CategoryDto> result = categoryService.getCategories(0, 10);

        // Then
        assertEquals(2, result.size());
        assertEquals("Concerts", result.get(0).getName());
        assertEquals("Theater", result.get(1).getName());
        verify(categoryRepository, times(1)).findAll(PageRequest.of(0, 10));
    }

    @Test
    void getCategory_ExistingId_ReturnsCategoryDto() {
        // Given
        Long categoryId = 1L;
        Category category = Category.builder().id(categoryId).name("Concerts").build();

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        // When
        CategoryDto result = categoryService.getCategory(categoryId);

        // Then
        assertNotNull(result);
        assertEquals(categoryId, result.getId());
        assertEquals("Concerts", result.getName());
        verify(categoryRepository, times(1)).findById(categoryId);
    }

    @Test
    void getCategory_NonExistingId_ThrowsNotFoundException() {
        // Given
        Long categoryId = 999L;
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        // When & Then
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> categoryService.getCategory(categoryId));
        assertEquals("Category with id=999 was not found", exception.getMessage());
        verify(categoryRepository, times(1)).findById(categoryId);
    }

    @Test
    void updateCategory_ValidData_ReturnsUpdatedCategoryDto() {
        // Given
        Long categoryId = 1L;
        CategoryDto updateDto = new CategoryDto(categoryId, "Updated Concerts");
        Category existingCategory = Category.builder().id(categoryId).name("Concerts").build();
        Category savedCategory = Category.builder().id(categoryId).name("Updated Concerts").build();

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(existingCategory));
        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);

        // When
        CategoryDto result = categoryService.updateCategory(categoryId, updateDto);

        // Then
        assertNotNull(result);
        assertEquals(categoryId, result.getId());
        assertEquals("Updated Concerts", result.getName());
        verify(categoryRepository, times(1)).findById(categoryId);
        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    @Test
    void updateCategory_NonExistingId_ThrowsNotFoundException() {
        // Given
        Long categoryId = 999L;
        CategoryDto updateDto = new CategoryDto(categoryId, "Updated Concerts");

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        // When & Then
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> categoryService.updateCategory(categoryId, updateDto));
        assertEquals("Category with id=999 was not found", exception.getMessage());
        verify(categoryRepository, times(1)).findById(categoryId);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void deleteCategory_ExistingId_DeletesSuccessfully() {
        // Given
        Long categoryId = 1L;
        when(categoryRepository.existsById(categoryId)).thenReturn(true);

        // When
        categoryService.deleteCategory(categoryId);

        // Then
        verify(categoryRepository, times(1)).existsById(categoryId);
        verify(categoryRepository, times(1)).deleteById(categoryId);
    }

    @Test
    void deleteCategory_NonExistingId_ThrowsNotFoundException() {
        // Given
        Long categoryId = 999L;
        when(categoryRepository.existsById(categoryId)).thenReturn(false);

        // When & Then
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> categoryService.deleteCategory(categoryId));
        assertEquals("Category with id=999 was not found", exception.getMessage());
        verify(categoryRepository, times(1)).existsById(categoryId);
        verify(categoryRepository, never()).deleteById(categoryId);
    }

    @Test
    void getCategories_EmptyResult_ReturnsEmptyList() {
        // Given
        Page<Category> emptyPage = new PageImpl<>(List.of());
        when(categoryRepository.findAll(PageRequest.of(0, 10))).thenReturn(emptyPage);

        // When
        List<CategoryDto> result = categoryService.getCategories(0, 10);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getCategories_SecondPage_ReturnsCorrectResults() {
        // Given
        Category category3 = Category.builder().id(3L).name("Cinema").build();
        Category category4 = Category.builder().id(4L).name("Sports").build();
        Page<Category> page = new PageImpl<>(List.of(category3, category4));

        when(categoryRepository.findAll(PageRequest.of(1, 2))).thenReturn(page);

        // When
        List<CategoryDto> result = categoryService.getCategories(2, 2);

        // Then
        assertEquals(2, result.size());
        assertEquals(3L, result.get(0).getId());
        assertEquals(4L, result.get(1).getId());
    }
}