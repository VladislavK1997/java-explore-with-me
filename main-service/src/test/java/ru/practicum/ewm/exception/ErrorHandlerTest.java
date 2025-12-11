package ru.practicum.ewm.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.controller.admin.AdminUserController;
import ru.practicum.ewm.dto.NewUserRequest;
import ru.practicum.ewm.service.UserService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminUserController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ErrorHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void handleNotFoundException_shouldReturnNotFound() throws Exception {
        when(userService.createUser(any(NewUserRequest.class)))
                .thenThrow(new NotFoundException("User not found"));

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"John Doe\",\"email\":\"john@example.com\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void handleValidationException_shouldReturnBadRequest() throws Exception {
        when(userService.createUser(any(NewUserRequest.class)))
                .thenThrow(new ValidationException("Invalid data"));

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"John Doe\",\"email\":\"john@example.com\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void handleConflictException_shouldReturnConflict() throws Exception {
        when(userService.createUser(any(NewUserRequest.class)))
                .thenThrow(new ConflictException("Duplicate entry"));

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"John Doe\",\"email\":\"john@example.com\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void handleDataIntegrityViolationException_shouldReturnConflict() throws Exception {
        when(userService.createUser(any(NewUserRequest.class)))
                .thenThrow(new DataIntegrityViolationException("Constraint violation"));

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"John Doe\",\"email\":\"john@example.com\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void handleMethodArgumentTypeMismatchException_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(delete("/admin/users/{userId}", "invalid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void handleInvalidRequestBody_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"email\":\"invalid-email\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void handleGenericException_shouldReturnInternalServerError() throws Exception {
        when(userService.createUser(any(NewUserRequest.class)))
                .thenThrow(new RuntimeException("Unexpected error"));

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"John Doe\",\"email\":\"john@example.com\"}"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void handleMissingServletRequestParameterException_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isBadRequest());
    }
}