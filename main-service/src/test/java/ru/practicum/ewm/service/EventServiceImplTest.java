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
import ru.practicum.ewm.dto.*;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.exception.ValidationException;
import ru.practicum.ewm.mapper.EventMapper;
import ru.practicum.ewm.model.*;
import ru.practicum.ewm.repository.CategoryRepository;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
    private final LocalDateTime pastDate = now.minusHours(1);

    @Test
    void createEvent_ValidData_ReturnsEventFullDto() {
        Long userId = 1L;
        Long categoryId = 2L;

        NewEventDto newEventDto = new NewEventDto(
                "Valid annotation with at least 20 characters",
                categoryId,
                "Valid description with at least 20 characters",
                futureDate,
                new LocationDto(55.754167f, 37.62f),
                false,
                10,
                true,
                "Test Event"
        );

        User user = User.builder().id(userId).name("John Doe").email("john@example.com").build();
        Category category = Category.builder().id(categoryId).name("Concerts").build();
        Event event = EventMapper.toEvent(newEventDto);
        event.setId(1L);
        event.setInitiator(user);
        event.setCategory(category);
        event.setConfirmedRequests(0);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(eventRepository.save(any(Event.class))).thenReturn(event);

        EventFullDto result = eventService.createEvent(userId, newEventDto);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Event", result.getTitle());
        assertEquals("Valid annotation with at least 20 characters", result.getAnnotation());
        assertEquals(futureDate, result.getEventDate());
        assertEquals(EventState.PENDING, result.getState());
        verify(userRepository, times(1)).findById(userId);
        verify(categoryRepository, times(1)).findById(categoryId);
        verify(eventRepository, times(1)).save(any(Event.class));
    }

    @Test
    void createEvent_EventDateTooSoon_ThrowsValidationException() {
        Long userId = 1L;
        Long categoryId = 2L;
        LocalDateTime tooSoonDate = now.plusHours(1);

        NewEventDto newEventDto = new NewEventDto(
                "Event annotation",
                categoryId,
                "Event description",
                tooSoonDate,
                new LocationDto(55.754167f, 37.62f),
                false,
                10,
                true,
                "Test Event"
        );

        User user = User.builder().id(userId).name("John Doe").email("john@example.com").build();
        Category category = Category.builder().id(categoryId).name("Concerts").build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        ValidationException exception = assertThrows(ValidationException.class,
                () -> eventService.createEvent(userId, newEventDto));
        assertTrue(exception.getMessage().contains("должно содержать дату, которая еще не наступила"));
        verify(userRepository, times(1)).findById(userId);
        verify(categoryRepository, times(1)).findById(categoryId);
        verify(eventRepository, never()).save(any());
    }

    @Test
    void createEvent_UserNotFound_ThrowsNotFoundException() {
        Long userId = 999L;
        Long categoryId = 2L;

        NewEventDto newEventDto = new NewEventDto(
                "Event annotation",
                categoryId,
                "Event description",
                futureDate,
                new LocationDto(55.754167f, 37.62f),
                false,
                10,
                true,
                "Test Event"
        );

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> eventService.createEvent(userId, newEventDto));
        assertEquals("User with id=999 was not found", exception.getMessage());
        verify(userRepository, times(1)).findById(userId);
        verify(categoryRepository, never()).findById(any());
        verify(eventRepository, never()).save(any());
    }

    @Test
    void createEvent_CategoryNotFound_ThrowsNotFoundException() {
        Long userId = 1L;
        Long categoryId = 999L;

        NewEventDto newEventDto = new NewEventDto(
                "Event annotation",
                categoryId,
                "Event description",
                futureDate,
                new LocationDto(55.754167f, 37.62f),
                false,
                10,
                true,
                "Test Event"
        );

        User user = User.builder().id(userId).name("John Doe").email("john@example.com").build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> eventService.createEvent(userId, newEventDto));
        assertEquals("Category with id=999 was not found", exception.getMessage());
        verify(userRepository, times(1)).findById(userId);
        verify(categoryRepository, times(1)).findById(categoryId);
        verify(eventRepository, never()).save(any());
    }

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
        when(eventRepository.findByInitiatorId(userId, PageRequest.of(0, 10)))
                .thenReturn(eventPage.getContent());
        when(statsService.getViews(List.of(1L, 2L))).thenReturn(Map.of(1L, 100L, 2L, 200L));

        List<EventShortDto> result = eventService.getEventsByUser(userId, 0, 10);

        assertEquals(2, result.size());
        assertEquals("Event 1", result.get(0).getTitle());
        assertEquals("Event 2", result.get(1).getTitle());
        verify(eventRepository, times(1)).findByInitiatorId(userId, PageRequest.of(0, 10));
        verify(statsService, times(1)).getViews(List.of(1L, 2L));
    }

    @Test
    void getEventByUser_ValidIds_ReturnsEventFullDto() {
        Long userId = 1L;
        Long eventId = 10L;

        Event event = Event.builder()
                .id(eventId)
                .title("Test Event")
                .annotation("Annotation")
                .description("Description")
                .eventDate(futureDate)
                .initiator(User.builder().id(userId).name("User1").build())
                .category(Category.builder().id(1L).name("Category").build())
                .location(new Location(55.754167f, 37.62f))
                .paid(true)
                .participantLimit(100)
                .requestModeration(true)
                .state(EventState.PENDING)
                .createdOn(now.minusDays(1))
                .confirmedRequests(50)
                .views(500L)
                .build();

        when(eventRepository.findByIdAndInitiatorId(eventId, userId))
                .thenReturn(Optional.of(event));
        when(statsService.getViews(List.of(eventId)))
                .thenReturn(Map.of(eventId, 500L));

        EventFullDto result = eventService.getEventByUser(userId, eventId);

        assertNotNull(result);
        assertEquals(eventId, result.getId());
        assertEquals("Test Event", result.getTitle());
        assertEquals(EventState.PENDING, result.getState());
        assertEquals(50L, result.getConfirmedRequests());
        assertEquals(500L, result.getViews());
        verify(eventRepository, times(1)).findByIdAndInitiatorId(eventId, userId);
        verify(statsService, times(1)).getViews(List.of(eventId));
    }

    @Test
    void getEventByUser_EventNotFound_ThrowsNotFoundException() {
        Long userId = 1L;
        Long eventId = 999L;

        when(eventRepository.findByIdAndInitiatorId(eventId, userId))
                .thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> eventService.getEventByUser(userId, eventId));
        assertEquals("Event with id=999 was not found", exception.getMessage());
        verify(eventRepository, times(1)).findByIdAndInitiatorId(eventId, userId);
        verify(statsService, never()).getViews(any());
    }

    @Test
    void updateEventByUser_ValidUpdate_ReturnsUpdatedEvent() {
        Long userId = 1L;
        Long eventId = 10L;

        Event existingEvent = Event.builder()
                .id(eventId)
                .title("Old Title")
                .annotation("Old Annotation")
                .description("Old Description")
                .eventDate(futureDate.plusDays(5))
                .initiator(User.builder().id(userId).name("User1").build())
                .category(Category.builder().id(1L).name("Old Category").build())
                .location(new Location(55.0f, 37.0f))
                .paid(false)
                .participantLimit(50)
                .requestModeration(true)
                .state(EventState.PENDING)
                .confirmedRequests(10)
                .build();

        UpdateEventUserRequest updateRequest = new UpdateEventUserRequest();
        updateRequest.setTitle("New Title");
        updateRequest.setAnnotation("New Annotation");
        updateRequest.setCategory(2L);
        updateRequest.setEventDate(futureDate.plusDays(10));
        updateRequest.setLocation(new LocationDto(56.0f, 38.0f));
        updateRequest.setPaid(true);
        updateRequest.setParticipantLimit(100);
        updateRequest.setRequestModeration(false);
        updateRequest.setStateAction("SEND_TO_REVIEW");

        Category newCategory = Category.builder().id(2L).name("New Category").build();

        when(eventRepository.findByIdAndInitiatorId(eventId, userId))
                .thenReturn(Optional.of(existingEvent));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(newCategory));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(statsService.getViews(List.of(eventId))).thenReturn(Map.of(eventId, 100L));

        EventFullDto result = eventService.updateEventByUser(userId, eventId, updateRequest);

        assertNotNull(result);
        assertEquals("New Title", result.getTitle());
        assertEquals("New Annotation", result.getAnnotation());
        assertEquals(EventState.PENDING, result.getState());
        verify(eventRepository, times(1)).findByIdAndInitiatorId(eventId, userId);
        verify(categoryRepository, times(1)).findById(2L);
        verify(eventRepository, times(1)).save(any(Event.class));
    }

    @Test
    void updateEventByUser_PublishedEvent_ThrowsConflictException() {
        Long userId = 1L;
        Long eventId = 10L;

        Event publishedEvent = Event.builder()
                .id(eventId)
                .title("Published Event")
                .state(EventState.PUBLISHED)
                .initiator(User.builder().id(userId).build())
                .eventDate(futureDate)
                .build();

        UpdateEventUserRequest updateRequest = new UpdateEventUserRequest();
        updateRequest.setTitle("New Title");

        when(eventRepository.findByIdAndInitiatorId(eventId, userId))
                .thenReturn(Optional.of(publishedEvent));

        ConflictException exception = assertThrows(ConflictException.class,
                () -> eventService.updateEventByUser(userId, eventId, updateRequest));
        assertEquals("Only pending or canceled events can be changed", exception.getMessage());
        verify(eventRepository, times(1)).findByIdAndInitiatorId(eventId, userId);
        verify(eventRepository, never()).save(any());
    }

    @Test
    void updateEventByUser_EventDateTooSoon_ThrowsValidationException() {
        Long userId = 1L;
        Long eventId = 10L;

        Event existingEvent = Event.builder()
                .id(eventId)
                .title("Old Title")
                .state(EventState.PENDING)
                .initiator(User.builder().id(userId).build())
                .eventDate(futureDate.plusDays(5))
                .build();

        UpdateEventUserRequest updateRequest = new UpdateEventUserRequest();
        updateRequest.setEventDate(now.plusHours(1));

        when(eventRepository.findByIdAndInitiatorId(eventId, userId))
                .thenReturn(Optional.of(existingEvent));

        ValidationException exception = assertThrows(ValidationException.class,
                () -> eventService.updateEventByUser(userId, eventId, updateRequest));
        assertTrue(exception.getMessage().contains("должно содержать дату, которая еще не наступила"));
        verify(eventRepository, times(1)).findByIdAndInitiatorId(eventId, userId);
        verify(eventRepository, never()).save(any());
    }

    @Test
    void updateEventByAdmin_PublishEvent_ValidState_ReturnsPublishedEvent() {
        Long eventId = 10L;

        Event pendingEvent = Event.builder()
                .id(eventId)
                .title("Test Event")
                .state(EventState.PENDING)
                .eventDate(futureDate.plusHours(2))
                .initiator(User.builder().id(1L).build())
                .category(Category.builder().id(1L).build())
                .paid(true)
                .participantLimit(100)
                .requestModeration(true)
                .confirmedRequests(0)
                .views(0L)
                .build();

        UpdateEventAdminRequest updateRequest = new UpdateEventAdminRequest();
        updateRequest.setStateAction("PUBLISH_EVENT");

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(pendingEvent));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(statsService.getViews(List.of(eventId))).thenReturn(Map.of(eventId, 100L));

        EventFullDto result = eventService.updateEventByAdmin(eventId, updateRequest);

        assertNotNull(result);
        assertEquals(EventState.PUBLISHED, result.getState());
        assertNotNull(result.getPublishedOn());
        verify(eventRepository, times(1)).findById(eventId);
        verify(eventRepository, times(1)).save(any(Event.class));
    }

    @Test
    void updateEventByAdmin_PublishEvent_EventDateTooSoon_ThrowsConflictException() {
        Long eventId = 10L;

        Event pendingEvent = Event.builder()
                .id(eventId)
                .title("Test Event")
                .state(EventState.PENDING)
                .eventDate(now.plusMinutes(30))
                .build();

        UpdateEventAdminRequest updateRequest = new UpdateEventAdminRequest();
        updateRequest.setStateAction("PUBLISH_EVENT");

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(pendingEvent));

        ConflictException exception = assertThrows(ConflictException.class,
                () -> eventService.updateEventByAdmin(eventId, updateRequest));
        assertTrue(exception.getMessage().contains("Cannot publish the event because the event date is too soon"));
        verify(eventRepository, times(1)).findById(eventId);
        verify(eventRepository, never()).save(any());
    }

    @Test
    void updateEventByAdmin_PublishEvent_NotPendingState_ThrowsConflictException() {
        Long eventId = 10L;

        Event publishedEvent = Event.builder()
                .id(eventId)
                .title("Test Event")
                .state(EventState.PUBLISHED)
                .eventDate(futureDate)
                .build();

        UpdateEventAdminRequest updateRequest = new UpdateEventAdminRequest();
        updateRequest.setStateAction("PUBLISH_EVENT");

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(publishedEvent));

        ConflictException exception = assertThrows(ConflictException.class,
                () -> eventService.updateEventByAdmin(eventId, updateRequest));
        assertTrue(exception.getMessage().contains("Cannot publish the event because it's not in the right state"));
        verify(eventRepository, times(1)).findById(eventId);
        verify(eventRepository, never()).save(any());
    }

    @Test
    void updateEventByAdmin_RejectEvent_NotPublished_ReturnsCanceledEvent() {
        Long eventId = 10L;

        Event pendingEvent = Event.builder()
                .id(eventId)
                .title("Test Event")
                .state(EventState.PENDING)
                .eventDate(futureDate)
                .initiator(User.builder().id(1L).build())
                .category(Category.builder().id(1L).build())
                .paid(true)
                .participantLimit(100)
                .requestModeration(true)
                .confirmedRequests(0)
                .views(0L)
                .build();

        UpdateEventAdminRequest updateRequest = new UpdateEventAdminRequest();
        updateRequest.setStateAction("REJECT_EVENT");

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(pendingEvent));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(statsService.getViews(List.of(eventId))).thenReturn(Map.of(eventId, 100L));

        EventFullDto result = eventService.updateEventByAdmin(eventId, updateRequest);

        assertNotNull(result);
        assertEquals(EventState.CANCELED, result.getState());
        verify(eventRepository, times(1)).findById(eventId);
        verify(eventRepository, times(1)).save(any(Event.class));
    }

    @Test
    void updateEventByAdmin_RejectEvent_AlreadyPublished_ThrowsConflictException() {
        Long eventId = 10L;

        Event publishedEvent = Event.builder()
                .id(eventId)
                .title("Test Event")
                .state(EventState.PUBLISHED)
                .eventDate(futureDate)
                .build();

        UpdateEventAdminRequest updateRequest = new UpdateEventAdminRequest();
        updateRequest.setStateAction("REJECT_EVENT");

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(publishedEvent));

        ConflictException exception = assertThrows(ConflictException.class,
                () -> eventService.updateEventByAdmin(eventId, updateRequest));
        assertTrue(exception.getMessage().contains("Cannot reject the event because it's already published"));
        verify(eventRepository, times(1)).findById(eventId);
        verify(eventRepository, never()).save(any());
    }

    @Test
    void getEventPublic_ExistingPublishedEvent_ReturnsEventFullDto() {
        Long eventId = 10L;
        String ip = "192.168.1.1";

        Event publishedEvent = Event.builder()
                .id(eventId)
                .title("Published Event")
                .state(EventState.PUBLISHED)
                .eventDate(futureDate)
                .initiator(User.builder().id(1L).name("User1").build())
                .category(Category.builder().id(1L).name("Category").build())
                .location(new Location(55.754167f, 37.62f))
                .paid(true)
                .participantLimit(100)
                .requestModeration(true)
                .createdOn(now.minusDays(1))
                .publishedOn(now.minusHours(1))
                .confirmedRequests(50)
                .views(1000L)
                .build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(publishedEvent));
        when(statsService.getViews(List.of(eventId))).thenReturn(Map.of(eventId, 1000L));

        EventFullDto result = eventService.getEventPublic(eventId, ip);

        assertNotNull(result);
        assertEquals(eventId, result.getId());
        assertEquals("Published Event", result.getTitle());
        assertEquals(EventState.PUBLISHED, result.getState());
        assertEquals(1000L, result.getViews());
        verify(eventRepository, times(1)).findById(eventId);
        verify(statsService, times(1)).saveHit("/events/" + eventId, ip);
        verify(statsService, times(1)).getViews(List.of(eventId));
    }

    @Test
    void getEventPublic_NotPublishedEvent_ThrowsNotFoundException() {
        Long eventId = 10L;
        String ip = "192.168.1.1";

        Event pendingEvent = Event.builder()
                .id(eventId)
                .title("Pending Event")
                .state(EventState.PENDING)
                .build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(pendingEvent));

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> eventService.getEventPublic(eventId, ip));
        assertEquals("Event with id=10 was not found", exception.getMessage());
        verify(eventRepository, times(1)).findById(eventId);
        verify(statsService, never()).saveHit(any(), any());
        verify(statsService, never()).getViews(any());
    }

    @Test
    void getEventPublic_NonExistingEvent_ThrowsNotFoundException() {
        Long eventId = 999L;
        String ip = "192.168.1.1";

        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> eventService.getEventPublic(eventId, ip));
        assertEquals("Event with id=999 was not found", exception.getMessage());
        verify(eventRepository, times(1)).findById(eventId);
        verify(statsService, never()).saveHit(any(), any());
        verify(statsService, never()).getViews(any());
    }

    @Test
    void getEventsPublic_WithFilters_ReturnsFilteredEvents() {
        String text = "test";
        List<Long> categories = List.of(1L, 2L);
        Boolean paid = true;
        String rangeStart = futureDate.minusDays(1).toString();
        String rangeEnd = futureDate.plusDays(1).toString();
        Boolean onlyAvailable = false;
        String sort = "EVENT_DATE";
        Integer from = 0;
        Integer size = 10;
        String ip = "192.168.1.1";

        Event event1 = Event.builder()
                .id(1L)
                .title("Test Event 1")
                .annotation("Annotation with test word")
                .description("Description with test")
                .state(EventState.PUBLISHED)
                .eventDate(futureDate.plusHours(1))
                .initiator(User.builder().id(1L).name("User1").build())
                .category(Category.builder().id(1L).name("Category1").build())
                .paid(true)
                .participantLimit(100)
                .confirmedRequests(50)
                .views(100L)
                .build();

        Event event2 = Event.builder()
                .id(2L)
                .title("Test Event 2")
                .annotation("Another annotation")
                .description("Another description")
                .state(EventState.PUBLISHED)
                .eventDate(futureDate.plusHours(2))
                .initiator(User.builder().id(2L).name("User2").build())
                .category(Category.builder().id(2L).name("Category2").build())
                .paid(true)
                .participantLimit(50)
                .confirmedRequests(25)
                .views(200L)
                .build();

        Page<Event> eventPage = new PageImpl<>(List.of(event1, event2));
        when(eventRepository.findEventsPublic(
                eq(text), eq(categories), eq(paid), any(), any(), eq(onlyAvailable), any(PageRequest.class)))
                .thenReturn(eventPage.getContent());
        when(statsService.getViews(List.of(1L, 2L))).thenReturn(Map.of(1L, 100L, 2L, 200L));

        List<EventShortDto> result = eventService.getEventsPublic(
                text, categories, paid, rangeStart, rangeEnd, onlyAvailable, sort, from, size, ip);

        assertEquals(2, result.size());
        assertEquals("Test Event 1", result.get(0).getTitle());
        assertEquals("Test Event 2", result.get(1).getTitle());
        verify(statsService, times(1)).saveHit("/events", ip);
        verify(eventRepository, times(1)).findEventsPublic(
                eq(text), eq(categories), eq(paid), any(), any(), eq(onlyAvailable), any(PageRequest.class));
        verify(statsService, times(1)).getViews(List.of(1L, 2L));
    }

    @Test
    void getEventsByAdmin_WithFilters_ReturnsFilteredEvents() {
        List<Long> users = List.of(1L, 2L);
        List<String> states = List.of("PENDING", "PUBLISHED");
        List<Long> categories = List.of(1L, 2L);
        String rangeStart = now.minusDays(1).toString();
        String rangeEnd = now.plusDays(1).toString();
        Integer from = 0;
        Integer size = 10;

        Event event1 = Event.builder()
                .id(1L)
                .title("Event 1")
                .state(EventState.PENDING)
                .initiator(User.builder().id(1L).build())
                .category(Category.builder().id(1L).build())
                .eventDate(futureDate)
                .confirmedRequests(10)
                .views(100L)
                .build();

        Event event2 = Event.builder()
                .id(2L)
                .title("Event 2")
                .state(EventState.PUBLISHED)
                .initiator(User.builder().id(2L).build())
                .category(Category.builder().id(2L).build())
                .eventDate(futureDate.plusDays(1))
                .confirmedRequests(20)
                .views(200L)
                .build();

        Page<Event> eventPage = new PageImpl<>(List.of(event1, event2));
        when(eventRepository.findEventsByAdmin(
                eq(users), any(), eq(categories), any(), any(), any(PageRequest.class)))
                .thenReturn(eventPage.getContent());
        when(statsService.getViews(List.of(1L, 2L))).thenReturn(Map.of(1L, 100L, 2L, 200L));

        List<EventFullDto> result = eventService.getEventsByAdmin(
                users, states, categories, rangeStart, rangeEnd, from, size);

        assertEquals(2, result.size());
        assertEquals("Event 1", result.get(0).getTitle());
        assertEquals("Event 2", result.get(1).getTitle());
        assertEquals(EventState.PENDING, result.get(0).getState());
        assertEquals(EventState.PUBLISHED, result.get(1).getState());
        verify(eventRepository, times(1)).findEventsByAdmin(
                eq(users), any(), eq(categories), any(), any(), any(PageRequest.class));
        verify(statsService, times(1)).getViews(List.of(1L, 2L));
    }

    @Test
    void getEventsPublic_SortByViews_ReturnsSortedByViews() {
        String ip = "192.168.1.1";
        String sort = "VIEWS";

        Event event1 = Event.builder()
                .id(1L)
                .title("Event 1")
                .state(EventState.PUBLISHED)
                .eventDate(futureDate)
                .initiator(User.builder().id(1L).name("User1").build())
                .category(Category.builder().id(1L).name("Category1").build())
                .paid(false)
                .confirmedRequests(10)
                .views(100L)
                .build();

        Event event2 = Event.builder()
                .id(2L)
                .title("Event 2")
                .state(EventState.PUBLISHED)
                .eventDate(futureDate.plusDays(1))
                .initiator(User.builder().id(2L).name("User2").build())
                .category(Category.builder().id(2L).name("Category2").build())
                .paid(false)
                .confirmedRequests(20)
                .views(300L)
                .build();

        Event event3 = Event.builder()
                .id(3L)
                .title("Event 3")
                .state(EventState.PUBLISHED)
                .eventDate(futureDate.plusDays(2))
                .initiator(User.builder().id(3L).name("User3").build())
                .category(Category.builder().id(3L).name("Category3").build())
                .paid(false)
                .confirmedRequests(30)
                .views(200L)
                .build();

        Page<Event> eventPage = new PageImpl<>(List.of(event1, event2, event3));
        when(eventRepository.findEventsPublic(
                any(), any(), any(), any(), any(), any(), any(PageRequest.class)))
                .thenReturn(eventPage.getContent());
        when(statsService.getViews(List.of(1L, 2L, 3L)))
                .thenReturn(Map.of(1L, 100L, 2L, 300L, 3L, 200L));

        List<EventShortDto> result = eventService.getEventsPublic(
                null, null, null, null, null, false, sort, 0, 10, ip);

        assertEquals(3, result.size());
        assertEquals(2L, result.get(0).getId());
        assertEquals(3L, result.get(1).getId());
        assertEquals(1L, result.get(2).getId());
        verify(statsService, times(1)).saveHit("/events", ip);
    }

    @Test
    void getEventsByUser_EmptyResult_ReturnsEmptyList() {
        Long userId = 1L;

        Page<Event> emptyPage = new PageImpl<>(List.of());
        when(eventRepository.findByInitiatorId(userId, PageRequest.of(0, 10)))
                .thenReturn(emptyPage.getContent());

        List<EventShortDto> result = eventService.getEventsByUser(userId, 0, 10);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(eventRepository, times(1)).findByInitiatorId(userId, PageRequest.of(0, 10));
        verify(statsService, never()).getViews(any());
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
    void getEventsPublic_InvalidSortParameter_ThrowsValidationException() {
        String ip = "192.168.1.1";
        String invalidSort = "INVALID_SORT";

        ValidationException exception = assertThrows(ValidationException.class,
                () -> eventService.getEventsPublic(null, null, null, null, null,
                        false, invalidSort, 0, 10, ip));
        assertTrue(exception.getMessage().contains("Invalid sort parameter"));
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
}