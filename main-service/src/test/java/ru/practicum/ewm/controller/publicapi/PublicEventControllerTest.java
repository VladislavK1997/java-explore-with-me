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
@ActiveProfiles("test")
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
    void getEvents_WithoutOnlyAvailableParam_ShouldUseDefaultFalse() throws Exception {
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
    void getEvents_WithOnlyAvailableTrue_ShouldPassTrueToService() throws Exception {
        List<EventShortDto> events = List.of(eventShortDto);

        when(eventService.getEventsPublic(isNull(), isNull(), isNull(), any(), isNull(),
                eq(true), isNull(), eq(0), eq(10), anyString())).thenReturn(events);

        mockMvc.perform(get("/events")
                        .param("onlyAvailable", "true"))
                .andExpect(status().isOk());

        verify(eventService, times(1)).getEventsPublic(
                isNull(), isNull(), isNull(), any(), isNull(),
                eq(true), isNull(), eq(0), eq(10), anyString());
    }

    @Test
    void getEvents_WithOnlyAvailableFalse_ShouldPassFalseToService() throws Exception {
        List<EventShortDto> events = List.of(eventShortDto);

        when(eventService.getEventsPublic(isNull(), isNull(), isNull(), any(), isNull(),
                eq(false), isNull(), eq(0), eq(10), anyString())).thenReturn(events);

        mockMvc.perform(get("/events")
                        .param("onlyAvailable", "false"))
                .andExpect(status().isOk());

        verify(eventService, times(1)).getEventsPublic(
                isNull(), isNull(), isNull(), any(), isNull(),
                eq(false), isNull(), eq(0), eq(10), anyString());
    }

    @Test
    void getEvents_WithInvalidFromParam_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/events")
                        .param("from", "-1"))
                .andExpect(status().isBadRequest());

        verify(eventService, never()).getEventsPublic(any(), any(), any(), any(), any(),
                anyBoolean(), any(), anyInt(), anyInt(), anyString());
    }

    @Test
    void getEvents_WithInvalidSizeParam_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/events")
                        .param("size", "0"))
                .andExpect(status().isBadRequest());

        verify(eventService, never()).getEventsPublic(any(), any(), any(), any(), any(),
                anyBoolean(), any(), anyInt(), anyInt(), anyString());
    }

    @Test
    void getEvents_WithInvalidDateFormat_ShouldReturnBadRequest() throws Exception {
        when(eventService.getEventsPublic(any(), any(), any(), any(), any(),
                anyBoolean(), any(), anyInt(), anyInt(), anyString()))
                .thenThrow(new ru.practicum.ewm.exception.ValidationException("Invalid date format"));

        mockMvc.perform(get("/events")
                        .param("rangeStart", "invalid-date"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getEvents_WithInvalidSortParam_ShouldReturnBadRequest() throws Exception {
        when(eventService.getEventsPublic(any(), any(), any(), any(), any(),
                anyBoolean(), any(), anyInt(), anyInt(), anyString()))
                .thenThrow(new ru.practicum.ewm.exception.ValidationException("Invalid sort parameter"));

        mockMvc.perform(get("/events")
                        .param("sort", "INVALID"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getEvent_ValidId_ReturnsEvent() throws Exception {
        when(eventService.getEventPublic(eq(1L), anyString())).thenReturn(eventFullDto);

        mockMvc.perform(get("/events/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Test Event"));

        verify(eventService, times(1)).getEventPublic(eq(1L), anyString());
    }

    @Test
    void getEvent_EventNotFound_ReturnsNotFound() throws Exception {
        when(eventService.getEventPublic(eq(999L), anyString()))
                .thenThrow(new ru.practicum.ewm.exception.NotFoundException("Event with id=999 was not found"));

        mockMvc.perform(get("/events/999"))
                .andExpect(status().isNotFound());

        verify(eventService, times(1)).getEventPublic(eq(999L), anyString());
    }
}