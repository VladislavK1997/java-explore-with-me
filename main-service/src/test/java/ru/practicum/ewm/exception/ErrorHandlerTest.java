package ru.practicum.ewm.exception;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.controller.admin.AdminUserController;
import ru.practicum.ewm.dto.NewUserRequest;
import ru.practicum.ewm.service.UserService;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminUserController.class)
class ErrorHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    void handleNotFoundException_shouldReturnNotFound() throws Exception {
        when(userService.createUser(any(NewUserRequest.class)))
                .thenThrow(new NotFoundException("User not found"));

        NewUserRequest request = new NewUserRequest("John Doe", "john@example.com");

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"John Doe\",\"email\":\"john@example.com\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                .andExpect(jsonPath("$.reason").value("The required object was not found."))
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    @Test
    void handleValidationException_shouldReturnBadRequest() throws Exception {
        when(userService.createUser(any(NewUserRequest.class)))
                .thenThrow(new ValidationException("Invalid data"));

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"John Doe\",\"email\":\"john@example.com\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.reason").value("Incorrectly made request."))
                .andExpect(jsonPath("$.message").value("Invalid data"));
    }

    @Test
    void handleConflictException_shouldReturnConflict() throws Exception {
        when(userService.createUser(any(NewUserRequest.class)))
                .thenThrow(new ConflictException("Duplicate entry"));

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"John Doe\",\"email\":\"john@example.com\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("CONFLICT"))
                .andExpect(jsonPath("$.reason").value("For the requested operation the conditions are not met."))
                .andExpect(jsonPath("$.message").value("Duplicate entry"));
    }

    @Test
    void handleDataIntegrityViolationException_shouldReturnConflict() throws Exception {
        when(userService.createUser(any(NewUserRequest.class)))
                .thenThrow(new DataIntegrityViolationException("Constraint violation"));

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"John Doe\",\"email\":\"john@example.com\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("CONFLICT"))
                .andExpect(jsonPath("$.reason").value("Integrity constraint has been violated."))
                .andExpect(jsonPath("$.message").value("Integrity constraint has been violated."));
    }

    @Test
    void handleMethodArgumentTypeMismatchException_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(delete("/admin/users/{userId}", "invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.reason").value("Incorrectly made request."));
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
        // Запрос без обязательного параметра
        mockMvc.perform(post("/users/{userId}/requests", 1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));
    }

    @Test
    void handleDateTimeParseException_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/admin/events")
                        .param("rangeStart", "invalid-date")
                        .param("rangeEnd", "invalid-date"))
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
                .andExpect(jsonPath("$.status").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.reason").value("Internal server error"));
    }

    @Test
    void handleInvalidPaginationParameters_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/categories")
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