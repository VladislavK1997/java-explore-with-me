package ru.practicum.ewm.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
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
                "Event annotation more than 20 characters",
                categoryDto,
                50L,
                now.minusDays(1),
                "Full event description more than 20 characters",
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
        updateRequest.setTitle("Updated Event Title");
        updateRequest.setAnnotation("Updated annotation more than 20 characters");
        updateRequest.setDescription("Updated description more than 20 characters");
        updateRequest.setEventDate(now.plusDays(2));
        updateRequest.setCategory(1L);
        updateRequest.setPaid(true);
        updateRequest.setParticipantLimit(200);
        updateRequest.setRequestModeration(false);
        updateRequest.setStateAction("PUBLISH_EVENT");
    }

    @Test
    void updateEvent_ValidRequest_ReturnsOk() throws Exception {
        when(eventService.updateEventByAdmin(eq(1L), any(UpdateEventAdminRequest.class)))
                .thenReturn(eventFullDto);

        mockMvc.perform(patch("/admin/events/{eventId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(eventService, times(1)).updateEventByAdmin(eq(1L), any(UpdateEventAdminRequest.class));
    }
}