package ru.practicum.ewm.controller.publicapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.config.StatsClientConfig;
import ru.practicum.ewm.dto.CategoryDto;
import ru.practicum.ewm.service.CategoryService;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(StatsClientConfig.class)
@WebMvcTest(PublicCategoryController.class)
@ActiveProfiles("test")
class PublicCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategoryService categoryService;

    private ObjectMapper objectMapper;
    private CategoryDto categoryDto;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        categoryDto = new CategoryDto(1L, "Concerts");
    }

    @Test
    void getCategories_ValidRequest_ReturnsCategories() throws Exception {
        List<CategoryDto> categories = List.of(categoryDto);

        when(categoryService.getCategories(anyInt(), anyInt())).thenReturn(categories);

        mockMvc.perform(get("/categories")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("Concerts"));

        verify(categoryService, times(1)).getCategories(0, 10);
    }

    @Test
    void getCategories_WithoutParams_ReturnsCategories() throws Exception {
        List<CategoryDto> categories = List.of(categoryDto);

        when(categoryService.getCategories(eq(0), eq(10))).thenReturn(categories);

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(categoryService, times(1)).getCategories(eq(0), eq(10));
    }

    @Test
    void getCategories_InvalidSizeParam_ShouldReturnBadRequest() throws Exception {
        // Контроллер сам проверяет @Min(1) для size
        mockMvc.perform(get("/categories")
                        .param("from", "0")
                        .param("size", "0"))
                .andExpect(status().isBadRequest());

        verify(categoryService, never()).getCategories(anyInt(), anyInt());
    }

    @Test
    void getCategories_InvalidFromParam_ShouldReturnBadRequest() throws Exception {
        // Контроллер сам проверяет @Min(0) для from
        mockMvc.perform(get("/categories")
                        .param("from", "-1")
                        .param("size", "10"))
                .andExpect(status().isBadRequest());

        verify(categoryService, never()).getCategories(anyInt(), anyInt());
    }
}