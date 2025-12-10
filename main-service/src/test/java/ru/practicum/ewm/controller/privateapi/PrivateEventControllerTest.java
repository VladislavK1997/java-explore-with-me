package ru.practicum.ewm.controller.privateapi;

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
import ru.practicum.ewm.model.EventState;
import ru.practicum.ewm.service.EventService;
import ru.practicum.ewm.service.RequestService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PrivateEventController.class)
@Import(StatsClientConfig.class)
class PrivateEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventService eventService;

    @MockBean
    private RequestService requestService;

    private ObjectMapper objectMapper;
    private NewEventDto newEventDto;
    private EventFullDto eventFullDto;
    private EventShortDto eventShortDto;
    private UpdateEventUserRequest updateRequest;
    private final LocalDateTime now = LocalDateTime.now();

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        newEventDto = new NewEventDto(
                "Event annotation",
                1L,
                "Event description",
                now.plusDays(1),
                new LocationDto(55.754167f, 37.62f),
                false,
                10,
                true,
                "Test Event"
        );

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
                EventState.PENDING,
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

        updateRequest = new UpdateEventUserRequest();
        updateRequest.setTitle("Updated Event");
        updateRequest.setAnnotation("Updated annotation");
        updateRequest.setDescription("Updated description");
        updateRequest.setEventDate(now.plusDays(2));
        updateRequest.setCategory(1L);
        updateRequest.setPaid(true);
        updateRequest.setParticipantLimit(200);
        updateRequest.setRequestModeration(false);
        updateRequest.setStateAction("SEND_TO_REVIEW");
    }

    @Test
    void getEvents_ValidUserId_ReturnsEvents() throws Exception {
        List<EventShortDto> events = List.of(eventShortDto);

        when(eventService.getEventsByUser(eq(1L), eq(0), eq(10))).thenReturn(events);

        mockMvc.perform(get("/users/{userId}/events", 1L)
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].title").value("Test Event"));

        verify(eventService, times(1)).getEventsByUser(1L, 0, 10);
    }

    @Test
    void createEvent_ValidRequest_ReturnsCreated() throws Exception {
        when(eventService.createEvent(eq(1L), any(NewEventDto.class))).thenReturn(eventFullDto);

        mockMvc.perform(post("/users/{userId}/events", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newEventDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Test Event"))
                .andExpect(jsonPath("$.state").value("PENDING"));

        verify(eventService, times(1)).createEvent(eq(1L), any(NewEventDto.class));
    }

    @Test
    void createEvent_InvalidPathParam_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/users/{userId}/events", "invalid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newEventDto)))
                .andExpect(status().isBadRequest());

        verify(eventService, never()).createEvent(any(), any());
    }

    @Test
    void createEvent_InvalidRequestBody_ReturnsBadRequest() throws Exception {
        NewEventDto invalidDto = new NewEventDto();
        invalidDto.setTitle("");

        mockMvc.perform(post("/users/{userId}/events", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());

        verify(eventService, never()).createEvent(any(), any());
    }

    @Test
    void getEvent_ValidIds_ReturnsEvent() throws Exception {
        when(eventService.getEventByUser(1L, 10L)).thenReturn(eventFullDto);

        mockMvc.perform(get("/users/{userId}/events/{eventId}", 1L, 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Test Event"));

        verify(eventService, times(1)).getEventByUser(1L, 10L);
    }

    @Test
    void getEvent_InvalidPathParams_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/users/{userId}/events/{eventId}", "invalid", "invalid"))
                .andExpect(status().isBadRequest());

        verify(eventService, never()).getEventByUser(any(), any());
    }

    @Test
    void updateEvent_ValidRequest_ReturnsOk() throws Exception {
        when(eventService.updateEventByUser(eq(1L), eq(10L), any(UpdateEventUserRequest.class)))
                .thenReturn(eventFullDto);

        mockMvc.perform(patch("/users/{userId}/events/{eventId}", 1L, 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Test Event"));

        verify(eventService, times(1)).updateEventByUser(eq(1L), eq(10L), any(UpdateEventUserRequest.class));
    }

    @Test
    void getEventParticipants_ValidIds_ReturnsRequests() throws Exception {
        ParticipationRequestDto requestDto = new ParticipationRequestDto(
                now, 10L, 1L, 2L, "PENDING"
        );
        List<ParticipationRequestDto> requests = List.of(requestDto);

        when(requestService.getRequestsForEvent(1L, 10L)).thenReturn(requests);

        mockMvc.perform(get("/users/{userId}/events/{eventId}/requests", 1L, 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].event").value(10L))
                .andExpect(jsonPath("$[0].status").value("PENDING"));

        verify(requestService, times(1)).getRequestsForEvent(1L, 10L);
    }

    @Test
    void updateRequestStatus_ValidRequest_ReturnsOk() throws Exception {
        EventRequestStatusUpdateRequest statusRequest = new EventRequestStatusUpdateRequest(
                List.of(1L, 2L),
                "CONFIRMED"
        );

        EventRequestStatusUpdateResult result = new EventRequestStatusUpdateResult(
                List.of(new ParticipationRequestDto(now, 10L, 1L, 2L, "CONFIRMED")),
                List.of(new ParticipationRequestDto(now, 10L, 2L, 3L, "REJECTED"))
        );

        when(requestService.updateRequestStatus(eq(1L), eq(10L), any(EventRequestStatusUpdateRequest.class)))
                .thenReturn(result);

        mockMvc.perform(patch("/users/{userId}/events/{eventId}/requests", 1L, 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmedRequests.length()").value(1))
                .andExpect(jsonPath("$.rejectedRequests.length()").value(1));

        verify(requestService, times(1)).updateRequestStatus(eq(1L), eq(10L), any(EventRequestStatusUpdateRequest.class));
    }

    @Test
    void updateRequestStatus_InvalidStatus_ReturnsBadRequest() throws Exception {
        EventRequestStatusUpdateRequest invalidRequest = new EventRequestStatusUpdateRequest(
                List.of(1L, 2L),
                "INVALID_STATUS"
        );

        mockMvc.perform(patch("/users/{userId}/events/{eventId}/requests", 1L, 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(requestService, never()).updateRequestStatus(any(), any(), any());
    }

    @Test
    void getEvents_DefaultPagination_ReturnsEvents() throws Exception {
        List<EventShortDto> events = List.of(eventShortDto);

        when(eventService.getEventsByUser(eq(1L), eq(0), eq(10))).thenReturn(events);

        mockMvc.perform(get("/users/{userId}/events", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(eventService, times(1)).getEventsByUser(1L, 0, 10);
    }

    @Test
    void createEvent_EventDateInPast_ReturnsBadRequest() throws Exception {
        NewEventDto invalidDto = new NewEventDto(
                "Event annotation",
                1L,
                "Event description",
                now.minusDays(1),
                new LocationDto(55.754167f, 37.62f),
                false,
                10,
                true,
                "Test Event"
        );

        mockMvc.perform(post("/users/{userId}/events", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());

        verify(eventService, never()).createEvent(any(), any());
    }
}