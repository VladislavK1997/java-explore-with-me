package ru.practicum.ewm.exception;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.config.StatsClientConfig;
import ru.practicum.ewm.controller.admin.AdminEventController;
import ru.practicum.ewm.dto.NewUserRequest;
import ru.practicum.ewm.service.EventService;
import ru.practicum.ewm.service.UserService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import({StatsClientConfig.class, ErrorHandler.class})
@WebMvcTest(controllers = {AdminEventController.class})
class ErrorHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventService eventService;

    @MockBean
    private UserService userService;

    @Test
    void handleNotFoundException_shouldReturnNotFound() throws Exception {
        when(userService.createUser(any(NewUserRequest.class)))
                .thenThrow(new NotFoundException("User not found"));

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"John Doe\",\"email\":\"john@example.com\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"));
    }

    @Test
    void handleValidationException_shouldReturnBadRequest() throws Exception {
        when(userService.createUser(any(NewUserRequest.class)))
                .thenThrow(new ValidationException("Invalid data"));

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"John Doe\",\"email\":\"john@example.com\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));
    }

    @Test
    void handleConflictException_shouldReturnConflict() throws Exception {
        when(userService.createUser(any(NewUserRequest.class)))
                .thenThrow(new ConflictException("Duplicate entry"));

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"John Doe\",\"email\":\"john@example.com\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("CONFLICT"));
    }

    @Test
    void handleDataIntegrityViolationException_shouldReturnConflict() throws Exception {
        when(userService.createUser(any(NewUserRequest.class)))
                .thenThrow(new DataIntegrityViolationException("Constraint violation"));

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"John Doe\",\"email\":\"john@example.com\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("CONFLICT"));
    }

    @Test
    void handleMethodArgumentTypeMismatchException_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(delete("/admin/users/{userId}", "invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));
    }

    @Test
    void handleInvalidRequestBody_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"email\":\"invalid-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));
    }

    @Test
    void handleMissingServletRequestParameterException_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/admin/users")
                        .param("from", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));
    }

    @Test
    void handleDateTimeParseException_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/admin/events")
                        .param("rangeStart", "invalid-date"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));
    }

    @Test
    void handleGenericException_shouldReturnInternalServerError() throws Exception {
        when(userService.createUser(any(NewUserRequest.class)))
                .thenThrow(new RuntimeException("Unexpected error"));

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"John Doe\",\"email\":\"john@example.com\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("INTERNAL_SERVER_ERROR"));
    }

    @Test
    void handleInvalidPaginationParameters_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/admin/users")
                        .param("from", "-1")
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));
    }

    @Test
    void handleInvalidEventState_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/admin/events")
                        .param("states", "INVALID_STATE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));
    }
}