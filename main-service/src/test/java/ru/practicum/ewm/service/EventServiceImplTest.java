package ru.practicum.ewm.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.ewm.dto.*;
import ru.practicum.ewm.exception.*;
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

    @BeforeEach
    void setUp() {
        reset(eventRepository, userRepository, categoryRepository, statsService);
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

        when(userRepository.existsById(userId)).thenReturn(true);
        when(eventRepository.findByInitiatorId(eq(userId), any(PageRequest.class)))
                .thenReturn(List.of(event1, event2));
        when(statsService.getViews(List.of(1L, 2L))).thenReturn(Map.of(1L, 100L, 2L, 200L));

        List<EventShortDto> result = eventService.getEventsByUser(userId, 0, 10);

        assertEquals(2, result.size());
        assertEquals("Event 1", result.get(0).getTitle());
        assertEquals("Event 2", result.get(1).getTitle());
        verify(userRepository, times(1)).existsById(userId);
        verify(eventRepository, times(1)).findByInitiatorId(eq(userId), any(PageRequest.class));
        verify(statsService, times(1)).getViews(List.of(1L, 2L));
    }

    @Test
    void getEventsByUser_WithEmptyResult_ReturnsEmptyList() {
        Long userId = 1L;

        when(userRepository.existsById(userId)).thenReturn(true);
        when(eventRepository.findByInitiatorId(eq(userId), any(PageRequest.class)))
                .thenReturn(List.of());

        List<EventShortDto> result = eventService.getEventsByUser(userId, 0, 10);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getEventsByUser_InvalidFromParam_ThrowsValidationException() {
        Long userId = 1L;

        when(userRepository.existsById(userId)).thenReturn(true);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> eventService.getEventsByUser(userId, -1, 10));

        assertTrue(exception.getMessage().contains("Parameter 'from' must be greater than or equal to 0"));
    }

    @Test
    void getEventsByUser_InvalidSizeParam_ThrowsValidationException() {
        Long userId = 1L;

        when(userRepository.existsById(userId)).thenReturn(true);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> eventService.getEventsByUser(userId, 0, 0));

        assertTrue(exception.getMessage().contains("Parameter 'size' must be greater than 0"));
    }

    @Test
    void getEventsByUser_NullParams_ReturnsEvents() {
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

        when(userRepository.existsById(userId)).thenReturn(true);
        when(eventRepository.findByInitiatorId(eq(userId), any(PageRequest.class)))
                .thenReturn(List.of(event));
        when(statsService.getViews(List.of(1L))).thenReturn(Map.of(1L, 100L));

        List<EventShortDto> result = eventService.getEventsByUser(userId, null, null);

        assertEquals(1, result.size());
        assertEquals("Event 1", result.get(0).getTitle());
    }

    @Test
    void getEventsPublic_WithFilters_ReturnsFilteredEvents() {
        String text = "test";
        List<Long> categories = List.of(1L, 2L);
        Boolean paid = true;
        String ip = "192.168.1.1";
        Integer from = 0;
        Integer size = 10;

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

        when(eventRepository.findAll(
                any(Specification.class),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(event1, event2)));

        when(statsService.getViews(List.of(1L, 2L))).thenReturn(Map.of(1L, 100L, 2L, 200L));
        doNothing().when(statsService).saveHit("/events", ip);

        List<EventShortDto> result = eventService.getEventsPublic(
                text, categories, paid, null, null, false, "EVENT_DATE", from, size, ip);

        assertEquals(2, result.size());
        assertEquals("Test Event 1", result.get(0).getTitle());
        assertEquals("Test Event 2", result.get(1).getTitle());

        verify(statsService, times(1)).saveHit("/events", ip);
        verify(eventRepository, times(1))
                .findAll(any(Specification.class), any(Pageable.class));
        verify(statsService, times(1)).getViews(List.of(1L, 2L));
    }



    @Test
    void getEventsPublic_WithEmptyTextAndCategories_ReturnsEvents() {
        String ip = "192.168.1.1";

        Event event = Event.builder()
                .id(1L)
                .title("Test Event")
                .annotation("Annotation")
                .description("Description")
                .state(EventState.PUBLISHED)
                .eventDate(futureDate.plusHours(1))
                .initiator(User.builder().id(1L).name("User1").build())
                .category(Category.builder().id(1L).name("Category1").build())
                .paid(true)
                .participantLimit(100)
                .confirmedRequests(50)
                .views(100L)
                .build();

        when(eventRepository.findAll(
                any(Specification.class),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(event)));

        when(statsService.getViews(List.of(1L))).thenReturn(Map.of(1L, 100L));
        doNothing().when(statsService).saveHit("/events", ip);

        List<EventShortDto> result = eventService.getEventsPublic(
                "", List.of(), null, null, null, false, null, 0, 10, ip);

        assertEquals(1, result.size());
        assertEquals("Test Event", result.get(0).getTitle());
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
    void getEventsPublic_WithStartAfterEnd_ThrowsValidationException() {
        String ip = "192.168.1.1";
        String rangeStart = futureDate.plusDays(2).format(formatter);
        String rangeEnd = futureDate.plusDays(1).format(formatter);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> eventService.getEventsPublic(null, null, null, rangeStart, rangeEnd,
                        false, null, 0, 10, ip));

        assertTrue(exception.getMessage().contains("rangeStart must be before rangeEnd"));
    }

    @Test
    void getEventsPublic_WithOnlyAvailableTrue_ReturnsFilteredEvents() {
        String ip = "192.168.1.1";

        Event event1 = Event.builder()
                .id(1L)
                .title("Available Event")
                .annotation("Annotation")
                .description("Description")
                .state(EventState.PUBLISHED)
                .eventDate(futureDate)
                .initiator(User.builder().id(1L).name("User1").build())
                .category(Category.builder().id(1L).name("Category1").build())
                .paid(true)
                .participantLimit(100)
                .confirmedRequests(50)
                .views(100L)
                .build();

        Event event2 = Event.builder()
                .id(2L)
                .title("Full Event")
                .annotation("Annotation")
                .description("Description")
                .state(EventState.PUBLISHED)
                .eventDate(futureDate)
                .initiator(User.builder().id(2L).name("User2").build())
                .category(Category.builder().id(2L).name("Category2").build())
                .paid(true)
                .participantLimit(10)
                .confirmedRequests(10)
                .views(200L)
                .build();

        when(eventRepository.findAll(
                any(Specification.class),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(event1, event2)));

        when(statsService.getViews(List.of(1L, 2L))).thenReturn(Map.of(1L, 100L, 2L, 200L));
        doNothing().when(statsService).saveHit("/events", ip);

        List<EventShortDto> result = eventService.getEventsPublic(
                null, null, null, null, null, true, null, 0, 10, ip);

        assertEquals(1, result.size());
        assertEquals("Available Event", result.get(0).getTitle());
    }


    @Test
    void getEventsPublic_WithSortViews_ReturnsSortedByViews() {
        String ip = "192.168.1.1";

        Event event1 = Event.builder()
                .id(1L)
                .title("Event 1")
                .annotation("Annotation 1")
                .description("Description 1")
                .state(EventState.PUBLISHED)
                .eventDate(futureDate)
                .initiator(User.builder().id(1L).name("User1").build())
                .category(Category.builder().id(1L).name("Category1").build())
                .paid(true)
                .participantLimit(100)
                .confirmedRequests(50)
                .views(100L)
                .build();

        Event event2 = Event.builder()
                .id(2L)
                .title("Event 2")
                .annotation("Annotation 2")
                .description("Description 2")
                .state(EventState.PUBLISHED)
                .eventDate(futureDate)
                .initiator(User.builder().id(2L).name("User2").build())
                .category(Category.builder().id(2L).name("Category2").build())
                .paid(true)
                .participantLimit(100)
                .confirmedRequests(50)
                .views(200L)
                .build();

        when(eventRepository.findAll(
                any(Specification.class),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(event1, event2)));

        when(statsService.getViews(List.of(1L, 2L))).thenReturn(Map.of(1L, 100L, 2L, 200L));
        doNothing().when(statsService).saveHit("/events", ip);

        List<EventShortDto> result = eventService.getEventsPublic(
                null, null, null, null, null, false, "VIEWS", 0, 10, ip);

        assertEquals(2, result.size());
        assertEquals(200L, result.get(0).getViews());
        assertEquals(100L, result.get(1).getViews());
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
    void createEvent_WithPastEventDate_ThrowsValidationException() {
        Long userId = 1L;
        NewEventDto newEventDto = new NewEventDto(
                "Annotation with at least 20 characters",
                1L,
                "Description with at least 20 characters",
                now.minusHours(1),
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

        assertTrue(exception.getMessage().contains("Field: eventDate. Error: должно содержать дату, которая еще не наступила"));
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
        doNothing().when(statsService).saveHit("/events/" + eventId, ip);

        EventFullDto result = eventService.getEventPublic(eventId, ip);

        assertNotNull(result);
        assertEquals(eventId, result.getId());
        verify(eventRepository, times(1)).findById(eventId);
        verify(statsService, times(1)).getViews(List.of(eventId));
        verify(statsService, times(1)).saveHit("/events/" + eventId, ip);
    }

    @Test
    void getEventPublic_EventNotPublished_ThrowsNotFoundException() {
        Long eventId = 1L;
        String ip = "192.168.1.1";

        Event event = Event.builder()
                .id(eventId)
                .title("Test Event")
                .annotation("Annotation")
                .description("Description")
                .state(EventState.PENDING)
                .eventDate(futureDate)
                .initiator(User.builder().id(1L).name("John Doe").build())
                .category(Category.builder().id(1L).name("Concerts").build())
                .paid(true)
                .participantLimit(100)
                .confirmedRequests(50)
                .views(100L)
                .build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> eventService.getEventPublic(eventId, ip));

        assertEquals("Event with id=1 was not found", exception.getMessage());
    }

    @Test
    void getEventByUser_ValidIds_ReturnsEventFullDto() {
        Long userId = 1L;
        Long eventId = 10L;

        Event event = Event.builder()
                .id(eventId)
                .title("User Event")
                .annotation("Annotation")
                .description("Description")
                .state(EventState.PENDING)
                .eventDate(futureDate)
                .initiator(User.builder().id(userId).name("John Doe").build())
                .category(Category.builder().id(1L).name("Concerts").build())
                .paid(true)
                .participantLimit(100)
                .confirmedRequests(50)
                .views(100L)
                .build();

        when(eventRepository.findByIdAndInitiatorId(eventId, userId)).thenReturn(Optional.of(event));
        when(statsService.getViews(List.of(eventId))).thenReturn(Map.of(eventId, 100L));

        EventFullDto result = eventService.getEventByUser(userId, eventId);

        assertNotNull(result);
        assertEquals(eventId, result.getId());
        assertEquals("User Event", result.getTitle());
    }

    @Test
    void updateEventByUser_ValidData_ReturnsUpdatedEvent() {
        Long userId = 1L;
        Long eventId = 10L;

        Event event = Event.builder()
                .id(eventId)
                .title("Old Title")
                .annotation("Old Annotation")
                .description("Old Description")
                .state(EventState.PENDING)
                .eventDate(futureDate)
                .initiator(User.builder().id(userId).name("John Doe").build())
                .category(Category.builder().id(1L).name("Concerts").build())
                .paid(false)
                .participantLimit(50)
                .confirmedRequests(0)
                .views(0L)
                .build();

        UpdateEventUserRequest updateRequest = new UpdateEventUserRequest();
        updateRequest.setTitle("New Title");
        updateRequest.setAnnotation("New Annotation with at least 20 characters");
        updateRequest.setDescription("New Description with at least 20 characters");
        updateRequest.setPaid(true);
        updateRequest.setParticipantLimit(100);
        updateRequest.setStateAction("SEND_TO_REVIEW");

        when(eventRepository.findByIdAndInitiatorId(eventId, userId)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenReturn(event);
        when(statsService.getViews(List.of(eventId))).thenReturn(Map.of(eventId, 0L));

        EventFullDto result = eventService.updateEventByUser(userId, eventId, updateRequest);

        assertNotNull(result);
        assertEquals("New Title", result.getTitle());
        assertEquals(EventState.PENDING, result.getState());
        verify(eventRepository, times(1)).save(any(Event.class));
    }

    @Test
    void updateEventByUser_EventPublished_ThrowsConflictException() {
        Long userId = 1L;
        Long eventId = 10L;

        Event event = Event.builder()
                .id(eventId)
                .title("Test Event")
                .annotation("Annotation")
                .description("Description")
                .state(EventState.PUBLISHED)
                .eventDate(futureDate)
                .initiator(User.builder().id(userId).name("John Doe").build())
                .category(Category.builder().id(1L).name("Concerts").build())
                .paid(true)
                .confirmedRequests(0)
                .views(0L)
                .build();

        UpdateEventUserRequest updateRequest = new UpdateEventUserRequest();
        updateRequest.setTitle("Updated Title");

        when(eventRepository.findByIdAndInitiatorId(eventId, userId)).thenReturn(Optional.of(event));

        ConflictException exception = assertThrows(ConflictException.class,
                () -> eventService.updateEventByUser(userId, eventId, updateRequest));

        assertEquals("Only pending or canceled events can be changed", exception.getMessage());
    }

    @Test
    void updateEventByUser_EventDateTooSoon_ThrowsValidationException() {
        Long userId = 1L;
        Long eventId = 10L;

        Event event = Event.builder()
                .id(eventId)
                .title("Test Event")
                .annotation("Annotation")
                .description("Description")
                .state(EventState.PENDING)
                .eventDate(futureDate)
                .initiator(User.builder().id(userId).name("John Doe").build())
                .category(Category.builder().id(1L).name("Concerts").build())
                .paid(true)
                .confirmedRequests(0)
                .views(0L)
                .build();

        UpdateEventUserRequest updateRequest = new UpdateEventUserRequest();
        updateRequest.setEventDate(now.plusHours(1));

        when(eventRepository.findByIdAndInitiatorId(eventId, userId)).thenReturn(Optional.of(event));

        ValidationException exception = assertThrows(ValidationException.class,
                () -> eventService.updateEventByUser(userId, eventId, updateRequest));

        assertTrue(exception.getMessage().contains("Field: eventDate. Error: должно содержать дату, которая еще не наступила"));
    }

    @Test
    void updateEventByAdmin_ValidData_ReturnsUpdatedEvent() {
        Long eventId = 1L;

        Event event = Event.builder()
                .id(eventId)
                .title("Old Title")
                .annotation("Old Annotation")
                .description("Old Description")
                .state(EventState.PENDING)
                .eventDate(futureDate.plusHours(2))
                .initiator(User.builder().id(1L).name("John Doe").build())
                .category(Category.builder().id(1L).name("Concerts").build())
                .paid(false)
                .participantLimit(50)
                .confirmedRequests(0)
                .views(0L)
                .build();

        UpdateEventAdminRequest updateRequest = new UpdateEventAdminRequest();
        updateRequest.setTitle("New Title");
        updateRequest.setAnnotation("New Annotation with at least 20 characters");
        updateRequest.setDescription("New Description with at least 20 characters");
        updateRequest.setPaid(true);
        updateRequest.setParticipantLimit(100);
        updateRequest.setStateAction("PUBLISH_EVENT");

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenReturn(event);
        when(statsService.getViews(List.of(eventId))).thenReturn(Map.of(eventId, 0L));

        EventFullDto result = eventService.updateEventByAdmin(eventId, updateRequest);

        assertNotNull(result);
        assertEquals("New Title", result.getTitle());
        assertEquals(EventState.PUBLISHED, result.getState());
        verify(eventRepository, times(1)).save(any(Event.class));
    }

    @Test
    void updateEventByAdmin_InvalidStateAction_ThrowsValidationException() {
        Long eventId = 1L;

        Event event = Event.builder()
                .id(eventId)
                .title("Test Event")
                .annotation("Annotation")
                .description("Description")
                .state(EventState.PENDING)
                .eventDate(futureDate)
                .initiator(User.builder().id(1L).name("John Doe").build())
                .category(Category.builder().id(1L).name("Concerts").build())
                .paid(true)
                .confirmedRequests(0)
                .views(0L)
                .build();

        UpdateEventAdminRequest updateRequest = new UpdateEventAdminRequest();
        updateRequest.setStateAction("INVALID_ACTION");

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        ValidationException exception = assertThrows(ValidationException.class,
                () -> eventService.updateEventByAdmin(eventId, updateRequest));

        assertTrue(exception.getMessage().contains("Invalid state action"));
    }

    @Test
    void updateEventByAdmin_EventAlreadyPublished_ThrowsConflictException() {
        Long eventId = 1L;

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
                .confirmedRequests(0)
                .views(0L)
                .build();

        UpdateEventAdminRequest updateRequest = new UpdateEventAdminRequest();
        updateRequest.setStateAction("REJECT_EVENT");

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        ConflictException exception = assertThrows(ConflictException.class,
                () -> eventService.updateEventByAdmin(eventId, updateRequest));

        assertEquals("Cannot reject the event because it's already published", exception.getMessage());
    }

    @Test
    void updateEventByAdmin_EventDateTooSoonForPublish_ThrowsConflictException() {
        Long eventId = 1L;

        Event event = Event.builder()
                .id(eventId)
                .title("Test Event")
                .annotation("Annotation")
                .description("Description")
                .state(EventState.PENDING)
                .eventDate(now.plusMinutes(30))
                .initiator(User.builder().id(1L).name("John Doe").build())
                .category(Category.builder().id(1L).name("Concerts").build())
                .paid(true)
                .confirmedRequests(0)
                .views(0L)
                .build();

        UpdateEventAdminRequest updateRequest = new UpdateEventAdminRequest();
        updateRequest.setStateAction("PUBLISH_EVENT");

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        ConflictException exception = assertThrows(ConflictException.class,
                () -> eventService.updateEventByAdmin(eventId, updateRequest));

        assertTrue(exception.getMessage().contains("Cannot publish the event because the event date is too soon"));
    }


    @Test
    void getEventsByAdmin_WithNullParams_ReturnsEvents() {
        Event event = Event.builder()
                .id(1L)
                .title("Admin Event")
                .annotation("Annotation")
                .description("Description")
                .state(EventState.PENDING)
                .eventDate(futureDate)
                .initiator(User.builder().id(1L).name("User1").build())
                .category(Category.builder().id(1L).name("Category1").build())
                .paid(true)
                .confirmedRequests(0)
                .views(0L)
                .build();

        when(eventRepository.findAll(
                any(Specification.class),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(event)));

        when(statsService.getViews(List.of(1L))).thenReturn(Map.of(1L, 0L));

        List<EventFullDto> result = eventService.getEventsByAdmin(
                null, null, null, null, null, 0, 10);

        assertEquals(1, result.size());
        assertEquals("Admin Event", result.get(0).getTitle());
    }

    @Test
    void getEventsByAdmin_WithEmptyLists_ReturnsEmptyList() {
        when(eventRepository.findAll(
                any(Specification.class),
                any(Pageable.class)
        )).thenReturn(Page.empty());


        List<EventFullDto> result = eventService.getEventsByAdmin(
                List.of(), List.of(), List.of(),
                null, null,
                0, 10
        );
        assertNotNull(result);
        assertTrue(result.isEmpty());


        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getEventsByAdmin_WithInvalidState_ThrowsValidationException() {
        List<String> invalidStates = List.of("INVALID_STATE");

        ValidationException exception = assertThrows(ValidationException.class,
                () -> eventService.getEventsByAdmin(
                        null, invalidStates, null, null, null, 0, 10));

        assertTrue(exception.getMessage().contains("Invalid state value in states parameter"));
    }

    @Test
    void getEventsByAdmin_WithStartAfterEnd_ThrowsValidationException() {
        String rangeStart = futureDate.plusDays(2).format(formatter);
        String rangeEnd = futureDate.plusDays(1).format(formatter);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> eventService.getEventsByAdmin(
                        null, null, null, rangeStart, rangeEnd, 0, 10));

        assertTrue(exception.getMessage().contains("rangeStart must be before rangeEnd"));
    }

    @Test
    void getEventsByAdmin_WithUsersFilter_ReturnsFilteredEvents() {
        List<Long> users = List.of(1L, 2L);
        List<String> states = List.of("PENDING", "PUBLISHED");
        List<Long> categories = List.of(1L, 2L);

        Event event = Event.builder()
                .id(1L)
                .title("Filtered Event")
                .annotation("Annotation")
                .description("Description")
                .state(EventState.PENDING)
                .eventDate(futureDate)
                .initiator(User.builder().id(1L).name("User1").build())
                .category(Category.builder().id(1L).name("Category1").build())
                .paid(true)
                .confirmedRequests(0)
                .views(0L)
                .build();

        when(eventRepository.findAll(
                any(Specification.class),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(event)));

        when(statsService.getViews(List.of(1L))).thenReturn(Map.of(1L, 0L));

        List<EventFullDto> result = eventService.getEventsByAdmin(
                users, states, categories, null, null, 0, 10);

        assertEquals(1, result.size());
        assertEquals("Filtered Event", result.get(0).getTitle());
    }
}