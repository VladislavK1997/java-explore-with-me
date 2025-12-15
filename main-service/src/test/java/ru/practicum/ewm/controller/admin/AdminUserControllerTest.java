package ru.practicum.ewm.controller.admin;

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
import ru.practicum.ewm.dto.NewUserRequest;
import ru.practicum.ewm.dto.UserDto;
import ru.practicum.ewm.service.UserService;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminUserController.class)
@Import(StatsClientConfig.class)
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    private ObjectMapper objectMapper;
    private NewUserRequest newUserRequest;
    private UserDto userDto;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        newUserRequest = new NewUserRequest("John Doe", "john@example.com");
        userDto = new UserDto(1L, "John Doe", "john@example.com");
    }

    @Test
    void getUsers_WithInvalidFromParam_ShouldReturnBadRequest() throws Exception {
        // Контроллер сам проверяет @Min(0) для from
        mockMvc.perform(get("/admin/users")
                        .param("from", "-1")
                        .param("size", "10"))
                .andExpect(status().isBadRequest());

        verify(userService, never()).getUsers(any(), anyInt(), anyInt());
    }

    @Test
    void getUsers_WithInvalidSizeParam_ShouldReturnBadRequest() throws Exception {
        // Контроллер сам проверяет @Min(1) для size
        mockMvc.perform(get("/admin/users")
                        .param("from", "0")
                        .param("size", "0"))
                .andExpect(status().isBadRequest());

        verify(userService, never()).getUsers(any(), anyInt(), anyInt());
    }
}