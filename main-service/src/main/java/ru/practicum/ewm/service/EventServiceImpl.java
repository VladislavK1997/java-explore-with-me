package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.*;
import ru.practicum.ewm.exception.*;
import ru.practicum.ewm.mapper.EventMapper;
import ru.practicum.ewm.model.*;
import ru.practicum.ewm.repository.CategoryRepository;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.UserRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final StatsService statsService;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Transactional(readOnly = true)
    public List<EventFullDto> getEventsByAdmin(List<Long> users, List<String> states, List<Long> categories,
                                               String rangeStart, String rangeEnd, Integer from, Integer size) {
        final Integer finalFrom = (from == null) ? 0 : from;
        final Integer finalSize = (size == null) ? 10 : size;

        if (finalFrom < 0) {
            throw new ValidationException("Parameter 'from' must be greater than or equal to 0");
        }
        if (finalSize <= 0) {
            throw new ValidationException("Parameter 'size' must be greater than 0");
        }

        int pageNumber = 0;
        if (finalSize > 0) {
            pageNumber = finalFrom / finalSize;
        }

        PageRequest page = PageRequest.of(pageNumber, finalSize, Sort.by("id").ascending());

        List<EventState> eventStates = null;
        if (states != null && !states.isEmpty()) {
            eventStates = new ArrayList<>();
            for (String state : states) {
                if (state != null && !state.trim().isEmpty()) {
                    try {
                        eventStates.add(EventState.valueOf(state.toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        throw new ValidationException("Invalid state value in states parameter");
                    }
                }
            }
        }

        LocalDateTime start = parseDateTime(rangeStart);
        LocalDateTime end = parseDateTime(rangeEnd);

        if (start != null && end != null && start.isAfter(end)) {
            throw new ValidationException("rangeStart must be before rangeEnd");
        }

        List<Event> events;
        try {
            events = eventRepository.findEventsByAdmin(
                    (users != null && !users.isEmpty()) ? users : null,
                    (eventStates != null && !eventStates.isEmpty()) ? eventStates : null,
                    (categories != null && !categories.isEmpty()) ? categories : null,
                    start, end, page
            );
        } catch (Exception e) {
            log.error("Error fetching events in getEventsByAdmin: {}", e.getMessage(), e);
            return Collections.emptyList();
        }

        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Long> views;
        try {
            views = statsService.getViews(events.stream()
                    .map(Event::getId)
                    .collect(Collectors.toList()));
        } catch (Exception e) {
            log.error("Error getting views in getEventsByAdmin: {}", e.getMessage(), e);
            views = new HashMap<>();
        }

        final Map<Long, Long> finalViews = views;

        return events.stream()
                .map(event -> {
                    event.setViews(finalViews.getOrDefault(event.getId(), 0L));
                    return EventMapper.toEventFullDto(event);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest updateRequest) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (updateRequest.getStateAction() != null) {
            switch (updateRequest.getStateAction()) {
                case "PUBLISH_EVENT":
                    if (event.getState() != EventState.PENDING) {
                        throw new ConflictException("Cannot publish the event because it's not in the right state: " + event.getState());
                    }
                    if (event.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
                        throw new ConflictException("Cannot publish the event because the event date is too soon");
                    }
                    event.setState(EventState.PUBLISHED);
                    event.setPublishedOn(LocalDateTime.now());
                    break;
                case "REJECT_EVENT":
                    if (event.getState() == EventState.PUBLISHED) {
                        throw new ConflictException("Cannot reject the event because it's already published");
                    }
                    event.setState(EventState.CANCELED);
                    break;
                default:
                    throw new ValidationException("Invalid state action: " + updateRequest.getStateAction());
            }
        }

        if (updateRequest.getEventDate() != null) {
            if (updateRequest.getEventDate().isBefore(LocalDateTime.now())) {
                throw new ValidationException("Event date must be in the future");
            }
        }

        updateEventFields(event, updateRequest);
        Event updatedEvent = eventRepository.save(event);

        Long views;
        try {
            views = statsService.getViews(List.of(eventId)).getOrDefault(eventId, 0L);
        } catch (Exception e) {
            log.error("Error getting views in updateEventByAdmin: {}", e.getMessage(), e);
            views = 0L;
        }
        updatedEvent.setViews(views);

        return EventMapper.toEventFullDto(updatedEvent);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getEventsByUser(Long userId, Integer from, Integer size) {
        final Integer finalFrom = (from == null) ? 0 : from;
        final Integer finalSize = (size == null) ? 10 : size;

        if (finalFrom < 0) {
            throw new ValidationException("Parameter 'from' must be greater than or equal to 0");
        }
        if (finalSize <= 0) {
            throw new ValidationException("Parameter 'size' must be greater than 0");
        }

        int pageNumber = 0;
        if (finalSize > 0) {
            pageNumber = finalFrom / finalSize;
        }

        PageRequest page = PageRequest.of(pageNumber, finalSize, Sort.by("eventDate").descending());

        List<Event> events;
        try {
            events = eventRepository.findByInitiatorId(userId, page);
        } catch (Exception e) {
            log.error("Error fetching events in getEventsByUser: {}", e.getMessage(), e);
            return Collections.emptyList();
        }

        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Long> views;
        try {
            views = statsService.getViews(events.stream()
                    .map(Event::getId)
                    .collect(Collectors.toList()));
        } catch (Exception e) {
            log.error("Error getting views in getEventsByUser: {}", e.getMessage(), e);
            views = new HashMap<>();
        }

        final Map<Long, Long> finalViews = views;

        return events.stream()
                .map(event -> {
                    event.setViews(finalViews.getOrDefault(event.getId(), 0L));
                    return EventMapper.toEventShortDto(event);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto newEventDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));

        Category category = categoryRepository.findById(newEventDto.getCategory())
                .orElseThrow(() -> new NotFoundException("Category with id=" + newEventDto.getCategory() + " was not found"));

        if (newEventDto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ValidationException("Field: eventDate. Error: должно содержать дату, которая еще не наступила. Value: " + newEventDto.getEventDate());
        }

        Event event = EventMapper.toEvent(newEventDto);
        event.setInitiator(user);
        event.setCategory(category);
        event.setConfirmedRequests(0);
        event.setViews(0L);

        Event savedEvent = eventRepository.save(event);
        return EventMapper.toEventFullDto(savedEvent);
    }

    @Override
    @Transactional(readOnly = true)
    public EventFullDto getEventByUser(Long userId, Long eventId) {
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        Long views;
        try {
            views = statsService.getViews(List.of(eventId)).getOrDefault(eventId, 0L);
        } catch (Exception e) {
            log.error("Error getting views in getEventByUser: {}", e.getMessage(), e);
            views = 0L;
        }
        event.setViews(views);

        return EventMapper.toEventFullDto(event);
    }

    @Override
    @Transactional
    public EventFullDto updateEventByUser(Long userId, Long eventId, UpdateEventUserRequest updateRequest) {
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Only pending or canceled events can be changed");
        }

        if (updateRequest.getStateAction() != null) {
            switch (updateRequest.getStateAction()) {
                case "SEND_TO_REVIEW":
                    event.setState(EventState.PENDING);
                    break;
                case "CANCEL_REVIEW":
                    event.setState(EventState.CANCELED);
                    break;
                default:
                    throw new ValidationException("Invalid state action: " + updateRequest.getStateAction());
            }
        }

        if (updateRequest.getEventDate() != null) {
            if (updateRequest.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
                throw new ValidationException("Field: eventDate. Error: должно содержать дату, которая еще не наступила. Value: " + updateRequest.getEventDate());
            }
        }

        updateEventFields(event, updateRequest);
        Event updatedEvent = eventRepository.save(event);

        Long views;
        try {
            views = statsService.getViews(List.of(eventId)).getOrDefault(eventId, 0L);
        } catch (Exception e) {
            log.error("Error getting views in updateEventByUser: {}", e.getMessage(), e);
            views = 0L;
        }
        updatedEvent.setViews(views);

        return EventMapper.toEventFullDto(updatedEvent);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getEventsPublic(String text, List<Long> categories, Boolean paid,
                                               String rangeStart, String rangeEnd, Boolean onlyAvailable,
                                               String sort, Integer from, Integer size, String ip) {
        try {
            log.info("Getting events public with params: text={}, categories={}, paid={}, rangeStart={}, " +
                            "rangeEnd={}, onlyAvailable={}, sort={}, from={}, size={}, ip={}",
                    text, categories, paid, rangeStart, rangeEnd, onlyAvailable, sort, from, size, ip);

            final Integer finalFrom = from != null ? from : 0;
            final Integer finalSize = size != null ? size : 10;
            final Boolean finalOnlyAvailable = onlyAvailable != null ? onlyAvailable : false;

            if (finalFrom < 0) {
                throw new ValidationException("Parameter 'from' must be greater than or equal to 0");
            }
            if (finalSize <= 0) {
                throw new ValidationException("Parameter 'size' must be greater than 0");
            }

            String finalSort = null;
            if (sort != null && !sort.trim().isEmpty()) {
                finalSort = sort.trim().toUpperCase();
                if (!"EVENT_DATE".equals(finalSort) && !"VIEWS".equals(finalSort)) {
                    throw new ValidationException("Invalid sort parameter: " + sort);
                }
            }

            LocalDateTime start = null;
            if (rangeStart != null && !rangeStart.trim().isEmpty()) {
                try {
                    start = LocalDateTime.parse(rangeStart.trim(), FORMATTER);
                } catch (DateTimeParseException e) {
                    throw new ValidationException("Invalid rangeStart format. Expected: yyyy-MM-dd HH:mm:ss");
                }
            }

            LocalDateTime end = null;
            if (rangeEnd != null && !rangeEnd.trim().isEmpty()) {
                try {
                    end = LocalDateTime.parse(rangeEnd.trim(), FORMATTER);
                } catch (DateTimeParseException e) {
                    throw new ValidationException("Invalid rangeEnd format. Expected: yyyy-MM-dd HH:mm:ss");
                }
            }

            if (start == null) {
                start = LocalDateTime.now();
            }

            if (end != null && start.isAfter(end)) {
                throw new ValidationException("rangeStart must be before rangeEnd");
            }

            int pageNumber = 0;
            if (finalSize > 0) {
                pageNumber = finalFrom / finalSize;
            }

            PageRequest page;
            if (finalSort != null && "EVENT_DATE".equals(finalSort)) {
                page = PageRequest.of(pageNumber, finalSize, Sort.by("eventDate").descending());
            } else {
                page = PageRequest.of(pageNumber, finalSize);
            }

            List<Event> events = Collections.emptyList();
            try {
                String finalText = (text != null && !text.trim().isEmpty()) ? text.trim() : null;
                List<Long> finalCategories = (categories != null && !categories.isEmpty()) ? categories : null;

                events = eventRepository.findEventsPublic(
                        finalText,
                        finalCategories,
                        paid,
                        start,
                        end,
                        EventState.PUBLISHED,
                        page
                );

            } catch (Exception e) {
                log.error("Repository error in getEventsPublic: {}", e.getMessage(), e);
                return Collections.emptyList();
            }

            if (Boolean.TRUE.equals(finalOnlyAvailable)) {
                events = events.stream()
                        .filter(event -> event.getParticipantLimit() == 0 ||
                                (event.getConfirmedRequests() != null &&
                                        event.getConfirmedRequests() < event.getParticipantLimit()))
                        .collect(Collectors.toList());
            }

            if (events.isEmpty()) {
                return Collections.emptyList();
            }

            Map<Long, Long> views = new HashMap<>();
            try {
                List<Long> eventIds = events.stream()
                        .map(Event::getId)
                        .collect(Collectors.toList());
                views = statsService.getViews(eventIds);
            } catch (Exception e) {
                log.error("Error getting views in getEventsPublic: {}", e.getMessage(), e);
            }

            final Map<Long, Long> finalViews = views;
            List<EventShortDto> result = events.stream()
                    .map(event -> {
                        try {
                            EventShortDto dto = EventMapper.toEventShortDto(event);
                            if (dto != null) {
                                dto.setViews(finalViews.getOrDefault(event.getId(), 0L));
                            }
                            return dto;
                        } catch (Exception e) {
                            log.error("Error mapping event {}: {}", event.getId(), e.getMessage());
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (finalSort != null && "VIEWS".equals(finalSort)) {
                result.sort(Comparator.comparing(EventShortDto::getViews,
                        Comparator.nullsLast(Long::compareTo)).reversed());
            }

            try {
                statsService.saveHit("/events", ip);
            } catch (Exception e) {
                log.error("Failed to save hit in getEventsPublic: {}", e.getMessage(), e);
            }

            return result;

        } catch (ValidationException e) {
            log.warn("Validation error in getEventsPublic: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error in getEventsPublic: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public EventFullDto getEventPublic(Long eventId, String ip) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Event with id=" + eventId + " was not found");
        }

        Long views;
        try {
            views = statsService.getViews(List.of(eventId)).getOrDefault(eventId, 0L);
        } catch (Exception e) {
            log.error("Error getting views in getEventPublic: {}", e.getMessage(), e);
            views = 0L;
        }
        event.setViews(views);

        try {
            statsService.saveHit("/events/" + eventId, ip);
        } catch (Exception e) {
            log.error("Failed to save hit in getEventPublic: {}", e.getMessage());
        }

        return EventMapper.toEventFullDto(event);
    }

    private LocalDateTime parseDateTime(String dateTime) {
        if (dateTime == null || dateTime.trim().isEmpty()) {
            return null;
        }

        try {
            return LocalDateTime.parse(dateTime.trim(), FORMATTER);
        } catch (DateTimeParseException e) {
            throw new ValidationException("Invalid date format. Expected format: yyyy-MM-dd HH:mm:ss");
        }
    }

    private void updateEventFields(Event event, Object updateRequest) {
        if (updateRequest instanceof UpdateEventAdminRequest) {
            UpdateEventAdminRequest adminRequest = (UpdateEventAdminRequest) updateRequest;
            updateCommonFields(event, adminRequest.getAnnotation(), adminRequest.getCategory(),
                    adminRequest.getDescription(), adminRequest.getEventDate(), adminRequest.getLocation(),
                    adminRequest.getPaid(), adminRequest.getParticipantLimit(),
                    adminRequest.getRequestModeration(), adminRequest.getTitle());
        } else if (updateRequest instanceof UpdateEventUserRequest) {
            UpdateEventUserRequest userRequest = (UpdateEventUserRequest) updateRequest;
            updateCommonFields(event, userRequest.getAnnotation(), userRequest.getCategory(),
                    userRequest.getDescription(), userRequest.getEventDate(), userRequest.getLocation(),
                    userRequest.getPaid(), userRequest.getParticipantLimit(),
                    userRequest.getRequestModeration(), userRequest.getTitle());
        }
    }

    private void updateCommonFields(Event event, String annotation, Long categoryId, String description,
                                    LocalDateTime eventDate, LocationDto location, Boolean paid,
                                    Integer participantLimit, Boolean requestModeration, String title) {
        if (annotation != null && !annotation.isEmpty()) {
            event.setAnnotation(annotation);
        }
        if (categoryId != null) {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new NotFoundException("Category with id=" + categoryId + " was not found"));
            event.setCategory(category);
        }
        if (description != null && !description.isEmpty()) {
            event.setDescription(description);
        }
        if (eventDate != null) {
            event.setEventDate(eventDate);
        }
        if (location != null) {
            event.setLocation(new Location(location.getLat(), location.getLon()));
        }
        if (paid != null) {
            event.setPaid(paid);
        }
        if (participantLimit != null) {
            event.setParticipantLimit(participantLimit);
        }
        if (requestModeration != null) {
            event.setRequestModeration(requestModeration);
        }
        if (title != null && !title.isEmpty()) {
            event.setTitle(title);
        }
    }
}