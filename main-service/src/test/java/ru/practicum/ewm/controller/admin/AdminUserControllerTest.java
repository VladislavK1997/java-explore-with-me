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
import ru.practicum.ewm.dto.*;
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
    void createUser_ValidRequest_ReturnsCreated() throws Exception {
        when(userService.createUser(any(NewUserRequest.class))).thenReturn(userDto);

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUserRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"));

        verify(userService, times(1)).createUser(any(NewUserRequest.class));
    }

    @Test
    void createUser_InvalidRequest_ReturnsBadRequest() throws Exception {
        NewUserRequest invalidRequest = new NewUserRequest("", "invalid-email");

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).createUser(any());
    }

    @Test
    void getUsers_WithIds_ReturnsUsers() throws Exception {
        List<UserDto> users = List.of(
                new UserDto(1L, "User1", "user1@example.com"),
                new UserDto(2L, "User2", "user2@example.com")
        );

        when(userService.getUsers(anyList(), eq(0), eq(10))).thenReturn(users);

        mockMvc.perform(get("/admin/users")
                        .param("ids", "1,2")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[1].id").value(2L));

        verify(userService, times(1)).getUsers(List.of(1L, 2L), 0, 10);
    }

    @Test
    void getUsers_WithoutParams_ReturnsUsers() throws Exception {
        List<UserDto> users = List.of(userDto);

        when(userService.getUsers(isNull(), eq(0), eq(10))).thenReturn(users);

        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(userService, times(1)).getUsers(isNull(), eq(0), eq(10));
    }

    @Test
    void getUsers_WithoutIds_ReturnsAllUsers() throws Exception {
        List<UserDto> users = List.of(
                new UserDto(1L, "User1", "user1@example.com")
        );

        when(userService.getUsers(isNull(), eq(0), eq(10))).thenReturn(users);

        mockMvc.perform(get("/admin/users")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(userService, times(1)).getUsers(isNull(), eq(0), eq(10));
    }

    @Test
    void deleteUser_ValidId_ReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/admin/users/{userId}", 1L))
                .andExpect(status().isNoContent());

        verify(userService, times(1)).deleteUser(1L);
    }

    @Test
    void deleteUser_InvalidPathParam_ReturnsBadRequest() throws Exception {
        mockMvc.perform(delete("/admin/users/{userId}", "invalid"))
                .andExpect(status().isBadRequest());

        verify(userService, never()).deleteUser(any());
    }
}