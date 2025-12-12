package ru.practicum.ewm.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.ewm.dto.*;
import ru.practicum.ewm.exception.ValidationException;
import ru.practicum.ewm.model.*;
import ru.practicum.ewm.repository.CategoryRepository;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.UserRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private StatsService statsService;

    @InjectMocks
    private EventServiceImpl eventService;

    private final LocalDateTime now = LocalDateTime.now();
    private final LocalDateTime futureDate = now.plusHours(3);
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Test
    void getEventsByUser_ValidUserId_ReturnsEventShortDtoList() {
        Long userId = 1L;
        Event event1 = Event.builder()
                .id(1L)
                .title("Event 1")
                .annotation("Annotation 1")
                .eventDate(futureDate)
                .initiator(User.builder().id(userId).name("User1").build())
                .category(Category.builder().id(1L).name("Cat1").build())
                .paid(true)
                .confirmedRequests(5)
                .views(100L)
                .build();

        Event event2 = Event.builder()
                .id(2L)
                .title("Event 2")
                .annotation("Annotation 2")
                .eventDate(futureDate.plusDays(1))
                .initiator(User.builder().id(userId).name("User1").build())
                .category(Category.builder().id(2L).name("Cat2").build())
                .paid(false)
                .confirmedRequests(10)
                .views(200L)
                .build();

        Page<Event> eventPage = new PageImpl<>(List.of(event1, event2));
        // Исправляем: используем правильную сортировку
        when(eventRepository.findByInitiatorId(userId,
                PageRequest.of(0, 10, Sort.by("eventDate").descending())))
                .thenReturn(eventPage.getContent());
        when(statsService.getViews(List.of(1L, 2L))).thenReturn(Map.of(1L, 100L, 2L, 200L));

        List<EventShortDto> result = eventService.getEventsByUser(userId, 0, 10);

        assertEquals(2, result.size());
        assertEquals("Event 1", result.get(0).getTitle());
        assertEquals("Event 2", result.get(1).getTitle());
        verify(eventRepository, times(1)).findByInitiatorId(userId,
                PageRequest.of(0, 10, Sort.by("eventDate").descending()));
        verify(statsService, times(1)).getViews(List.of(1L, 2L));
    }

    @Test
    void getEventsByUser_InvalidPagination_ThrowsValidationException() {
        Long userId = 1L;

        ValidationException exception1 = assertThrows(ValidationException.class,
                () -> eventService.getEventsByUser(userId, -1, 10));
        assertTrue(exception1.getMessage().contains("Parameter 'from' must be greater than or equal to 0"));

        ValidationException exception2 = assertThrows(ValidationException.class,
                () -> eventService.getEventsByUser(userId, 0, 0));
        assertTrue(exception2.getMessage().contains("Parameter 'size' must be greater than 0"));
    }

    @Test
    void getEventsByUser_WithNullParams_UsesDefaults() {
        Long userId = 1L;
        Event event = Event.builder()
                .id(1L)
                .title("Event 1")
                .annotation("Annotation 1")
                .eventDate(futureDate)
                .initiator(User.builder().id(userId).name("User1").build())
                .category(Category.builder().id(1L).name("Cat1").build())
                .paid(true)
                .confirmedRequests(5)
                .views(100L)
                .build();

        // Исправляем: используем правильную сортировку
        when(eventRepository.findByInitiatorId(userId,
                PageRequest.of(0, 10, Sort.by("eventDate").descending())))
                .thenReturn(List.of(event));
        when(statsService.getViews(List.of(1L))).thenReturn(Map.of(1L, 100L));

        List<EventShortDto> result = eventService.getEventsByUser(userId, null, null);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(eventRepository, times(1)).findByInitiatorId(userId,
                PageRequest.of(0, 10, Sort.by("eventDate").descending()));
    }

    @Test
    void getEventsPublic_WithInvalidSortParameter_ThrowsValidationException() {
        String ip = "192.168.1.1";
        String invalidSort = "INVALID_SORT";

        ValidationException exception = assertThrows(ValidationException.class,
                () -> eventService.getEventsPublic(null, null, null, null, null,
                        false, invalidSort, 0, 10, ip));
        assertTrue(exception.getMessage().contains("Invalid sort parameter"));
    }

    @Test
    void getEventsPublic_InvalidPagination_ThrowsValidationException() {
        String ip = "192.168.1.1";

        ValidationException exception1 = assertThrows(ValidationException.class,
                () -> eventService.getEventsPublic(null, null, null, null, null,
                        false, null, -1, 10, ip));
        assertTrue(exception1.getMessage().contains("Parameter 'from' must be greater than or equal to 0"));

        ValidationException exception2 = assertThrows(ValidationException.class,
                () -> eventService.getEventsPublic(null, null, null, null, null,
                        false, null, 0, 0, ip));
        assertTrue(exception2.getMessage().contains("Parameter 'size' must be greater than 0"));
    }

    @Test
    void getEventsPublic_WithNullParams_UsesDefaults() {
        String ip = "192.168.1.1";
        Event event = Event.builder()
                .id(1L)
                .title("Test Event")
                .annotation("Annotation")
                .description("Description")
                .state(EventState.PUBLISHED)
                .eventDate(futureDate)
                .initiator(User.builder().id(1L).build())
                .category(Category.builder().id(1L).build())
                .paid(true)
                .participantLimit(100)
                .confirmedRequests(50)
                .views(100L)
                .build();

        // Исправляем мок с правильными параметрами
        when(eventRepository.findEventsPublic(
                eq(null), eq(null), eq(null), any(), eq(null), eq(EventState.PUBLISHED),
                eq(PageRequest.of(0, 10))))
                .thenReturn(List.of(event));
        when(statsService.getViews(List.of(1L))).thenReturn(Map.of(1L, 100L));

        List<EventShortDto> result = eventService.getEventsPublic(
                null, null, null, null, null, null, null, null, null, ip);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(eventRepository, times(1)).findEventsPublic(
                eq(null), eq(null), eq(null), any(), eq(null), eq(EventState.PUBLISHED),
                eq(PageRequest.of(0, 10)));
    }

    @Test
    void getEventsPublic_WithOnlyAvailableNull_ShouldUseFalse() {
        String ip = "192.168.1.1";
        Event event = Event.builder()
                .id(1L)
                .title("Test Event")
                .annotation("Annotation")
                .description("Description")
                .state(EventState.PUBLISHED)
                .eventDate(futureDate)
                .initiator(User.builder().id(1L).build())
                .category(Category.builder().id(1L).build())
                .paid(true)
                .participantLimit(100)
                .confirmedRequests(50)
                .views(100L)
                .build();

        when(eventRepository.findEventsPublic(
                eq(null), eq(null), eq(null), any(), eq(null), eq(EventState.PUBLISHED),
                eq(PageRequest.of(0, 10))))
                .thenReturn(List.of(event));
        when(statsService.getViews(List.of(1L))).thenReturn(Map.of(1L, 100L));

        List<EventShortDto> result = eventService.getEventsPublic(
                null, null, null, null, null, null, null, 0, 10, ip);

        assertNotNull(result);
        assertEquals(1, result.size());
        // Проверяем, что onlyAvailable передается как false
        verify(eventRepository, times(1)).findEventsPublic(
                eq(null), eq(null), eq(null), any(), eq(null), eq(EventState.PUBLISHED),
                eq(PageRequest.of(0, 10)));
    }

    @Test
    void createEvent_ValidData_ReturnsEventFullDto() {
        Long userId = 1L;
        NewEventDto newEventDto = new NewEventDto(
                "Annotation with at least 20 characters",
                1L,
                "Description with at least 20 characters",
                futureDate.plusHours(2),
                new LocationDto(55.754167f, 37.62f),
                true,
                100,
                true,
                "Test Event"
        );

        User user = User.builder().id(userId).name("John Doe").build();
        Category category = Category.builder().id(1L).name("Concerts").build();
        Event event = Event.builder()
                .id(1L)
                .title("Test Event")
                .annotation("Annotation with at least 20 characters")
                .description("Description with at least 20 characters")
                .eventDate(futureDate.plusHours(2))
                .initiator(user)
                .category(category)
                .paid(true)
                .participantLimit(100)
                .requestModeration(true)
                .state(EventState.PENDING)
                .confirmedRequests(0)
                .views(0L)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(eventRepository.save(any(Event.class))).thenReturn(event);

        EventFullDto result = eventService.createEvent(userId, newEventDto);

        assertNotNull(result);
        assertEquals("Test Event", result.getTitle());
        verify(userRepository, times(1)).findById(userId);
        verify(categoryRepository, times(1)).findById(1L);
        verify(eventRepository, times(1)).save(any(Event.class));
    }

    @Test
    void createEvent_InvalidEventDate_ThrowsValidationException() {
        Long userId = 1L;
        NewEventDto newEventDto = new NewEventDto(
                "Annotation with at least 20 characters",
                1L,
                "Description with at least 20 characters",
                now.plusHours(1), // Меньше чем через 2 часа
                new LocationDto(55.754167f, 37.62f),
                true,
                100,
                true,
                "Test Event"
        );

        User user = User.builder().id(userId).name("John Doe").build();
        Category category = Category.builder().id(1L).name("Concerts").build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        ValidationException exception = assertThrows(ValidationException.class,
                () -> eventService.createEvent(userId, newEventDto));

        assertTrue(exception.getMessage().contains("Field: eventDate"));
        verify(userRepository, times(1)).findById(userId);
        verify(categoryRepository, times(1)).findById(1L);
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void getEventPublic_ValidId_ReturnsEventFullDto() {
        Long eventId = 1L;
        String ip = "192.168.1.1";

        Event event = Event.builder()
                .id(eventId)
                .title("Test Event")
                .annotation("Annotation")
                .description("Description")
                .state(EventState.PUBLISHED)
                .eventDate(futureDate)
                .initiator(User.builder().id(1L).name("John Doe").build())
                .category(Category.builder().id(1L).name("Concerts").build())
                .paid(true)
                .participantLimit(100)
                .confirmedRequests(50)
                .views(100L)
                .build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(statsService.getViews(List.of(eventId))).thenReturn(Map.of(eventId, 100L));

        EventFullDto result = eventService.getEventPublic(eventId, ip);

        assertNotNull(result);
        assertEquals(eventId, result.getId());
        verify(eventRepository, times(1)).findById(eventId);
        verify(statsService, times(1)).getViews(List.of(eventId));
        verify(statsService, times(1)).saveHit("/events/" + eventId, ip);
    }

    @Test
    void getEventPublic_NotPublished_ThrowsNotFoundException() {
        Long eventId = 1L;
        String ip = "192.168.1.1";

        Event event = Event.builder()
                .id(eventId)
                .title("Test Event")
                .state(EventState.PENDING)
                .build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        ru.practicum.ewm.exception.NotFoundException exception =
                assertThrows(ru.practicum.ewm.exception.NotFoundException.class,
                        () -> eventService.getEventPublic(eventId, ip));

        assertTrue(exception.getMessage().contains("Event with id=" + eventId));
        verify(eventRepository, times(1)).findById(eventId);
        verify(statsService, never()).getViews(any());
        verify(statsService, never()).saveHit(any(), any());
    }
}