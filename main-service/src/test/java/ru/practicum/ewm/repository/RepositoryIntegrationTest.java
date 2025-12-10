package ru.practicum.ewm.repository;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import ru.practicum.ewm.model.*;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class RepositoryIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ParticipationRequestRepository requestRepository;

    @Autowired
    private CompilationRepository compilationRepository;

    private User user1;
    private User user2;
    private Category category1;
    private Category category2;
    private Event event1;
    private Event event2;
    private Event event3;
    private final LocalDateTime now = LocalDateTime.now();

    @BeforeEach
    void setUp() {
        user1 = User.builder()
                .name("John Doe")
                .email("john@example.com")
                .build();
        user1 = entityManager.persist(user1);

        user2 = User.builder()
                .name("Jane Smith")
                .email("jane@example.com")
                .build();
        user2 = entityManager.persist(user2);

        category1 = Category.builder()
                .name("Concerts")
                .build();
        category1 = entityManager.persist(category1);

        category2 = Category.builder()
                .name("Theater")
                .build();
        category2 = entityManager.persist(category2);

        event1 = Event.builder()
                .title("Rock Concert")
                .annotation("Amazing rock concert")
                .description("Full description of the rock concert")
                .eventDate(now.plusDays(1))
                .initiator(user1)
                .category(category1)
                .location(new Location(55.754167f, 37.62f))
                .paid(true)
                .participantLimit(100)
                .requestModeration(true)
                .state(EventState.PUBLISHED)
                .confirmedRequests(50)
                .createdOn(now.minusDays(1))
                .publishedOn(now.minusHours(1))
                .build();
        event1 = entityManager.persist(event1);

        event2 = Event.builder()
                .title("Theater Play")
                .annotation("Classic theater play")
                .description("Full description of the theater play")
                .eventDate(now.plusDays(2))
                .initiator(user2)
                .category(category2)
                .location(new Location(55.755f, 37.621f))
                .paid(false)
                .participantLimit(50)
                .requestModeration(false)
                .state(EventState.PENDING)
                .confirmedRequests(10)
                .createdOn(now.minusDays(1))
                .build();
        event2 = entityManager.persist(event2);

        event3 = Event.builder()
                .title("Jazz Concert")
                .annotation("Relaxing jazz evening")
                .description("Full description of the jazz concert")
                .eventDate(now.plusDays(3))
                .initiator(user1)
                .category(category1)
                .location(new Location(55.756f, 37.622f))
                .paid(true)
                .participantLimit(200)
                .requestModeration(true)
                .state(EventState.CANCELED)
                .confirmedRequests(0)
                .createdOn(now.minusDays(2))
                .build();
        event3 = entityManager.persist(event3);
    }

    @Test
    void userRepository_saveAndFindById_shouldWork() {
        User newUser = User.builder()
                .name("Test User")
                .email("test@example.com")
                .build();

        User savedUser = userRepository.save(newUser);
        User foundUser = userRepository.findById(savedUser.getId()).orElse(null);

        assertNotNull(foundUser);
        assertEquals("Test User", foundUser.getName());
        assertEquals("test@example.com", foundUser.getEmail());
    }

    @Test
    void userRepository_findByIdIn_shouldReturnFilteredUsers() {
        List<User> users = userRepository.findByIdIn(
                List.of(user1.getId(), user2.getId()),
                PageRequest.of(0, 10));

        assertEquals(2, users.size());
        assertTrue(users.stream().anyMatch(u -> u.getEmail().equals("john@example.com")));
        assertTrue(users.stream().anyMatch(u -> u.getEmail().equals("jane@example.com")));
    }

    @Test
    void userRepository_findByIdIn_withNonExistingIds_shouldReturnEmptyList() {
        List<User> users = userRepository.findByIdIn(
                List.of(999L, 1000L),
                PageRequest.of(0, 10));

        assertTrue(users.isEmpty());
    }

    @Test
    void categoryRepository_saveAndFindById_shouldWork() {
        Category newCategory = Category.builder()
                .name("Cinema")
                .build();

        Category savedCategory = categoryRepository.save(newCategory);
        Category foundCategory = categoryRepository.findById(savedCategory.getId()).orElse(null);

        assertNotNull(foundCategory);
        assertEquals("Cinema", foundCategory.getName());
    }

    @Test
    void categoryRepository_findAll_shouldReturnAllCategories() {
        List<Category> categories = categoryRepository.findAll();

        assertEquals(2, categories.size());
        assertTrue(categories.stream().anyMatch(c -> c.getName().equals("Concerts")));
        assertTrue(categories.stream().anyMatch(c -> c.getName().equals("Theater")));
    }

    @Test
    void eventRepository_findByInitiatorId_shouldReturnUserEvents() {
        List<Event> events = eventRepository.findByInitiatorId(user1.getId(), PageRequest.of(0, 10));

        assertEquals(2, events.size());
        assertTrue(events.stream().allMatch(e -> e.getInitiator().getId().equals(user1.getId())));
    }

    @Test
    void eventRepository_findByIdAndInitiatorId_shouldReturnSpecificEvent() {
        var foundEvent = eventRepository.findByIdAndInitiatorId(event1.getId(), user1.getId());

        assertTrue(foundEvent.isPresent());
        assertEquals("Rock Concert", foundEvent.get().getTitle());
        assertEquals(user1.getId(), foundEvent.get().getInitiator().getId());
    }

    @Test
    void eventRepository_findByIdAndInitiatorId_withWrongInitiator_shouldReturnEmpty() {
        var foundEvent = eventRepository.findByIdAndInitiatorId(event1.getId(), user2.getId());

        assertTrue(foundEvent.isEmpty());
    }

    @Test
    void eventRepository_findEventsByAdmin_withAllFilters_shouldReturnFilteredEvents() {
        List<Long> users = List.of(user1.getId());
        List<ru.practicum.ewm.model.EventState> states = List.of(EventState.PUBLISHED);
        List<Long> categories = List.of(category1.getId());
        LocalDateTime rangeStart = now.minusDays(3);
        LocalDateTime rangeEnd = now.plusDays(5);

        List<Event> events = eventRepository.findEventsByAdmin(
                users, states, categories, rangeStart, rangeEnd, PageRequest.of(0, 10));

        assertEquals(1, events.size());
        assertEquals("Rock Concert", events.get(0).getTitle());
        assertEquals(EventState.PUBLISHED, events.get(0).getState());
    }

    @Test
    void eventRepository_findEventsByAdmin_withNullFilters_shouldReturnAllEvents() {
        List<Event> events = eventRepository.findEventsByAdmin(
                null, null, null, null, null, PageRequest.of(0, 10));

        assertEquals(3, events.size());
    }

    @Test
    void eventRepository_findEventsPublic_withTextSearch_shouldReturnMatchingEvents() {
        String text = "rock";
        List<Long> categories = List.of(category1.getId());
        Boolean paid = true;
        LocalDateTime rangeStart = now.minusDays(1);
        LocalDateTime rangeEnd = now.plusDays(10);
        Boolean onlyAvailable = false;

        List<Event> events = eventRepository.findEventsPublic(
                text, categories, paid, rangeStart, rangeEnd, onlyAvailable, PageRequest.of(0, 10));

        assertEquals(1, events.size());
        assertEquals("Rock Concert", events.get(0).getTitle());
        assertEquals(EventState.PUBLISHED, events.get(0).getState());
    }

    @Test
    void eventRepository_findEventsPublic_onlyAvailable_shouldReturnEventsWithFreeSlots() {
        Event fullEvent = Event.builder()
                .title("Full Event")
                .annotation("This event is full")
                .description("Description")
                .eventDate(now.plusDays(5))
                .initiator(user1)
                .category(category1)
                .location(new Location(55.0f, 37.0f))
                .paid(false)
                .participantLimit(10)
                .confirmedRequests(10)
                .requestModeration(true)
                .state(EventState.PUBLISHED)
                .createdOn(now)
                .build();
        entityManager.persist(fullEvent);

        Boolean onlyAvailable = true;

        List<Event> events = eventRepository.findEventsPublic(
                null, null, null, now.minusDays(1), now.plusDays(10), onlyAvailable, PageRequest.of(0, 10));

        assertEquals(1, events.size());
        assertEquals("Rock Concert", events.get(0).getTitle());
    }

    @Test
    void eventRepository_findEventsPublic_withUnlimitedParticipantLimit_shouldAlwaysBeAvailable() {
        Event unlimitedEvent = Event.builder()
                .title("Unlimited Event")
                .annotation("Unlimited participants")
                .description("Description")
                .eventDate(now.plusDays(6))
                .initiator(user2)
                .category(category2)
                .location(new Location(55.0f, 37.0f))
                .paid(false)
                .participantLimit(0)
                .confirmedRequests(1000)
                .requestModeration(true)
                .state(EventState.PUBLISHED)
                .createdOn(now)
                .build();
        entityManager.persist(unlimitedEvent);

        Boolean onlyAvailable = true;

        List<Event> events = eventRepository.findEventsPublic(
                null, null, null, now.minusDays(1), now.plusDays(10), onlyAvailable, PageRequest.of(0, 10));

        assertTrue(events.size() >= 2);
        assertTrue(events.stream().anyMatch(e -> e.getTitle().equals("Rock Concert")));
        assertTrue(events.stream().anyMatch(e -> e.getTitle().equals("Unlimited Event")));
    }

    @Test
    void eventRepository_findByIdIn_shouldReturnEventsByIds() {
        List<Long> eventIds = List.of(event1.getId(), event2.getId());

        List<Event> events = eventRepository.findByIdIn(eventIds);

        assertEquals(2, events.size());
        assertTrue(events.stream().anyMatch(e -> e.getTitle().equals("Rock Concert")));
        assertTrue(events.stream().anyMatch(e -> e.getTitle().equals("Theater Play")));
    }

    @Test
    void participationRequestRepository_saveAndFind_shouldWork() {
        ParticipationRequest request = ParticipationRequest.builder()
                .event(event1)
                .requester(user2)
                .status(RequestStatus.PENDING)
                .created(now)
                .build();

        ParticipationRequest savedRequest = requestRepository.save(request);
        ParticipationRequest foundRequest = requestRepository.findById(savedRequest.getId()).orElse(null);

        assertNotNull(foundRequest);
        assertEquals(event1.getId(), foundRequest.getEvent().getId());
        assertEquals(user2.getId(), foundRequest.getRequester().getId());
        assertEquals(RequestStatus.PENDING, foundRequest.getStatus());
    }

    @Test
    void participationRequestRepository_findByRequesterId_shouldReturnUserRequests() {
        ParticipationRequest request1 = ParticipationRequest.builder()
                .event(event1)
                .requester(user2)
                .status(RequestStatus.PENDING)
                .created(now)
                .build();
        entityManager.persist(request1);

        ParticipationRequest request2 = ParticipationRequest.builder()
                .event(event3)
                .requester(user2)
                .status(RequestStatus.CONFIRMED)
                .created(now.minusHours(1))
                .build();
        entityManager.persist(request2);

        List<ParticipationRequest> requests = requestRepository.findByRequesterId(user2.getId());

        assertEquals(2, requests.size());
        assertTrue(requests.stream().allMatch(r -> r.getRequester().getId().equals(user2.getId())));
    }

    @Test
    void participationRequestRepository_findByEventId_shouldReturnEventRequests() {
        ParticipationRequest request1 = ParticipationRequest.builder()
                .event(event1)
                .requester(user2)
                .status(RequestStatus.PENDING)
                .created(now)
                .build();
        entityManager.persist(request1);

        List<ParticipationRequest> requests = requestRepository.findByEventId(event1.getId());

        assertEquals(1, requests.size());
        assertEquals(event1.getId(), requests.get(0).getEvent().getId());
        assertEquals(user2.getId(), requests.get(0).getRequester().getId());
    }

    @Test
    void participationRequestRepository_findByEventIdAndStatus_shouldReturnFilteredRequests() {
        ParticipationRequest request1 = ParticipationRequest.builder()
                .event(event1)
                .requester(user2)
                .status(RequestStatus.PENDING)
                .created(now)
                .build();
        entityManager.persist(request1);

        ParticipationRequest request2 = ParticipationRequest.builder()
                .event(event1)
                .requester(user1)
                .status(RequestStatus.CONFIRMED)
                .created(now.minusHours(1))
                .build();
        entityManager.persist(request2);

        List<ParticipationRequest> pendingRequests = requestRepository.findByEventIdAndStatus(
                event1.getId(), RequestStatus.PENDING);
        List<ParticipationRequest> confirmedRequests = requestRepository.findByEventIdAndStatus(
                event1.getId(), RequestStatus.CONFIRMED);

        assertEquals(1, pendingRequests.size());
        assertEquals(RequestStatus.PENDING, pendingRequests.get(0).getStatus());

        assertEquals(1, confirmedRequests.size());
        assertEquals(RequestStatus.CONFIRMED, confirmedRequests.get(0).getStatus());
    }

    @Test
    void participationRequestRepository_findByEventIdAndRequesterId_shouldReturnSpecificRequest() {
        ParticipationRequest request = ParticipationRequest.builder()
                .event(event1)
                .requester(user2)
                .status(RequestStatus.PENDING)
                .created(now)
                .build();
        entityManager.persist(request);

        var foundRequest = requestRepository.findByEventIdAndRequesterId(event1.getId(), user2.getId());

        assertTrue(foundRequest.isPresent());
        assertEquals(event1.getId(), foundRequest.get().getEvent().getId());
        assertEquals(user2.getId(), foundRequest.get().getRequester().getId());
    }

    @Test
    void participationRequestRepository_countByEventIdAndStatus_shouldReturnCorrectCount() {
        ParticipationRequest request1 = ParticipationRequest.builder()
                .event(event1)
                .requester(user2)
                .status(RequestStatus.CONFIRMED)
                .created(now)
                .build();
        entityManager.persist(request1);

        ParticipationRequest request2 = ParticipationRequest.builder()
                .event(event1)
                .requester(user1)
                .status(RequestStatus.CONFIRMED)
                .created(now.minusHours(1))
                .build();
        entityManager.persist(request2);

        ParticipationRequest request3 = ParticipationRequest.builder()
                .event(event1)
                .requester(new User())
                .status(RequestStatus.PENDING)
                .created(now.minusHours(2))
                .build();
        entityManager.persist(request3);

        Long confirmedCount = requestRepository.countByEventIdAndStatus(event1.getId(), RequestStatus.CONFIRMED);
        Long pendingCount = requestRepository.countByEventIdAndStatus(event1.getId(), RequestStatus.PENDING);

        assertEquals(2L, confirmedCount);
        assertEquals(1L, pendingCount);
    }

    @Test
    void compilationRepository_saveAndFind_shouldWork() {
        Compilation compilation = Compilation.builder()
                .events(new java.util.HashSet<>(List.of(event1, event2)))
                .pinned(true)
                .title("Test Compilation")
                .build();

        Compilation savedCompilation = compilationRepository.save(compilation);
        Compilation foundCompilation = compilationRepository.findById(savedCompilation.getId()).orElse(null);

        assertNotNull(foundCompilation);
        assertEquals("Test Compilation", foundCompilation.getTitle());
        assertTrue(foundCompilation.getPinned());
        assertEquals(2, foundCompilation.getEvents().size());
    }

    @Test
    void compilationRepository_findByPinned_shouldReturnFilteredCompilations() {
        Compilation pinnedCompilation = Compilation.builder()
                .events(new java.util.HashSet<>())
                .pinned(true)
                .title("Pinned Compilation")
                .build();
        entityManager.persist(pinnedCompilation);

        Compilation notPinnedCompilation = Compilation.builder()
                .events(new java.util.HashSet<>())
                .pinned(false)
                .title("Not Pinned Compilation")
                .build();
        entityManager.persist(notPinnedCompilation);

        List<Compilation> pinnedCompilations = compilationRepository.findByPinned(
                true, PageRequest.of(0, 10));
        List<Compilation> notPinnedCompilations = compilationRepository.findByPinned(
                false, PageRequest.of(0, 10));

        assertEquals(1, pinnedCompilations.size());
        assertEquals("Pinned Compilation", pinnedCompilations.get(0).getTitle());

        assertEquals(1, notPinnedCompilations.size());
        assertEquals("Not Pinned Compilation", notPinnedCompilations.get(0).getTitle());
    }

    @Test
    void compilationRepository_findAll_shouldReturnAllCompilations() {
        Compilation compilation1 = Compilation.builder()
                .events(new java.util.HashSet<>())
                .pinned(true)
                .title("Compilation 1")
                .build();
        entityManager.persist(compilation1);

        Compilation compilation2 = Compilation.builder()
                .events(new java.util.HashSet<>())
                .pinned(false)
                .title("Compilation 2")
                .build();
        entityManager.persist(compilation2);

        List<Compilation> compilations = compilationRepository.findAll();

        assertEquals(2, compilations.size());
    }

    @Test
    void eventRepository_findEventsPublic_withCategoriesFilter_shouldReturnEventsInCategories() {
        List<Long> categories = List.of(category1.getId());
        Boolean onlyAvailable = false;

        List<Event> events = eventRepository.findEventsPublic(
                null, categories, null, now.minusDays(1), now.plusDays(10),
                onlyAvailable, PageRequest.of(0, 10));

        assertEquals(2, events.size());
        assertTrue(events.stream().allMatch(e -> e.getCategory().getId().equals(category1.getId())));
    }

    @Test
    void eventRepository_findEventsPublic_withPaidFilter_shouldReturnPaidEvents() {
        Boolean paid = true;
        Boolean onlyAvailable = false;

        List<Event> events = eventRepository.findEventsPublic(
                null, null, paid, now.minusDays(1), now.plusDays(10),
                onlyAvailable, PageRequest.of(0, 10));

        assertTrue(events.size() >= 1);
        assertTrue(events.stream().allMatch(Event::getPaid));
    }

    @Test
    void eventRepository_findEventsPublic_withDateRange_shouldReturnEventsInRange() {
        LocalDateTime rangeStart = now.plusDays(1).minusHours(1);
        LocalDateTime rangeEnd = now.plusDays(2).plusHours(1);
        Boolean onlyAvailable = false;

        List<Event> events = eventRepository.findEventsPublic(
                null, null, null, rangeStart, rangeEnd,
                onlyAvailable, PageRequest.of(0, 10));

        assertEquals(2, events.size());
        assertTrue(events.stream().anyMatch(e -> e.getTitle().equals("Rock Concert")));
        assertTrue(events.stream().anyMatch(e -> e.getTitle().equals("Theater Play")));
    }

    @Test
    void eventRepository_findEventsPublic_withoutRangeEnd_shouldReturnEventsAfterRangeStart() {
        LocalDateTime rangeStart = now.plusDays(2).minusHours(1);
        Boolean onlyAvailable = false;

        List<Event> events = eventRepository.findEventsPublic(
                null, null, null, rangeStart, null,
                onlyAvailable, PageRequest.of(0, 10));

        assertEquals(2, events.size());
        assertTrue(events.stream().anyMatch(e -> e.getTitle().equals("Theater Play")));
        assertTrue(events.stream().anyMatch(e -> e.getTitle().equals("Jazz Concert")));
    }
}