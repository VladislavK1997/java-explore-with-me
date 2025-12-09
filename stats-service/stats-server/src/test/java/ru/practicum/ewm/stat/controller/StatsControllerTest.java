package ru.practicum.ewm.stat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.stat.dto.EndpointHitDto;
import ru.practicum.ewm.stat.dto.ViewStatsDto;
import ru.practicum.ewm.stat.service.StatsService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StatsController.class)
class StatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StatsService statsService;

    private ObjectMapper objectMapper;
    private EndpointHitDto endpointHitDto;
    private LocalDateTime now;
    private DateTimeFormatter formatter;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        now = LocalDateTime.now().withNano(0);

        endpointHitDto = new EndpointHitDto(
                1L,
                "ewm-main-service",
                "/events/1",
                "192.168.1.1",
                now
        );
    }

    @Test
    void shouldCreateHit() throws Exception {
        doNothing().when(statsService).saveHit(any(EndpointHitDto.class));

        mockMvc.perform(post("/hit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(endpointHitDto)))
                .andExpect(status().isCreated());

        verify(statsService, times(1)).saveHit(any(EndpointHitDto.class));
    }

    @Test
    void shouldReturnBadRequestWhenHitHasInvalidData() throws Exception {
        EndpointHitDto invalidDto = new EndpointHitDto(
                1L,
                "",
                "/events/1",
                "192.168.1.1",
                now
        );

        mockMvc.perform(post("/hit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());

        verify(statsService, never()).saveHit(any());
    }

    @Test
    void shouldGetStats() throws Exception {
        LocalDateTime start = now.minusDays(1).withNano(0);
        LocalDateTime end = now.plusDays(1).withNano(0);
        List<String> uris = List.of("/events/1");
        Boolean unique = false;

        List<ViewStatsDto> expectedStats = List.of(
                new ViewStatsDto("ewm-main-service", "/events/1", 10L)
        );

        when(statsService.getStats(start, end, uris, unique)).thenReturn(expectedStats);

        mockMvc.perform(get("/stats")
                        .param("start", start.format(formatter))
                        .param("end", end.format(formatter))
                        .param("uris", "/events/1")
                        .param("unique", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].app").value("ewm-main-service"))
                .andExpect(jsonPath("$[0].uri").value("/events/1"))
                .andExpect(jsonPath("$[0].hits").value(10));

        verify(statsService, times(1)).getStats(start, end, uris, unique);
    }

    @Test
    void shouldGetStatsWithMultipleUris() throws Exception {
        LocalDateTime start = now.minusDays(1).withNano(0);
        LocalDateTime end = now.plusDays(1).withNano(0);

        List<ViewStatsDto> expectedStats = List.of(
                new ViewStatsDto("ewm-main-service", "/events/1", 10L),
                new ViewStatsDto("ewm-main-service", "/events/2", 5L)
        );

        when(statsService.getStats(eq(start), eq(end), any(List.class), eq(false)))
                .thenReturn(expectedStats);

        mockMvc.perform(get("/stats")
                        .param("start", start.format(formatter))
                        .param("end", end.format(formatter))
                        .param("uris", "/events/1", "/events/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].hits").value(10))
                .andExpect(jsonPath("$[1].hits").value(5));
    }

    @Test
    void shouldGetUniqueStats() throws Exception {
        LocalDateTime start = now.minusDays(1).withNano(0);
        LocalDateTime end = now.plusDays(1).withNano(0);

        List<ViewStatsDto> expectedStats = List.of(
                new ViewStatsDto("ewm-main-service", "/events/1", 5L)
        );

        when(statsService.getStats(start, end, null, true)).thenReturn(expectedStats);

        mockMvc.perform(get("/stats")
                        .param("start", start.format(formatter))
                        .param("end", end.format(formatter))
                        .param("unique", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].hits").value(5));

        verify(statsService, times(1)).getStats(start, end, null, true);
    }

    @Test
    void shouldGetStatsWithoutUrisParameter() throws Exception {
        LocalDateTime start = now.minusDays(1).withNano(0);
        LocalDateTime end = now.plusDays(1).withNano(0);

        List<ViewStatsDto> expectedStats = List.of(
                new ViewStatsDto("ewm-main-service", "/events/1", 10L)
        );

        when(statsService.getStats(start, end, null, false)).thenReturn(expectedStats);

        mockMvc.perform(get("/stats")
                        .param("start", start.format(formatter))
                        .param("end", end.format(formatter)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].app").value("ewm-main-service"))
                .andExpect(jsonPath("$[0].uri").value("/events/1"))
                .andExpect(jsonPath("$[0].hits").value(10));

        verify(statsService, times(1)).getStats(start, end, null, false);
    }

    @Test
    void shouldReturnBadRequestWhenDatesInvalid() throws Exception {
        LocalDateTime start = now.plusDays(1).withNano(0);
        LocalDateTime end = now.minusDays(1).withNano(0);

        when(statsService.getStats(start, end, null, false))
                .thenThrow(new IllegalArgumentException("Start date must be before end date"));

        mockMvc.perform(get("/stats")
                        .param("start", start.format(formatter))
                        .param("end", end.format(formatter)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnEmptyListWhenNoStatsFound() throws Exception {
        LocalDateTime start = now.minusDays(1).withNano(0);
        LocalDateTime end = now.plusDays(1).withNano(0);

        when(statsService.getStats(start, end, null, false)).thenReturn(List.of());

        mockMvc.perform(get("/stats")
                        .param("start", start.format(formatter))
                        .param("end", end.format(formatter)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldHandleInvalidDateFormat() throws Exception {
        mockMvc.perform(get("/stats")
                        .param("start", "invalid-date")
                        .param("end", "invalid-date"))
                .andExpect(status().isBadRequest());
    }
}