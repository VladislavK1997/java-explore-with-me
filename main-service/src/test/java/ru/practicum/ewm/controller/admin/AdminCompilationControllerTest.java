package ru.practicum.ewm.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.config.StatsClientConfig;
import ru.practicum.ewm.dto.*;
import ru.practicum.ewm.service.CompilationService;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminCompilationController.class)
@Import(StatsClientConfig.class)
class AdminCompilationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CompilationService compilationService;

    private ObjectMapper objectMapper;
    private NewCompilationDto newCompilationDto;
    private CompilationDto compilationDto;
    private UpdateCompilationRequest updateRequest;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        newCompilationDto = new NewCompilationDto(
                List.of(1L, 2L, 3L),
                true,
                "Summer Events"
        );

        EventShortDto event1 = new EventShortDto(
                1L, "Annotation 1", new CategoryDto(1L, "Concerts"),
                50L, null, new UserShortDto(1L, "User1"),
                true, "Event 1", 100L);

        EventShortDto event2 = new EventShortDto(
                2L, "Annotation 2", new CategoryDto(2L, "Theater"),
                30L, null, new UserShortDto(2L, "User2"),
                false, "Event 2", 200L);

        compilationDto = new CompilationDto(
                List.of(event1, event2),
                1L,
                true,
                "Summer Events"
        );

        updateRequest = new UpdateCompilationRequest();
        updateRequest.setEvents(List.of(1L, 2L));
        updateRequest.setPinned(false);
        updateRequest.setTitle("Updated Compilation");
    }

    @Test
    void createCompilation_ValidRequest_ReturnsCreated() throws Exception {
        when(compilationService.createCompilation(any(NewCompilationDto.class))).thenReturn(compilationDto);

        mockMvc.perform(post("/admin/compilations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCompilationDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Summer Events"))
                .andExpect(jsonPath("$.pinned").value(true))
                .andExpect(jsonPath("$.events.length()").value(2));

        verify(compilationService, times(1)).createCompilation(any(NewCompilationDto.class));
    }

    @Test
    void createCompilation_InvalidRequest_ReturnsBadRequest() throws Exception {
        NewCompilationDto invalidDto = new NewCompilationDto();
        invalidDto.setTitle("");

        mockMvc.perform(post("/admin/compilations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());

        verify(compilationService, never()).createCompilation(any());
    }

    @Test
    void updateCompilation_ValidRequest_ReturnsOk() throws Exception {
        when(compilationService.updateCompilation(eq(1L), any(UpdateCompilationRequest.class)))
                .thenReturn(compilationDto);

        mockMvc.perform(patch("/admin/compilations/{compId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Summer Events"));

        verify(compilationService, times(1)).updateCompilation(eq(1L), any(UpdateCompilationRequest.class));
    }

    @Test
    void updateCompilation_InvalidPathParam_ReturnsBadRequest() throws Exception {
        mockMvc.perform(patch("/admin/compilations/{compId}", "invalid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest());

        verify(compilationService, never()).updateCompilation(any(), any());
    }

    @Test
    void deleteCompilation_ValidId_ReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/admin/compilations/{compId}", 1L))
                .andExpect(status().isNoContent());

        verify(compilationService, times(1)).deleteCompilation(1L);
    }

    @Test
    void deleteCompilation_InvalidPathParam_ReturnsBadRequest() throws Exception {
        mockMvc.perform(delete("/admin/compilations/{compId}", "invalid"))
                .andExpect(status().isBadRequest());

        verify(compilationService, never()).deleteCompilation(any());
    }

    @Test
    void createCompilation_WithoutEvents_ReturnsCreated() throws Exception {
        NewCompilationDto dtoWithoutEvents = new NewCompilationDto();
        dtoWithoutEvents.setTitle("Empty Compilation");
        dtoWithoutEvents.setPinned(false);

        CompilationDto emptyCompilation = new CompilationDto(
                null,
                2L,
                false,
                "Empty Compilation"
        );

        when(compilationService.createCompilation(any(NewCompilationDto.class))).thenReturn(emptyCompilation);

        mockMvc.perform(post("/admin/compilations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoWithoutEvents)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Empty Compilation"))
                .andExpect(jsonPath("$.pinned").value(false));

        verify(compilationService, times(1)).createCompilation(any(NewCompilationDto.class));
    }
}