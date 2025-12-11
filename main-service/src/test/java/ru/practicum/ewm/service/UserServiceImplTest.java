package ru.practicum.ewm.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.ewm.dto.NewUserRequest;
import ru.practicum.ewm.dto.UserDto;
import ru.practicum.ewm.mapper.UserMapper;
import ru.practicum.ewm.model.User;
import ru.practicum.ewm.repository.UserRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void createUser_ValidData_ReturnsUserDto() {
        NewUserRequest request = new NewUserRequest("John Doe", "john@example.com");
        User user = UserMapper.toUser(request);
        user.setId(1L);

        when(userRepository.save(any(User.class))).thenReturn(user);

        UserDto result = userService.createUser(request);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("John Doe", result.getName());
        assertEquals("john@example.com", result.getEmail());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void getUsers_WithIds_ReturnsFilteredUsers() {
        List<Long> ids = List.of(1L, 2L);
        User user1 = User.builder().id(1L).name("User1").email("user1@example.com").build();
        User user2 = User.builder().id(2L).name("User2").email("user2@example.com").build();

        when(userRepository.findByIdIn(ids, PageRequest.of(0, 10))).thenReturn(List.of(user1, user2));

        List<UserDto> result = userService.getUsers(ids, 0, 10);

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(2L, result.get(1).getId());
        verify(userRepository, times(1)).findByIdIn(ids, PageRequest.of(0, 10));
    }

    @Test
    void getUsers_WithoutIds_ReturnsAllUsers() {
        User user1 = User.builder().id(1L).name("User1").email("user1@example.com").build();
        User user2 = User.builder().id(2L).name("User2").email("user2@example.com").build();
        Page<User> page = new PageImpl<>(List.of(user1, user2));

        when(userRepository.findAll(PageRequest.of(0, 10))).thenReturn(page);

        List<UserDto> result = userService.getUsers(null, 0, 10);

        assertEquals(2, result.size());
        verify(userRepository, times(1)).findAll(PageRequest.of(0, 10));
        verify(userRepository, never()).findByIdIn(any(), any());
    }

    @Test
    void getUsers_WithPagination_ReturnsCorrectPage() {
        User user3 = User.builder().id(3L).name("User3").email("user3@example.com").build();
        User user4 = User.builder().id(4L).name("User4").email("user4@example.com").build();
        Page<User> page = new PageImpl<>(List.of(user3, user4));

        when(userRepository.findAll(PageRequest.of(1, 2))).thenReturn(page);

        List<UserDto> result = userService.getUsers(null, 2, 2);

        assertEquals(2, result.size());
        assertEquals(3L, result.get(0).getId());
        assertEquals(4L, result.get(1).getId());
    }

    @Test
    void deleteUser_ExistingUser_DeletesSuccessfully() {
        Long userId = 1L;

        userService.deleteUser(userId);

        verify(userRepository, times(1)).deleteById(userId);
    }

    @Test
    void deleteUser_NonExistingUser_DeletesWithoutError() {
        Long userId = 999L;

        userService.deleteUser(userId);

        verify(userRepository, times(1)).deleteById(userId);
    }

    @Test
    void getUsers_EmptyResult_ReturnsEmptyList() {
        Page<User> emptyPage = new PageImpl<>(List.of());
        when(userRepository.findAll(PageRequest.of(0, 10))).thenReturn(emptyPage);

        List<UserDto> result = userService.getUsers(null, 0, 10);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void createUser_DuplicateEmail_ShouldNotThrowException() {
        NewUserRequest request = new NewUserRequest("John Doe", "john@example.com");
        User user = UserMapper.toUser(request);
        user.setId(1L);

        when(userRepository.save(any(User.class))).thenReturn(user);

        assertDoesNotThrow(() -> userService.createUser(request));
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void getUsers_WithEmptyIdsList_ReturnsAllUsers() {
        List<Long> emptyIds = List.of();
        User user1 = User.builder().id(1L).name("User1").email("user1@example.com").build();
        Page<User> page = new PageImpl<>(List.of(user1));

        when(userRepository.findAll(PageRequest.of(0, 10))).thenReturn(page);

        List<UserDto> result = userService.getUsers(emptyIds, 0, 10);

        assertEquals(1, result.size());
        verify(userRepository, times(1)).findAll(PageRequest.of(0, 10));
        verify(userRepository, never()).findByIdIn(any(), any());
    }
}