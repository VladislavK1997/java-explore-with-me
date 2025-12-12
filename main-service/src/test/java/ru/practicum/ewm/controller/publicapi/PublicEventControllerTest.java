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
import ru.practicum.ewm.dto.EventShortDto;
import ru.practicum.ewm.dto.CategoryDto;
import ru.practicum.ewm.dto.UserShortDto;
import ru.practicum.ewm.dto.EventFullDto;
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
    private EventShortDto eventShortDto;
    private EventFullDto eventFullDto;
    private final LocalDateTime now = LocalDateTime.now();

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        CategoryDto categoryDto = new CategoryDto(1L, "Concerts");
        UserShortDto userShortDto = new UserShortDto(1L, "John Doe");

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
                ru.practicum.ewm.model.EventState.PUBLISHED,
                "Test Event",
                1000L
        );
    }

    @Test
    void getEvents_WithoutParameters_ShouldReturnEvents() throws Exception {
        List<EventShortDto> events = List.of(eventShortDto);

        when(eventService.getEventsPublic(
                isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(false), isNull(), eq(0), eq(10), anyString())).thenReturn(events);

        mockMvc.perform(get("/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].title").value("Test Event"));

        verify(eventService, times(1)).getEventsPublic(
                isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(false), isNull(), eq(0), eq(10), anyString());
    }

    @Test
    void getEvents_WithEmptyResult_ShouldReturnEmptyList() throws Exception {
        when(eventService.getEventsPublic(
                isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(false), isNull(), eq(0), eq(10), anyString())).thenReturn(List.of());

        mockMvc.perform(get("/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(eventService, times(1)).getEventsPublic(
                isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(false), isNull(), eq(0), eq(10), anyString());
    }

    @Test
    void getEvents_WithTextFilter_ShouldPassTextToService() throws Exception {
        List<EventShortDto> events = List.of(eventShortDto);

        when(eventService.getEventsPublic(
                eq("concert"), isNull(), isNull(), isNull(), isNull(),
                eq(false), isNull(), eq(0), eq(10), anyString())).thenReturn(events);

        mockMvc.perform(get("/events")
                        .param("text", "concert"))
                .andExpect(status().isOk());

        verify(eventService, times(1)).getEventsPublic(
                eq("concert"), isNull(), isNull(), isNull(), isNull(),
                eq(false), isNull(), eq(0), eq(10), anyString());
    }

    @Test
    void getEvents_WithCategories_ShouldPassCategoriesToService() throws Exception {
        List<EventShortDto> events = List.of(eventShortDto);

        when(eventService.getEventsPublic(
                isNull(), eq(List.of(1L, 2L)), isNull(), isNull(), isNull(),
                eq(false), isNull(), eq(0), eq(10), anyString())).thenReturn(events);

        mockMvc.perform(get("/events")
                        .param("categories", "1,2"))
                .andExpect(status().isOk());

        verify(eventService, times(1)).getEventsPublic(
                isNull(), eq(List.of(1L, 2L)), isNull(), isNull(), isNull(),
                eq(false), isNull(), eq(0), eq(10), anyString());
    }

    @Test
    void getEvents_WithPaidFilter_ShouldPassPaidToService() throws Exception {
        List<EventShortDto> events = List.of(eventShortDto);

        when(eventService.getEventsPublic(
                isNull(), isNull(), eq(true), isNull(), isNull(),
                eq(false), isNull(), eq(0), eq(10), anyString())).thenReturn(events);

        mockMvc.perform(get("/events")
                        .param("paid", "true"))
                .andExpect(status().isOk());

        verify(eventService, times(1)).getEventsPublic(
                isNull(), isNull(), eq(true), isNull(), isNull(),
                eq(false), isNull(), eq(0), eq(10), anyString());
    }

    @Test
    void getEvents_WithRangeDates_ShouldPassDatesToService() throws Exception {
        List<EventShortDto> events = List.of(eventShortDto);
        String rangeStart = "2024-01-01 00:00:00";
        String rangeEnd = "2024-12-31 23:59:59";

        when(eventService.getEventsPublic(
                isNull(), isNull(), isNull(), eq(rangeStart), eq(rangeEnd),
                eq(false), isNull(), eq(0), eq(10), anyString())).thenReturn(events);

        mockMvc.perform(get("/events")
                        .param("rangeStart", rangeStart)
                        .param("rangeEnd", rangeEnd))
                .andExpect(status().isOk());

        verify(eventService, times(1)).getEventsPublic(
                isNull(), isNull(), isNull(), eq(rangeStart), eq(rangeEnd),
                eq(false), isNull(), eq(0), eq(10), anyString());
    }

    @Test
    void getEvents_WithOnlyAvailableTrue_ShouldPassTrueToService() throws Exception {
        List<EventShortDto> events = List.of(eventShortDto);

        when(eventService.getEventsPublic(
                isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(true), isNull(), eq(0), eq(10), anyString())).thenReturn(events);

        mockMvc.perform(get("/events")
                        .param("onlyAvailable", "true"))
                .andExpect(status().isOk());

        verify(eventService, times(1)).getEventsPublic(
                isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(true), isNull(), eq(0), eq(10), anyString());
    }

    @Test
    void getEvents_WithSortParameter_ShouldPassSortToService() throws Exception {
        List<EventShortDto> events = List.of(eventShortDto);

        when(eventService.getEventsPublic(
                isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(false), eq("EVENT_DATE"), eq(0), eq(10), anyString())).thenReturn(events);

        mockMvc.perform(get("/events")
                        .param("sort", "EVENT_DATE"))
                .andExpect(status().isOk());

        verify(eventService, times(1)).getEventsPublic(
                isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(false), eq("EVENT_DATE"), eq(0), eq(10), anyString());
    }

    @Test
    void getEvents_WithFromAndSize_ShouldPassPaginationToService() throws Exception {
        List<EventShortDto> events = List.of(eventShortDto);

        when(eventService.getEventsPublic(
                isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(false), isNull(), eq(20), eq(20), anyString())).thenReturn(events);

        mockMvc.perform(get("/events")
                        .param("from", "20")
                        .param("size", "20"))
                .andExpect(status().isOk());

        verify(eventService, times(1)).getEventsPublic(
                isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(false), isNull(), eq(20), eq(20), anyString());
    }

    @Test
    void getEvents_WithEmptyTextAndCategories_ReturnsEvents() throws Exception {
        List<EventShortDto> events = List.of(eventShortDto);

        when(eventService.getEventsPublic(
                eq(""), eq(List.of()), isNull(), isNull(), isNull(),
                eq(false), isNull(), eq(0), eq(10), anyString())).thenReturn(events);

        mockMvc.perform(get("/events")
                        .param("text", "")
                        .param("categories", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(eventService, times(1)).getEventsPublic(
                eq(""), eq(List.of()), isNull(), isNull(), isNull(),
                eq(false), isNull(), eq(0), eq(10), anyString());
    }

    @Test
    void getEvents_WithInvalidSortParameter_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/events")
                        .param("sort", "INVALID_SORT"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getEvents_WithInvalidFromParameter_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/events")
                        .param("from", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getEvents_WithInvalidSizeParameter_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/events")
                        .param("size", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getEvents_WithStartDateAfterEndDate_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/events")
                        .param("rangeStart", "2024-12-31 23:59:59")
                        .param("rangeEnd", "2024-01-01 00:00:00"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getEvents_WithOnlyAvailableAsString_ReturnsEvents() throws Exception {
        List<EventShortDto> events = List.of(eventShortDto);

        when(eventService.getEventsPublic(
                isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(true), isNull(), eq(0), eq(10), anyString())).thenReturn(events);

        mockMvc.perform(get("/events")
                        .param("onlyAvailable", "true"))
                .andExpect(status().isOk());

        verify(eventService, times(1)).getEventsPublic(
                isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(true), isNull(), eq(0), eq(10), anyString());
    }

    @Test
    void getEvents_WithDefaultParameters_ReturnsEvents() throws Exception {
        List<EventShortDto> events = List.of(eventShortDto);

        when(eventService.getEventsPublic(
                isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(false), isNull(), eq(0), eq(10), anyString())).thenReturn(events);

        mockMvc.perform(get("/events"))
                .andExpect(status().isOk());

        verify(eventService, times(1)).getEventsPublic(
                isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(false), isNull(), eq(0), eq(10), anyString());
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

    @Test
    void getEvents_WithValidSortParameter_ReturnsEvents() throws Exception {
        List<EventShortDto> events = List.of(eventShortDto);

        when(eventService.getEventsPublic(
                isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(false), eq("EVENT_DATE"), eq(0), eq(10), anyString())).thenReturn(events);

        mockMvc.perform(get("/events")
                        .param("sort", "EVENT_DATE"))
                .andExpect(status().isOk());

        verify(eventService, times(1)).getEventsPublic(
                isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(false), eq("EVENT_DATE"), eq(0), eq(10), anyString());
    }

    @Test
    void getEvents_WithViewsSort_ReturnsEvents() throws Exception {
        List<EventShortDto> events = List.of(eventShortDto);

        when(eventService.getEventsPublic(
                isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(false), eq("VIEWS"), eq(0), eq(10), anyString())).thenReturn(events);

        mockMvc.perform(get("/events")
                        .param("sort", "VIEWS"))
                .andExpect(status().isOk());

        verify(eventService, times(1)).getEventsPublic(
                isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(false), eq("VIEWS"), eq(0), eq(10), anyString());
    }

    @Test
    void getEvents_WithMultipleCategories_ReturnsEvents() throws Exception {
        List<EventShortDto> events = List.of(eventShortDto);

        when(eventService.getEventsPublic(
                isNull(), eq(List.of(1L, 2L, 3L)), isNull(), isNull(), isNull(),
                eq(false), isNull(), eq(0), eq(10), anyString())).thenReturn(events);

        mockMvc.perform(get("/events")
                        .param("categories", "1", "2", "3"))
                .andExpect(status().isOk());

        verify(eventService, times(1)).getEventsPublic(
                isNull(), eq(List.of(1L, 2L, 3L)), isNull(), isNull(), isNull(),
                eq(false), isNull(), eq(0), eq(10), anyString());
    }

    @Test
    void getEvents_WithSingleCategory_ReturnsEvents() throws Exception {
        List<EventShortDto> events = List.of(eventShortDto);

        when(eventService.getEventsPublic(
                isNull(), eq(List.of(1L)), isNull(), isNull(), isNull(),
                eq(false), isNull(), eq(0), eq(10), anyString())).thenReturn(events);

        mockMvc.perform(get("/events")
                        .param("categories", "1"))
                .andExpect(status().isOk());

        verify(eventService, times(1)).getEventsPublic(
                isNull(), eq(List.of(1L)), isNull(), isNull(), isNull(),
                eq(false), isNull(), eq(0), eq(10), anyString());
    }

    @Test
    void getEvents_WithPaidFalse_ReturnsEvents() throws Exception {
        List<EventShortDto> events = List.of(eventShortDto);

        when(eventService.getEventsPublic(
                isNull(), isNull(), eq(false), isNull(), isNull(),
                eq(false), isNull(), eq(0), eq(10), anyString())).thenReturn(events);

        mockMvc.perform(get("/events")
                        .param("paid", "false"))
                .andExpect(status().isOk());

        verify(eventService, times(1)).getEventsPublic(
                isNull(), isNull(), eq(false), isNull(), isNull(),
                eq(false), isNull(), eq(0), eq(10), anyString());
    }

    @Test
    void getEvents_WithOnlyAvailableFalse_ReturnsEvents() throws Exception {
        List<EventShortDto> events = List.of(eventShortDto);

        when(eventService.getEventsPublic(
                isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(false), isNull(), eq(0), eq(10), anyString())).thenReturn(events);

        mockMvc.perform(get("/events")
                        .param("onlyAvailable", "false"))
                .andExpect(status().isOk());

        verify(eventService, times(1)).getEventsPublic(
                isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(false), isNull(), eq(0), eq(10), anyString());
    }

    @Test
    void getEvents_WithComplexFilters_ReturnsEvents() throws Exception {
        List<EventShortDto> events = List.of(eventShortDto);
        String rangeStart = "2024-06-01 00:00:00";
        String rangeEnd = "2024-06-30 23:59:59";

        when(eventService.getEventsPublic(
                eq("music"), eq(List.of(1L, 2L)), eq(true), eq(rangeStart), eq(rangeEnd),
                eq(true), eq("EVENT_DATE"), eq(10), eq(20), anyString())).thenReturn(events);

        mockMvc.perform(get("/events")
                        .param("text", "music")
                        .param("categories", "1", "2")
                        .param("paid", "true")
                        .param("rangeStart", rangeStart)
                        .param("rangeEnd", rangeEnd)
                        .param("onlyAvailable", "true")
                        .param("sort", "EVENT_DATE")
                        .param("from", "10")
                        .param("size", "20"))
                .andExpect(status().isOk());

        verify(eventService, times(1)).getEventsPublic(
                eq("music"), eq(List.of(1L, 2L)), eq(true), eq(rangeStart), eq(rangeEnd),
                eq(true), eq("EVENT_DATE"), eq(10), eq(20), anyString());
    }

    @Test
    void getEvent_WithInvalidPathParam_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/events/invalid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getEvents_WithInvalidDateFormat_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/events")
                        .param("rangeStart", "invalid-date"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getEvents_ServiceThrowsException_ReturnsInternalServerError() throws Exception {
        when(eventService.getEventsPublic(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), anyString()))
                .thenThrow(new RuntimeException("Service error"));

        mockMvc.perform(get("/events"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getEvent_ServiceThrowsException_ReturnsInternalServerError() throws Exception {
        when(eventService.getEventPublic(eq(1L), anyString()))
                .thenThrow(new RuntimeException("Service error"));

        mockMvc.perform(get("/events/1"))
                .andExpect(status().isInternalServerError());
    }
}