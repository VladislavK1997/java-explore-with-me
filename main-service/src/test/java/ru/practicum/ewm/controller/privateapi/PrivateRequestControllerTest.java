package ru.practicum.ewm.controller.privateapi;

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
import ru.practicum.ewm.dto.ParticipationRequestDto;
import ru.practicum.ewm.service.RequestService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(StatsClientConfig.class)
@WebMvcTest(PrivateRequestController.class)
class PrivateRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RequestService requestService;

    private ObjectMapper objectMapper;
    private ParticipationRequestDto requestDto;
    private final LocalDateTime now = LocalDateTime.now();

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        requestDto = new ParticipationRequestDto(
                now,
                10L,
                1L,
                2L,
                "PENDING"
        );
    }

    @Test
    void getRequests_ValidUserId_ReturnsRequests() throws Exception {
        List<ParticipationRequestDto> requests = List.of(requestDto);

        when(requestService.getRequestsByUser(1L)).thenReturn(requests);

        mockMvc.perform(get("/users/{userId}/requests", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].event").value(10L))
                .andExpect(jsonPath("$[0].requester").value(2L));

        verify(requestService, times(1)).getRequestsByUser(1L);
    }

    @Test
    void createRequest_ValidIds_ReturnsCreated() throws Exception {
        when(requestService.createRequest(eq(1L), eq(10L))).thenReturn(requestDto);

        mockMvc.perform(post("/users/{userId}/requests", 1L)
                        .param("eventId", "10"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.event").value(10L))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(requestService, times(1)).createRequest(1L, 10L);
    }

    @Test
    void createRequest_InvalidPathParam_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/users/{userId}/requests", "invalid")
                        .param("eventId", "10"))
                .andExpect(status().isBadRequest());

        verify(requestService, never()).createRequest(any(), any());
    }

    @Test
    void createRequest_InvalidEventIdParam_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/users/{userId}/requests", 1L)
                        .param("eventId", "invalid"))
                .andExpect(status().isBadRequest());

        verify(requestService, never()).createRequest(any(), any());
    }

    @Test
    void cancelRequest_ValidIds_ReturnsOk() throws Exception {
        ParticipationRequestDto canceledRequest = new ParticipationRequestDto(
                now,
                10L,
                1L,
                2L,
                "CANCELED"
        );

        when(requestService.cancelRequest(1L, 100L)).thenReturn(canceledRequest);

        mockMvc.perform(patch("/users/{userId}/requests/{requestId}/cancel", 1L, 100L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value("CANCELED"));

        verify(requestService, times(1)).cancelRequest(1L, 100L);
    }

    @Test
    void cancelRequest_InvalidPathParams_ReturnsBadRequest() throws Exception {
        mockMvc.perform(patch("/users/{userId}/requests/{requestId}/cancel", "invalid", "invalid"))
                .andExpect(status().isBadRequest());

        verify(requestService, never()).cancelRequest(any(), any());
    }

    @Test
    void getRequests_UserNotFound_ReturnsNotFound() throws Exception {
        when(requestService.getRequestsByUser(999L))
                .thenThrow(new ru.practicum.ewm.exception.NotFoundException("User not found"));

        mockMvc.perform(get("/users/{userId}/requests", 999L))
                .andExpect(status().isNotFound());

        verify(requestService, times(1)).getRequestsByUser(999L);
    }

    @Test
    void createRequest_MissingEventIdParam_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/users/{userId}/requests", 1L))
                .andExpect(status().isBadRequest());

        verify(requestService, never()).createRequest(any(), any());
    }
}