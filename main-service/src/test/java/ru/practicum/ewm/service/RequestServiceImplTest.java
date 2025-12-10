package ru.practicum.ewm.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.dto.EventRequestStatusUpdateResult;
import ru.practicum.ewm.dto.ParticipationRequestDto;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.ParticipationRequestMapper;
import ru.practicum.ewm.model.*;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.ParticipationRequestRepository;
import ru.practicum.ewm.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestServiceImplTest {

    @Mock
    private ParticipationRequestRepository requestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private RequestServiceImpl requestService;

    private final LocalDateTime now = LocalDateTime.now();

    @Test
    void getRequestsByUser_ValidUserId_ReturnsRequests() {
        // Given
        Long userId = 1L;

        User user = User.builder().id(userId).name("John Doe").build();
        Event event = Event.builder().id(10L).title("Test Event").build();

        ParticipationRequest request1 = ParticipationRequest.builder()
                .id(1L)
                .event(event)
                .requester(user)
                .status(RequestStatus.PENDING)
                .created(now.minusDays(1))
                .build();

        ParticipationRequest request2 = ParticipationRequest.builder()
                .id(2L)
                .event(event)
                .requester(user)
                .status(RequestStatus.CONFIRMED)
                .created(now)
                .build();

        when(userRepository.existsById(userId)).thenReturn(true);
        when(requestRepository.findByRequesterId(userId)).thenReturn(List.of(request1, request2));

        // When
        List<ParticipationRequestDto> result = requestService.getRequestsByUser(userId);

        // Then
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(2L, result.get(1).getId());
        verify(userRepository, times(1)).existsById(userId);
        verify(requestRepository, times(1)).findByRequesterId(userId);
    }

    @Test
    void getRequestsByUser_UserNotFound_ThrowsNotFoundException() {
        // Given
        Long userId = 999L;

        when(userRepository.existsById(userId)).thenReturn(false);

        // When & Then
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> requestService.getRequestsByUser(userId));
        assertEquals("User with id=999 was not found", exception.getMessage());
        verify(userRepository, times(1)).existsById(userId);
        verify(requestRepository, never()).findByRequesterId(any());
    }

    @Test
    void createRequest_ValidData_ReturnsRequestDto() {
        // Given
        Long userId = 1L;
        Long eventId = 10L;

        User user = User.builder().id(userId).name("John Doe").email("john@example.com").build();
        User initiator = User.builder().id(2L).name("Jane Smith").build();

        Event event = Event.builder()
                .id(eventId)
                .title("Test Event")
                .initiator(initiator)
                .state(EventState.PUBLISHED)
                .participantLimit(100)
                .confirmedRequests(50)
                .requestModeration(true)
                .build();

        ParticipationRequest request = ParticipationRequest.builder()
                .id(1L)
                .event(event)
                .requester(user)
                .status(RequestStatus.PENDING)
                .created(now)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(requestRepository.findByEventIdAndRequesterId(eventId, userId)).thenReturn(Optional.empty());
        when(requestRepository.save(any(ParticipationRequest.class))).thenReturn(request);

        // When
        ParticipationRequestDto result = requestService.createRequest(userId, eventId);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(RequestStatus.PENDING.name(), result.getStatus());
        verify(userRepository, times(1)).findById(userId);
        verify(eventRepository, times(1)).findById(eventId);
        verify(requestRepository, times(1)).findByEventIdAndRequesterId(eventId, userId);
        verify(requestRepository, times(1)).save(any(ParticipationRequest.class));
    }

    @Test
    void createRequest_UserIsInitiator_ThrowsConflictException() {
        // Given
        Long userId = 1L;
        Long eventId = 10L;

        User user = User.builder().id(userId).name("John Doe").build();
        Event event = Event.builder()
                .id(eventId)
                .title("Test Event")
                .initiator(user) // Тот же пользователь
                .state(EventState.PUBLISHED)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        // When & Then
        ConflictException exception = assertThrows(ConflictException.class,
                () -> requestService.createRequest(userId, eventId));
        assertEquals("The initiator cannot add a request to participate in his event", exception.getMessage());
        verify(userRepository, times(1)).findById(userId);
        verify(eventRepository, times(1)).findById(eventId);
        verify(requestRepository, never()).save(any());
    }

    @Test
    void createRequest_EventNotPublished_ThrowsConflictException() {
        // Given
        Long userId = 1L;
        Long eventId = 10L;

        User user = User.builder().id(userId).name("John Doe").build();
        User initiator = User.builder().id(2L).name("Jane Smith").build();

        Event event = Event.builder()
                .id(eventId)
                .title("Test Event")
                .initiator(initiator)
                .state(EventState.PENDING) // Не опубликовано
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        // When & Then
        ConflictException exception = assertThrows(ConflictException.class,
                () -> requestService.createRequest(userId, eventId));
        assertEquals("You cannot participate in an unpublished event", exception.getMessage());
        verify(userRepository, times(1)).findById(userId);
        verify(eventRepository, times(1)).findById(eventId);
        verify(requestRepository, never()).save(any());
    }

    @Test
    void createRequest_ParticipantLimitReached_ThrowsConflictException() {
        // Given
        Long userId = 1L;
        Long eventId = 10L;

        User user = User.builder().id(userId).name("John Doe").build();
        User initiator = User.builder().id(2L).name("Jane Smith").build();

        Event event = Event.builder()
                .id(eventId)
                .title("Test Event")
                .initiator(initiator)
                .state(EventState.PUBLISHED)
                .participantLimit(10)
                .confirmedRequests(10) // Лимит достигнут
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        // When & Then
        ConflictException exception = assertThrows(ConflictException.class,
                () -> requestService.createRequest(userId, eventId));
        assertEquals("The participant limit has been reached", exception.getMessage());
        verify(userRepository, times(1)).findById(userId);
        verify(eventRepository, times(1)).findById(eventId);
        verify(requestRepository, never()).save(any());
    }

    @Test
    void createRequest_DuplicateRequest_ThrowsConflictException() {
        // Given
        Long userId = 1L;
        Long eventId = 10L;

        User user = User.builder().id(userId).name("John Doe").build();
        User initiator = User.builder().id(2L).name("Jane Smith").build();

        Event event = Event.builder()
                .id(eventId)
                .title("Test Event")
                .initiator(initiator)
                .state(EventState.PUBLISHED)
                .participantLimit(100)
                .confirmedRequests(50)
                .build();

        ParticipationRequest existingRequest = ParticipationRequest.builder()
                .id(1L)
                .event(event)
                .requester(user)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(requestRepository.findByEventIdAndRequesterId(eventId, userId))
                .thenReturn(Optional.of(existingRequest));

        // When & Then
        ConflictException exception = assertThrows(ConflictException.class,
                () -> requestService.createRequest(userId, eventId));
        assertEquals("You cannot add a repeat request", exception.getMessage());
        verify(userRepository, times(1)).findById(userId);
        verify(eventRepository, times(1)).findById(eventId);
        verify(requestRepository, times(1)).findByEventIdAndRequesterId(eventId, userId);
        verify(requestRepository, never()).save(any());
    }

    @Test
    void createRequest_NoModerationRequired_AutoConfirms() {
        // Given
        Long userId = 1L;
        Long eventId = 10L;

        User user = User.builder().id(userId).name("John Doe").build();
        User initiator = User.builder().id(2L).name("Jane Smith").build();

        Event event = Event.builder()
                .id(eventId)
                .title("Test Event")
                .initiator(initiator)
                .state(EventState.PUBLISHED)
                .participantLimit(0) // Нет лимита, автоматическое подтверждение
                .confirmedRequests(0)
                .requestModeration(false)
                .build();

        ParticipationRequest request = ParticipationRequest.builder()
                .id(1L)
                .event(event)
                .requester(user)
                .status(RequestStatus.CONFIRMED)
                .created(now)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(requestRepository.findByEventIdAndRequesterId(eventId, userId)).thenReturn(Optional.empty());
        when(requestRepository.save(any(ParticipationRequest.class))).thenReturn(request);

        // When
        ParticipationRequestDto result = requestService.createRequest(userId, eventId);

        // Then
        assertNotNull(result);
        assertEquals(RequestStatus.CONFIRMED.name(), result.getStatus());
        verify(eventRepository, times(1)).save(event);
        verify(requestRepository, times(1)).save(any(ParticipationRequest.class));
    }

    @Test
    void cancelRequest_ValidRequest_ReturnsCanceledRequest() {
        // Given
        Long userId = 1L;
        Long requestId = 10L;

        User user = User.builder().id(userId).name("John Doe").build();
        Event event = Event.builder()
                .id(20L)
                .title("Test Event")
                .confirmedRequests(5)
                .build();

        ParticipationRequest request = ParticipationRequest.builder()
                .id(requestId)
                .event(event)
                .requester(user)
                .status(RequestStatus.PENDING)
                .created(now.minusDays(1))
                .build();

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(requestRepository.save(any(ParticipationRequest.class))).thenReturn(request);

        // When
        ParticipationRequestDto result = requestService.cancelRequest(userId, requestId);

        // Then
        assertNotNull(result);
        assertEquals(RequestStatus.CANCELED.name(), result.getStatus());
        verify(requestRepository, times(1)).findById(requestId);
        verify(requestRepository, times(1)).save(any(ParticipationRequest.class));
    }

    @Test
    void cancelRequest_RequestNotFound_ThrowsNotFoundException() {
        // Given
        Long userId = 1L;
        Long requestId = 999L;

        when(requestRepository.findById(requestId)).thenReturn(Optional.empty());

        // When & Then
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> requestService.cancelRequest(userId, requestId));
        assertEquals("Request with id=999 was not found", exception.getMessage());
        verify(requestRepository, times(1)).findById(requestId);
        verify(requestRepository, never()).save(any());
    }

    @Test
    void cancelRequest_NotOwnRequest_ThrowsNotFoundException() {
        // Given
        Long userId = 1L;
        Long requestId = 10L;

        User otherUser = User.builder().id(2L).name("Other User").build();
        Event event = Event.builder().id(20L).title("Test Event").build();

        ParticipationRequest request = ParticipationRequest.builder()
                .id(requestId)
                .event(event)
                .requester(otherUser) // Другой пользователь
                .status(RequestStatus.PENDING)
                .build();

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));

        // When & Then
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> requestService.cancelRequest(userId, requestId));
        assertEquals("Request with id=10 was not found", exception.getMessage());
        verify(requestRepository, times(1)).findById(requestId);
        verify(requestRepository, never()).save(any());
    }

    @Test
    void cancelRequest_ConfirmedRequest_DecreasesConfirmedCount() {
        // Given
        Long userId = 1L;
        Long requestId = 10L;

        User user = User.builder().id(userId).name("John Doe").build();
        Event event = Event.builder()
                .id(20L)
                .title("Test Event")
                .confirmedRequests(5)
                .build();

        ParticipationRequest request = ParticipationRequest.builder()
                .id(requestId)
                .event(event)
                .requester(user)
                .status(RequestStatus.CONFIRMED)
                .created(now.minusDays(1))
                .build();

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(requestRepository.save(any(ParticipationRequest.class))).thenReturn(request);
        when(eventRepository.save(any(Event.class))).thenReturn(event);

        // When
        ParticipationRequestDto result = requestService.cancelRequest(userId, requestId);

        // Then
        assertNotNull(result);
        assertEquals(RequestStatus.CANCELED.name(), result.getStatus());
        verify(eventRepository, times(1)).save(event);
        verify(requestRepository, times(1)).save(any(ParticipationRequest.class));
    }

    @Test
    void getRequestsForEvent_ValidData_ReturnsRequests() {
        // Given
        Long userId = 1L;
        Long eventId = 10L;

        User initiator = User.builder().id(userId).name("John Doe").build();
        Event event = Event.builder()
                .id(eventId)
                .title("Test Event")
                .initiator(initiator)
                .build();

        User requester1 = User.builder().id(2L).name("Jane Smith").build();
        User requester2 = User.builder().id(3L).name("Bob Johnson").build();

        ParticipationRequest request1 = ParticipationRequest.builder()
                .id(1L)
                .event(event)
                .requester(requester1)
                .status(RequestStatus.PENDING)
                .created(now.minusDays(1))
                .build();

        ParticipationRequest request2 = ParticipationRequest.builder()
                .id(2L)
                .event(event)
                .requester(requester2)
                .status(RequestStatus.CONFIRMED)
                .created(now)
                .build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(requestRepository.findByEventId(eventId)).thenReturn(List.of(request1, request2));

        // When
        List<ParticipationRequestDto> result = requestService.getRequestsForEvent(userId, eventId);

        // Then
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(2L, result.get(1).getId());
        verify(eventRepository, times(1)).findById(eventId);
        verify(requestRepository, times(1)).findByEventId(eventId);
    }

    @Test
    void getRequestsForEvent_NotInitiator_ThrowsNotFoundException() {
        // Given
        Long userId = 1L;
        Long eventId = 10L;

        User otherInitiator = User.builder().id(2L).name("Other User").build();
        Event event = Event.builder()
                .id(eventId)
                .title("Test Event")
                .initiator(otherInitiator) // Другой инициатор
                .build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        // When & Then
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> requestService.getRequestsForEvent(userId, eventId));
        assertEquals("Event with id=10 was not found", exception.getMessage());
        verify(eventRepository, times(1)).findById(eventId);
        verify(requestRepository, never()).findByEventId(any());
    }

    @Test
    void updateRequestStatus_ConfirmRequests_Success() {
        // Given
        Long userId = 1L;
        Long eventId = 10L;

        User initiator = User.builder().id(userId).name("John Doe").build();
        Event event = Event.builder()
                .id(eventId)
                .title("Test Event")
                .initiator(initiator)
                .participantLimit(10)
                .confirmedRequests(5)
                .requestModeration(true)
                .build();

        User requester1 = User.builder().id(2L).name("Jane Smith").build();
        User requester2 = User.builder().id(3L).name("Bob Johnson").build();

        ParticipationRequest request1 = ParticipationRequest.builder()
                .id(1L)
                .event(event)
                .requester(requester1)
                .status(RequestStatus.PENDING)
                .created(now.minusDays(1))
                .build();

        ParticipationRequest request2 = ParticipationRequest.builder()
                .id(2L)
                .event(event)
                .requester(requester2)
                .status(RequestStatus.PENDING)
                .created(now)
                .build();

        EventRequestStatusUpdateRequest updateRequest = new EventRequestStatusUpdateRequest();
        updateRequest.setRequestIds(List.of(1L, 2L));
        updateRequest.setStatus("CONFIRMED");

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(requestRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(request1, request2));
        when(requestRepository.saveAll(any())).thenReturn(List.of(request1, request2));
        when(eventRepository.save(any(Event.class))).thenReturn(event);

        // When
        EventRequestStatusUpdateResult result = requestService.updateRequestStatus(userId, eventId, updateRequest);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getConfirmedRequests().size());
        assertEquals(0, result.getRejectedRequests().size());
        assertEquals(7, event.getConfirmedRequests()); // 5 + 2
        verify(eventRepository, times(1)).findById(eventId);
        verify(requestRepository, times(1)).findAllById(List.of(1L, 2L));
        verify(requestRepository, times(1)).saveAll(any());
        verify(eventRepository, times(1)).save(event);
    }

    @Test
    void updateRequestStatus_RejectRequests_Success() {
        // Given
        Long userId = 1L;
        Long eventId = 10L;

        User initiator = User.builder().id(userId).name("John Doe").build();
        Event event = Event.builder()
                .id(eventId)
                .title("Test Event")
                .initiator(initiator)
                .participantLimit(10)
                .confirmedRequests(5)
                .requestModeration(true)
                .build();

        User requester = User.builder().id(2L).name("Jane Smith").build();

        ParticipationRequest request = ParticipationRequest.builder()
                .id(1L)
                .event(event)
                .requester(requester)
                .status(RequestStatus.PENDING)
                .created(now.minusDays(1))
                .build();

        EventRequestStatusUpdateRequest updateRequest = new EventRequestStatusUpdateRequest();
        updateRequest.setRequestIds(List.of(1L));
        updateRequest.setStatus("REJECTED");

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(requestRepository.findAllById(List.of(1L))).thenReturn(List.of(request));
        when(requestRepository.saveAll(any())).thenReturn(List.of(request));

        // When
        EventRequestStatusUpdateResult result = requestService.updateRequestStatus(userId, eventId, updateRequest);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getConfirmedRequests().size());
        assertEquals(1, result.getRejectedRequests().size());
        assertEquals(5, event.getConfirmedRequests()); // Не изменилось
        verify(eventRepository, times(1)).findById(eventId);
        verify(requestRepository, times(1)).findAllById(List.of(1L));
        verify(requestRepository, times(1)).saveAll(any());
        verify(eventRepository, never()).save(any());
    }

    @Test
    void updateRequestStatus_LimitReached_RejectsRemaining() {
        // Given
        Long userId = 1L;
        Long eventId = 10L;

        User initiator = User.builder().id(userId).name("John Doe").build();
        Event event = Event.builder()
                .id(eventId)
                .title("Test Event")
                .initiator(initiator)
                .participantLimit(1) // Лимит 1
                .confirmedRequests(1) // Уже достигнут
                .requestModeration(true)
                .build();

        User requester = User.builder().id(2L).name("Jane Smith").build();

        ParticipationRequest request = ParticipationRequest.builder()
                .id(1L)
                .event(event)
                .requester(requester)
                .status(RequestStatus.PENDING)
                .created(now.minusDays(1))
                .build();

        EventRequestStatusUpdateRequest updateRequest = new EventRequestStatusUpdateRequest();
        updateRequest.setRequestIds(List.of(1L));
        updateRequest.setStatus("CONFIRMED");

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(requestRepository.findAllById(List.of(1L))).thenReturn(List.of(request));

        // When & Then
        ConflictException exception = assertThrows(ConflictException.class,
                () -> requestService.updateRequestStatus(userId, eventId, updateRequest));
        assertEquals("The participant limit has been reached", exception.getMessage());
        verify(eventRepository, times(1)).findById(eventId);
        verify(requestRepository, times(1)).findAllById(List.of(1L));
        verify(requestRepository, never()).saveAll(any());
    }

    @Test
    void updateRequestStatus_RequestNotPending_ThrowsConflictException() {
        // Given
        Long userId = 1L;
        Long eventId = 10L;

        User initiator = User.builder().id(userId).name("John Doe").build();
        Event event = Event.builder()
                .id(eventId)
                .title("Test Event")
                .initiator(initiator)
                .participantLimit(10)
                .confirmedRequests(5)
                .requestModeration(true)
                .build();

        User requester = User.builder().id(2L).name("Jane Smith").build();

        ParticipationRequest request = ParticipationRequest.builder()
                .id(1L)
                .event(event)
                .requester(requester)
                .status(RequestStatus.CONFIRMED) // Уже подтвержден
                .created(now.minusDays(1))
                .build();

        EventRequestStatusUpdateRequest updateRequest = new EventRequestStatusUpdateRequest();
        updateRequest.setRequestIds(List.of(1L));
        updateRequest.setStatus("REJECTED");

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(requestRepository.findAllById(List.of(1L))).thenReturn(List.of(request));

        // When & Then
        ConflictException exception = assertThrows(ConflictException.class,
                () -> requestService.updateRequestStatus(userId, eventId, updateRequest));
        assertEquals("Request must have status PENDING", exception.getMessage());
        verify(eventRepository, times(1)).findById(eventId);
        verify(requestRepository, times(1)).findAllById(List.of(1L));
        verify(requestRepository, never()).saveAll(any());
    }

    @Test
    void updateRequestStatus_NoModerationRequired_ThrowsConflictException() {
        // Given
        Long userId = 1L;
        Long eventId = 10L;

        User initiator = User.builder().id(userId).name("John Doe").build();
        Event event = Event.builder()
                .id(eventId)
                .title("Test Event")
                .initiator(initiator)
                .participantLimit(0) // Нет лимита - не требуется модерация
                .requestModeration(false)
                .build();

        EventRequestStatusUpdateRequest updateRequest = new EventRequestStatusUpdateRequest();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        // When & Then
        ConflictException exception = assertThrows(ConflictException.class,
                () -> requestService.updateRequestStatus(userId, eventId, updateRequest));
        assertEquals("Event does not require moderation", exception.getMessage());
        verify(eventRepository, times(1)).findById(eventId);
        verify(requestRepository, never()).findAllById(any());
    }

    @Test
    void getRequestsByUser_NoRequests_ReturnsEmptyList() {
        // Given
        Long userId = 1L;

        when(userRepository.existsById(userId)).thenReturn(true);
        when(requestRepository.findByRequesterId(userId)).thenReturn(List.of());

        // When
        List<ParticipationRequestDto> result = requestService.getRequestsByUser(userId);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userRepository, times(1)).existsById(userId);
        verify(requestRepository, times(1)).findByRequesterId(userId);
    }
}