package ru.practicum.ewm.controller.privateapi;

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
import ru.practicum.ewm.service.RequestService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PrivateEventController.class)
@ActiveProfiles("test")
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
    private UpdateEventUserRequest updateRequest;
    private final LocalDateTime now = LocalDateTime.now();

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        newEventDto = new NewEventDto(
                "Event annotation more than 20 characters",
                1L,
                "Event description more than 20 characters",
                now.plusDays(1),
                new LocationDto(55.754167f, 37.62f),
                false,
                10,
                true,
                "Test Event Title"
        );

        CategoryDto categoryDto = new CategoryDto(1L, "Concerts");
        UserShortDto userShortDto = new UserShortDto(1L, "John Doe");

        eventFullDto = new EventFullDto(
                1L,
                "Event annotation more than 20 characters",
                categoryDto,
                50L,
                now.minusDays(1),
                "Full event description more than 20 characters",
                now.plusDays(1),
                userShortDto,
                new LocationDto(55.754167f, 37.62f),
                true,
                100,
                now.minusHours(1),
                true,
                EventState.PENDING,
                "Test Event Title",
                1000L
        );

        updateRequest = new UpdateEventUserRequest();
        updateRequest.setTitle("Updated Event Title");
        updateRequest.setAnnotation("Updated annotation more than 20 characters");
        updateRequest.setDescription("Updated description more than 20 characters");
        updateRequest.setEventDate(now.plusDays(2));
        updateRequest.setCategory(1L);
        updateRequest.setPaid(true);
        updateRequest.setParticipantLimit(200);
        updateRequest.setRequestModeration(false);
        updateRequest.setStateAction("SEND_TO_REVIEW");
    }

    @Test
    void createEvent_ValidRequest_ReturnsCreated() throws Exception {
        when(eventService.createEvent(eq(1L), any(NewEventDto.class))).thenReturn(eventFullDto);

        mockMvc.perform(post("/users/{userId}/events", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newEventDto)))
                .andExpect(status().isCreated());

        verify(eventService, times(1)).createEvent(eq(1L), any(NewEventDto.class));
    }

    @Test
    void updateEvent_ValidRequest_ReturnsOk() throws Exception {
        when(eventService.updateEventByUser(eq(1L), eq(10L), any(UpdateEventUserRequest.class)))
                .thenReturn(eventFullDto);

        mockMvc.perform(patch("/users/{userId}/events/{eventId}", 1L, 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());

        verify(eventService, times(1)).updateEventByUser(eq(1L), eq(10L), any(UpdateEventUserRequest.class));
    }
}