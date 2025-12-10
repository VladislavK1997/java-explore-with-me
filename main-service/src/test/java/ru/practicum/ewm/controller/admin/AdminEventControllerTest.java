package ru.practicum.ewm.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.dto.*;
import ru.practicum.ewm.model.EventState;
import ru.practicum.ewm.service.EventService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@WebMvcTest(AdminEventController.class)
class AdminEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventService eventService;

    private ObjectMapper objectMapper;
    private EventFullDto eventFullDto;
    private UpdateEventAdminRequest updateRequest;
    private final LocalDateTime now = LocalDateTime.now();

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        CategoryDto categoryDto = new CategoryDto(1L, "Concerts");
        UserShortDto userShortDto = new UserShortDto(1L, "John Doe");
        LocationDto locationDto = new LocationDto(55.754167f, 37.62f);

        eventFullDto = new EventFullDto(
                1L,
                "Event annotation",
                categoryDto,
                50L,
                now.minusDays(1),
                "Full event description",
                now.plusDays(1),
                userShortDto,
                locationDto,
                true,
                100,
                now.minusHours(1),
                true,
                EventState.PUBLISHED,
                "Test Event",
                1000L
        );

        updateRequest = new UpdateEventAdminRequest();
        updateRequest.setTitle("Updated Event");
        updateRequest.setAnnotation("Updated annotation");
        updateRequest.setDescription("Updated description");
        updateRequest.setEventDate(now.plusDays(2));
        updateRequest.setCategory(1L);
        updateRequest.setPaid(true);
        updateRequest.setParticipantLimit(200);
        updateRequest.setRequestModeration(false);
        updateRequest.setStateAction("PUBLISH_EVENT");
    }

    @Test
    void getEvents_WithFilters_ReturnsEvents() throws Exception {
        List<EventFullDto> events = List.of(eventFullDto);

        when(eventService.getEventsByAdmin(any(), any(), any(), any(), any(), eq(0), eq(10)))
                .thenReturn(events);

        mockMvc.perform(get("/admin/events")
                        .param("users", "1,2")
                        .param("states", "PUBLISHED,PENDING")
                        .param("categories", "1,2")
                        .param("rangeStart", "2024-01-01 00:00:00")
                        .param("rangeEnd", "2024-12-31 23:59:59")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].title").value("Test Event"));

        verify(eventService, times(1)).getEventsByAdmin(
                List.of(1L, 2L),
                List.of("PUBLISHED", "PENDING"),
                List.of(1L, 2L),
                "2024-01-01 00:00:00",
                "2024-12-31 23:59:59",
                0, 10);
    }

    @Test
    void getEvents_WithoutFilters_ReturnsEvents() throws Exception {
        List<EventFullDto> events = List.of(eventFullDto);

        when(eventService.getEventsByAdmin(isNull(), isNull(), isNull(), isNull(), isNull(), eq(0), eq(10)))
                .thenReturn(events);

        mockMvc.perform(get("/admin/events")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(eventService, times(1)).getEventsByAdmin(
                isNull(), isNull(), isNull(), isNull(), isNull(), eq(0), eq(10));
    }

    @Test
    void updateEvent_ValidRequest_ReturnsOk() throws Exception {
        when(eventService.updateEventByAdmin(eq(1L), any(UpdateEventAdminRequest.class)))
                .thenReturn(eventFullDto);

        mockMvc.perform(patch("/admin/events/{eventId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Test Event"));

        verify(eventService, times(1)).updateEventByAdmin(eq(1L), any(UpdateEventAdminRequest.class));
    }

    @Test
    void updateEvent_InvalidPathParam_ReturnsBadRequest() throws Exception {
        mockMvc.perform(patch("/admin/events/{eventId}", "invalid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest());

        verify(eventService, never()).updateEventByAdmin(any(), any());
    }

    @Test
    void updateEvent_InvalidStateAction_ReturnsBadRequest() throws Exception {
        updateRequest.setStateAction("INVALID_ACTION");

        mockMvc.perform(patch("/admin/events/{eventId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest());

        verify(eventService, never()).updateEventByAdmin(any(), any());
    }

    @Test
    void getEvents_InvalidDateFormats_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/admin/events")
                        .param("rangeStart", "invalid-date")
                        .param("rangeEnd", "invalid-date"))
                .andExpect(status().isBadRequest());

        verify(eventService, never()).getEventsByAdmin(any(), any(), any(), any(), any(), anyInt(), anyInt());
    }
}