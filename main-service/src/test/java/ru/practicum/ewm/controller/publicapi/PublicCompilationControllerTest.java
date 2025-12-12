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
import ru.practicum.ewm.dto.CompilationDto;
import ru.practicum.ewm.dto.EventShortDto;
import ru.practicum.ewm.dto.CategoryDto;
import ru.practicum.ewm.dto.UserShortDto;
import ru.practicum.ewm.service.CompilationService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(StatsClientConfig.class)
@WebMvcTest(PublicCompilationController.class)
@ActiveProfiles("test")
class PublicCompilationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CompilationService compilationService;

    private ObjectMapper objectMapper;
    private CompilationDto compilationDto;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        EventShortDto event1 = new EventShortDto(
                1L, "Annotation 1", new CategoryDto(1L, "Concerts"),
                50L, LocalDateTime.now().plusDays(1),
                new UserShortDto(1L, "User1"), true, "Event 1", 100L);

        EventShortDto event2 = new EventShortDto(
                2L, "Annotation 2", new CategoryDto(2L, "Theater"),
                30L, LocalDateTime.now().plusDays(2),
                new UserShortDto(2L, "User2"), false, "Event 2", 200L);

        compilationDto = new CompilationDto(
                List.of(event1, event2),
                1L,
                true,
                "Summer Events"
        );
    }

    @Test
    void getCompilations_InvalidPaginationParams_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/compilations")
                        .param("from", "-1")  // невалидно
                        .param("size", "10"))
                .andExpect(status().isBadRequest());

        verify(compilationService, never()).getCompilations(any(), anyInt(), anyInt());
    }

    @Test
    void getCompilations_InvalidSizeParam_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/compilations")
                        .param("from", "0")
                        .param("size", "0"))  // невалидно
                .andExpect(status().isBadRequest());

        verify(compilationService, never()).getCompilations(any(), anyInt(), anyInt());
    }
}