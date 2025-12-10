package ru.practicum.ewm.mapper;

import org.junit.jupiter.api.Test;
import ru.practicum.ewm.dto.*;
import ru.practicum.ewm.model.*;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class MapperTest {

    private final LocalDateTime now = LocalDateTime.now();

    @Test
    void userMapper_toUser_shouldMapCorrectly() {
        NewUserRequest request = new NewUserRequest("John Doe", "john@example.com");

        User user = UserMapper.toUser(request);

        assertNotNull(user);
        assertEquals("John Doe", user.getName());
        assertEquals("john@example.com", user.getEmail());
        assertNull(user.getId());
    }

    @Test
    void userMapper_toUserDto_shouldMapCorrectly() {
        User user = User.builder()
                .id(1L)
                .name("John Doe")
                .email("john@example.com")
                .build();

        UserDto dto = UserMapper.toUserDto(user);

        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("John Doe", dto.getName());
        assertEquals("john@example.com", dto.getEmail());
    }

    @Test
    void categoryMapper_toCategory_shouldMapCorrectly() {
        NewCategoryDto request = new NewCategoryDto("Concerts");

        Category category = CategoryMapper.toCategory(request);

        assertNotNull(category);
        assertEquals("Concerts", category.getName());
        assertNull(category.getId());
    }

    @Test
    void categoryMapper_toCategoryDto_shouldMapCorrectly() {
        Category category = Category.builder()
                .id(1L)
                .name("Concerts")
                .build();

        CategoryDto dto = CategoryMapper.toCategoryDto(category);

        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("Concerts", dto.getName());
    }

    @Test
    void eventMapper_toEvent_shouldMapCorrectly() {
        NewEventDto newEventDto = new NewEventDto(
                "Annotation",
                1L,
                "Description",
                now.plusDays(1),
                new LocationDto(55.754167f, 37.62f),
                true,
                100,
                false,
                "Test Event"
        );

        Event event = EventMapper.toEvent(newEventDto);

        assertNotNull(event);
        assertEquals("Annotation", event.getAnnotation());
        assertEquals("Description", event.getDescription());
        assertEquals(now.plusDays(1), event.getEventDate());
        assertEquals(55.754167f, event.getLocation().getLat());
        assertEquals(37.62f, event.getLocation().getLon());
        assertTrue(event.getPaid());
        assertEquals(100, event.getParticipantLimit());
        assertFalse(event.getRequestModeration());
        assertEquals("Test Event", event.getTitle());
        assertEquals(EventState.PENDING, event.getState());
        assertNotNull(event.getCreatedOn());
        assertEquals(0, event.getConfirmedRequests());
        assertNull(event.getViews());
    }

    @Test
    void eventMapper_toEventFullDto_shouldMapCorrectly() {
        User initiator = User.builder()
                .id(1L)
                .name("John Doe")
                .email("john@example.com")
                .build();

        Category category = Category.builder()
                .id(2L)
                .name("Concerts")
                .build();

        Event event = Event.builder()
                .id(10L)
                .title("Test Event")
                .annotation("Annotation")
                .description("Description")
                .eventDate(now.plusDays(1))
                .initiator(initiator)
                .category(category)
                .location(new Location(55.754167f, 37.62f))
                .paid(true)
                .participantLimit(100)
                .requestModeration(true)
                .state(EventState.PUBLISHED)
                .createdOn(now.minusDays(1))
                .publishedOn(now.minusHours(1))
                .confirmedRequests(50)
                .views(1000L)
                .build();

        EventFullDto dto = EventMapper.toEventFullDto(event);

        assertNotNull(dto);
        assertEquals(10L, dto.getId());
        assertEquals("Test Event", dto.getTitle());
        assertEquals("Annotation", dto.getAnnotation());
        assertEquals("Description", dto.getDescription());
        assertEquals(now.plusDays(1), dto.getEventDate());
        assertEquals(now.minusDays(1), dto.getCreatedOn());
        assertEquals(now.minusHours(1), dto.getPublishedOn());

        assertNotNull(dto.getInitiator());
        assertEquals(1L, dto.getInitiator().getId());
        assertEquals("John Doe", dto.getInitiator().getName());

        assertNotNull(dto.getCategory());
        assertEquals(2L, dto.getCategory().getId());
        assertEquals("Concerts", dto.getCategory().getName());

        assertNotNull(dto.getLocation());
        assertEquals(55.754167f, dto.getLocation().getLat());
        assertEquals(37.62f, dto.getLocation().getLon());

        assertTrue(dto.getPaid());
        assertEquals(100, dto.getParticipantLimit());
        assertTrue(dto.getRequestModeration());
        assertEquals(EventState.PUBLISHED, dto.getState());
        assertEquals(50L, dto.getConfirmedRequests());
        assertEquals(1000L, dto.getViews());
    }

    @Test
    void eventMapper_toEventShortDto_shouldMapCorrectly() {
        User initiator = User.builder()
                .id(1L)
                .name("John Doe")
                .build();

        Category category = Category.builder()
                .id(2L)
                .name("Concerts")
                .build();

        Event event = Event.builder()
                .id(10L)
                .title("Test Event")
                .annotation("Annotation")
                .eventDate(now.plusDays(1))
                .initiator(initiator)
                .category(category)
                .paid(true)
                .confirmedRequests(50)
                .views(1000L)
                .build();

        EventShortDto dto = EventMapper.toEventShortDto(event);

        assertNotNull(dto);
        assertEquals(10L, dto.getId());
        assertEquals("Test Event", dto.getTitle());
        assertEquals("Annotation", dto.getAnnotation());
        assertEquals(now.plusDays(1), dto.getEventDate());

        assertNotNull(dto.getInitiator());
        assertEquals(1L, dto.getInitiator().getId());
        assertEquals("John Doe", dto.getInitiator().getName());

        assertNotNull(dto.getCategory());
        assertEquals(2L, dto.getCategory().getId());
        assertEquals("Concerts", dto.getCategory().getName());

        assertTrue(dto.getPaid());
        assertEquals(50L, dto.getConfirmedRequests());
        assertEquals(1000L, dto.getViews());
    }

    @Test
    void compilationMapper_toCompilationDto_shouldMapCorrectly() {
        User initiator = User.builder().id(1L).name("User1").build();
        Category category = Category.builder().id(1L).name("Category1").build();

        Event event1 = Event.builder()
                .id(1L)
                .title("Event 1")
                .annotation("Annotation 1")
                .eventDate(now.plusDays(1))
                .initiator(initiator)
                .category(category)
                .paid(true)
                .confirmedRequests(10)
                .views(100L)
                .build();

        Event event2 = Event.builder()
                .id(2L)
                .title("Event 2")
                .annotation("Annotation 2")
                .eventDate(now.plusDays(2))
                .initiator(initiator)
                .category(category)
                .paid(false)
                .confirmedRequests(20)
                .views(200L)
                .build();

        Compilation compilation = Compilation.builder()
                .id(1L)
                .events(java.util.Set.of(event1, event2))
                .pinned(true)
                .title("Test Compilation")
                .build();

        CompilationDto dto = CompilationMapper.toCompilationDto(compilation);

        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("Test Compilation", dto.getTitle());
        assertTrue(dto.getPinned());
        assertNotNull(dto.getEvents());
        assertEquals(2, dto.getEvents().size());

        assertEquals(1L, dto.getEvents().get(0).getId());
        assertEquals("Event 1", dto.getEvents().get(0).getTitle());
        assertEquals(2L, dto.getEvents().get(1).getId());
        assertEquals("Event 2", dto.getEvents().get(1).getTitle());
    }

    @Test
    void compilationMapper_toCompilationDto_withNullEvents_shouldHandleNull() {
        Compilation compilation = Compilation.builder()
                .id(1L)
                .events(null)
                .pinned(false)
                .title("Empty Compilation")
                .build();

        CompilationDto dto = CompilationMapper.toCompilationDto(compilation);

        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("Empty Compilation", dto.getTitle());
        assertFalse(dto.getPinned());
        assertNull(dto.getEvents());
    }

    @Test
    void compilationMapper_toCompilationDto_withEmptyEvents_shouldHandleEmptySet() {
        Compilation compilation = Compilation.builder()
                .id(1L)
                .events(java.util.Set.of())
                .pinned(true)
                .title("Empty Events Compilation")
                .build();

        CompilationDto dto = CompilationMapper.toCompilationDto(compilation);

        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("Empty Events Compilation", dto.getTitle());
        assertTrue(dto.getPinned());
        assertNotNull(dto.getEvents());
        assertTrue(dto.getEvents().isEmpty());
    }

    @Test
    void participationRequestMapper_toParticipationRequestDto_shouldMapCorrectly() {
        User requester = User.builder()
                .id(2L)
                .name("Jane Doe")
                .build();

        Event event = Event.builder()
                .id(10L)
                .title("Test Event")
                .build();

        ParticipationRequest request = ParticipationRequest.builder()
                .id(1L)
                .created(now)
                .event(event)
                .requester(requester)
                .status(RequestStatus.PENDING)
                .build();

        ParticipationRequestDto dto = ParticipationRequestMapper.toParticipationRequestDto(request);

        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals(now, dto.getCreated());
        assertEquals(10L, dto.getEvent());
        assertEquals(2L, dto.getRequester());
        assertEquals("PENDING", dto.getStatus());
    }

    @Test
    void participationRequestMapper_toParticipationRequestDto_withConfirmedStatus_shouldMapCorrectly() {
        ParticipationRequest request = ParticipationRequest.builder()
                .id(1L)
                .created(now)
                .event(Event.builder().id(10L).build())
                .requester(User.builder().id(2L).build())
                .status(RequestStatus.CONFIRMED)
                .build();

        ParticipationRequestDto dto = ParticipationRequestMapper.toParticipationRequestDto(request);

        assertNotNull(dto);
        assertEquals("CONFIRMED", dto.getStatus());
    }

    @Test
    void eventMapper_toEvent_withDefaultValues_shouldUseDefaults() {
        NewEventDto newEventDto = new NewEventDto();
        newEventDto.setAnnotation("Annotation");
        newEventDto.setCategory(1L);
        newEventDto.setDescription("Description");
        newEventDto.setEventDate(now.plusDays(1));
        newEventDto.setLocation(new LocationDto(55.0f, 37.0f));
        newEventDto.setTitle("Test Event");


        Event event = EventMapper.toEvent(newEventDto);

        assertNotNull(event);
        assertFalse(event.getPaid());
        assertEquals(0, event.getParticipantLimit());
        assertTrue(event.getRequestModeration());
        assertEquals(EventState.PENDING, event.getState());
    }
}