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
import ru.practicum.ewm.dto.EventFullDto;
import ru.practicum.ewm.dto.EventShortDto;
import ru.practicum.ewm.dto.CategoryDto;
import ru.practicum.ewm.dto.UserShortDto;
import ru.practicum.ewm.model.EventState;
import ru.practicum.ewm.service.EventService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(StatsClientConfig.class)
@WebMvcTest(PublicEventController.class)
class PublicEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventService eventService;

    private ObjectMapper objectMapper;
    private EventFullDto eventFullDto;
    private EventShortDto eventShortDto;
    private final LocalDateTime now = LocalDateTime.now();

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        CategoryDto categoryDto = new CategoryDto(1L, "Concerts");
        UserShortDto userShortDto = new UserShortDto(1L, "John Doe");

        eventFullDto = new EventFullDto(
                1L,
                "Event annotation",
                categoryDto,
                50L,
                now.minusDays(1),
                "Full event description",
                now.plusDays(1),
                userShortDto,
                new ru.practicum.ewm.dto.LocationDto(55.754167f, 37.62f),
                true,
                100,
                now.minusHours(1),
                true,
                EventState.PUBLISHED,
                "Test Event",
                1000L
        );

        eventShortDto = new EventShortDto(
                1L,
                "Event annotation",
                categoryDto,
                50L,
                now.plusDays(1),
                userShortDto,
                true,
                "Test Event",
                1000L
        );
    }

    @Test
    void getEvents_WithAllFilters_ReturnsEvents() throws Exception {
        List<EventShortDto> events = List.of(eventShortDto);

        when(eventService.getEventsPublic(anyString(), any(), any(), any(), any(), anyBoolean(),
                anyString(), anyInt(), anyInt(), anyString())).thenReturn(events);

        mockMvc.perform(get("/events")
                        .param("text", "test")
                        .param("categories", "1,2")
                        .param("paid", "true")
                        .param("rangeStart", "2024-01-01 00:00:00")
                        .param("rangeEnd", "2024-12-31 23:59:59")
                        .param("onlyAvailable", "false")
                        .param("sort", "EVENT_DATE")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].title").value("Test Event"));

        verify(eventService, times(1)).getEventsPublic(
                eq("test"), eq(List.of(1L, 2L)), eq(true),
                eq("2024-01-01 00:00:00"), eq("2024-12-31 23:59:59"),
                eq(false), eq("EVENT_DATE"), eq(0), eq(10), anyString());
    }

    @Test
    void getEvents_WithoutFilters_ReturnsEvents() throws Exception {
        List<EventShortDto> events = List.of(eventShortDto);

        when(eventService.getEventsPublic(isNull(), isNull(), isNull(), any(), isNull(),
                eq(false), isNull(), eq(0), eq(10), anyString())).thenReturn(events);

        mockMvc.perform(get("/events")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(eventService, times(1)).getEventsPublic(
                isNull(), isNull(), isNull(), any(), isNull(),
                eq(false), isNull(), eq(0), eq(10), anyString());
    }

    @Test
    void getEvents_DefaultParameters_ReturnsEvents() throws Exception {
        List<EventShortDto> events = List.of(eventShortDto);

        when(eventService.getEventsPublic(isNull(), isNull(), isNull(), any(), isNull(),
                eq(false), isNull(), eq(0), eq(10), anyString())).thenReturn(events);

        mockMvc.perform(get("/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(eventService, times(1)).getEventsPublic(
                isNull(), isNull(), isNull(), any(), isNull(),
                eq(false), isNull(), eq(0), eq(10), anyString());
    }

    @Test
    void getEvent_ValidId_ReturnsEvent() throws Exception {
        when(eventService.getEventPublic(eq(1L), anyString())).thenReturn(eventFullDto);

        mockMvc.perform(get("/events/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Test Event"))
                .andExpect(jsonPath("$.state").value("PUBLISHED"))
                .andExpect(jsonPath("$.views").value(1000L));

        verify(eventService, times(1)).getEventPublic(eq(1L), anyString());
    }

    @Test
    void getEvent_InvalidPathParam_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/events/{id}", "invalid"))
                .andExpect(status().isBadRequest());

        verify(eventService, never()).getEventPublic(any(), any());
    }

    @Test
    void getEvent_NonExistingId_ReturnsNotFound() throws Exception {
        when(eventService.getEventPublic(eq(999L), anyString()))
                .thenThrow(new ru.practicum.ewm.exception.NotFoundException("Event not found"));

        mockMvc.perform(get("/events/{id}", 999L))
                .andExpect(status().isNotFound());

        verify(eventService, times(1)).getEventPublic(eq(999L), anyString());
    }

    @Test
    void getEvents_WithSortByViews_ReturnsSortedEvents() throws Exception {
        List<EventShortDto> events = List.of(eventShortDto);

        when(eventService.getEventsPublic(isNull(), isNull(), isNull(), any(), isNull(),
                eq(false), eq("VIEWS"), eq(0), eq(10), anyString())).thenReturn(events);

        mockMvc.perform(get("/events")
                        .param("sort", "VIEWS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(eventService, times(1)).getEventsPublic(
                isNull(), isNull(), isNull(), any(), isNull(),
                eq(false), eq("VIEWS"), eq(0), eq(10), anyString());
    }

    @Test
    void getEvents_WithOnlyAvailable_ReturnsAvailableEvents() throws Exception {
        List<EventShortDto> events = List.of(eventShortDto);

        when(eventService.getEventsPublic(isNull(), isNull(), isNull(), any(), isNull(),
                eq(true), isNull(), eq(0), eq(10), anyString())).thenReturn(events);

        mockMvc.perform(get("/events")
                        .param("onlyAvailable", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(eventService, times(1)).getEventsPublic(
                isNull(), isNull(), isNull(), any(), isNull(),
                eq(true), isNull(), eq(0), eq(10), anyString());
    }

    @Test
    void getEvents_InvalidSortParameter_ReturnsBadRequest() throws Exception {
        when(eventService.getEventsPublic(any(), any(), any(), any(), any(),
                anyBoolean(), any(), anyInt(), anyInt(), anyString()))
                .thenThrow(new ru.practicum.ewm.exception.ValidationException("Invalid sort parameter"));

        mockMvc.perform(get("/events")
                        .param("sort", "INVALID_SORT"))
                .andExpect(status().isBadRequest());

        verify(eventService, times(1)).getEventsPublic(any(), any(), any(), any(), any(),
                anyBoolean(), anyString(), anyInt(), anyInt(), anyString());
    }

    @Test
    void getEvents_InvalidDateFormats_ReturnsOkWithDefaults() throws Exception {
        List<EventShortDto> events = List.of(eventShortDto);

        when(eventService.getEventsPublic(any(), any(), any(), any(), any(), anyBoolean(),
                anyString(), anyInt(), anyInt(), anyString())).thenReturn(events);

        mockMvc.perform(get("/events")
                        .param("rangeStart", "invalid-date")
                        .param("rangeEnd", "invalid-date"))
                .andExpect(status().isOk());

        verify(eventService, times(1)).getEventsPublic(any(), any(), any(), any(), any(),
                anyBoolean(), any(), anyInt(), anyInt(), anyString());
    }


    @Test
    void getEvents_TextTooLong_ReturnsBadRequest() throws Exception {
        String longText = "a".repeat(7001);

        mockMvc.perform(get("/events")
                        .param("text", longText))
                .andExpect(status().isBadRequest());

        verify(eventService, never()).getEventsPublic(any(), any(), any(), any(), any(),
                anyBoolean(), any(), anyInt(), anyInt(), anyString());
    }
}