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

    @Override
    public List<EventFullDto> getEventsByAdmin(List<Long> users, List<String> states, List<Long> categories,
                                               String rangeStart, String rangeEnd, Integer from, Integer size) {
        validatePagination(from, size);

        PageRequest page = PageRequest.of(from / size, size);

        List<EventState> eventStates = null;
        if (states != null && !states.isEmpty()) {
            try {
                eventStates = states.stream()
                        .map(EventState::valueOf)
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                throw new ValidationException("Invalid state value in states parameter");
            }
        }

        LocalDateTime start = parseDateTime(rangeStart);
        LocalDateTime end = parseDateTime(rangeEnd);

        List<Event> events = eventRepository.findEventsByAdmin(users, eventStates, categories, start, end, page);

        Map<Long, Long> views = statsService.getViews(events.stream()
                .map(Event::getId)
                .collect(Collectors.toList()));

        return events.stream()
                .map(event -> {
                    event.setViews(views.getOrDefault(event.getId(), 0L));
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
            LocalDateTime newEventDate = updateRequest.getEventDate();
            event.setEventDate(newEventDate);
        }

        updateEventFields(event, updateRequest);
        Event updatedEvent = eventRepository.save(event);

        Long views = statsService.getViews(List.of(eventId)).getOrDefault(eventId, 0L);
        updatedEvent.setViews(views);

        return EventMapper.toEventFullDto(updatedEvent);
    }

    @Override
    public List<EventShortDto> getEventsByUser(Long userId, Integer from, Integer size) {
        validatePagination(from, size);

        PageRequest page = PageRequest.of(from / size, size);
        List<Event> events = eventRepository.findByInitiatorId(userId, page);

        Map<Long, Long> views = statsService.getViews(events.stream()
                .map(Event::getId)
                .collect(Collectors.toList()));

        return events.stream()
                .map(event -> {
                    event.setViews(views.getOrDefault(event.getId(), 0L));
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

        Event savedEvent = eventRepository.save(event);
        return EventMapper.toEventFullDto(savedEvent);
    }

    @Override
    public EventFullDto getEventByUser(Long userId, Long eventId) {
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        Long views = statsService.getViews(List.of(eventId)).getOrDefault(eventId, 0L);
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
            event.setEventDate(updateRequest.getEventDate());
        }

        updateEventFields(event, updateRequest);
        Event updatedEvent = eventRepository.save(event);

        Long views = statsService.getViews(List.of(eventId)).getOrDefault(eventId, 0L);
        updatedEvent.setViews(views);

        return EventMapper.toEventFullDto(updatedEvent);
    }

    @Override
    public List<EventShortDto> getEventsPublic(String text, List<Long> categories, Boolean paid,
                                               String rangeStart, String rangeEnd, Boolean onlyAvailable,
                                               String sort, Integer from, Integer size, String ip) {
        validatePagination(from, size);

        if (sort != null && !sort.equals("EVENT_DATE") && !sort.equals("VIEWS")) {
            throw new IllegalArgumentException("Invalid sort parameter: " + sort);
        }

        if (text != null && text.length() > 7000) {
            throw new IllegalArgumentException("Text parameter is too long");
        }

        statsService.saveHit("/events", ip);

        PageRequest page;
        if ("EVENT_DATE".equals(sort)) {
            page = PageRequest.of(from / size, size, Sort.by("eventDate").descending());
        } else if ("VIEWS".equals(sort)) {
            page = PageRequest.of(from / size, size);
        } else {
            page = PageRequest.of(from / size, size);
        }

        LocalDateTime start = parseDateTime(rangeStart);
        LocalDateTime end = parseDateTime(rangeEnd);

        if (start == null) {
            start = LocalDateTime.now();
        }

        List<Event> events = eventRepository.findEventsPublic(text, categories, paid, start, end, onlyAvailable, page);

        Map<Long, Long> views = statsService.getViews(events.stream()
                .map(Event::getId)
                .collect(Collectors.toList()));

        List<EventShortDto> result = events.stream()
                .map(event -> {
                    event.setViews(views.getOrDefault(event.getId(), 0L));
                    return EventMapper.toEventShortDto(event);
                })
                .collect(Collectors.toList());

        if ("VIEWS".equals(sort)) {
            result.sort(Comparator.comparing(EventShortDto::getViews).reversed());
        }

        return result;
    }

    @Override
    public EventFullDto getEventPublic(Long eventId, String ip) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Event with id=" + eventId + " was not found");
        }

        statsService.saveHit("/events/" + eventId, ip);

        Long views = statsService.getViews(List.of(eventId)).getOrDefault(eventId, 0L);
        event.setViews(views);

        return EventMapper.toEventFullDto(event);
    }

    private LocalDateTime parseDateTime(String dateTime) {
        if (dateTime == null || dateTime.trim().isEmpty()) {
            return null;
        }

        try {
            // Сначала пробуем ISO формат
            return LocalDateTime.parse(dateTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException e1) {
            try {
                // Потом пробуем кастомный формат
                return LocalDateTime.parse(dateTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (DateTimeParseException e2) {
                throw new IllegalArgumentException("Invalid date format: " + dateTime);
            }
        }
    }

    private void validatePagination(Integer from, Integer size) {
        if (from < 0) {
            throw new IllegalArgumentException("Parameter 'from' must be greater than or equal to 0");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Parameter 'size' must be greater than 0");
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