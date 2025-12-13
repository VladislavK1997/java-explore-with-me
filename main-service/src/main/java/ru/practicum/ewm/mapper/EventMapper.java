package ru.practicum.ewm.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.ewm.dto.*;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.EventState;
import ru.practicum.ewm.model.Location;

import java.time.LocalDateTime;

@UtilityClass
public class EventMapper {
    public static Event toEvent(NewEventDto newEventDto) {
        Location location = null;
        if (newEventDto.getLocation() != null) {
            location = new Location(
                    newEventDto.getLocation().getLat(),
                    newEventDto.getLocation().getLon()
            );
        }

        return Event.builder()
                .annotation(newEventDto.getAnnotation())
                .description(newEventDto.getDescription())
                .eventDate(newEventDto.getEventDate())
                .location(location)
                .paid(newEventDto.getPaid() != null ? newEventDto.getPaid() : false)
                .participantLimit(newEventDto.getParticipantLimit() != null ? newEventDto.getParticipantLimit() : 0)
                .requestModeration(newEventDto.getRequestModeration() != null ? newEventDto.getRequestModeration() : true)
                .title(newEventDto.getTitle())
                .state(EventState.PENDING)
                .createdOn(LocalDateTime.now())
                .confirmedRequests(0)
                .views(0L)
                .build();
    }

    public static EventFullDto toEventFullDto(Event event) {
        if (event == null) {
            return null;
        }

        EventFullDto dto = new EventFullDto();
        dto.setId(event.getId());
        dto.setAnnotation(event.getAnnotation() != null ? event.getAnnotation() : "");
        dto.setDescription(event.getDescription() != null ? event.getDescription() : "");
        dto.setEventDate(event.getEventDate());
        dto.setCreatedOn(event.getCreatedOn());
        dto.setPublishedOn(event.getPublishedOn());
        dto.setPaid(event.getPaid() != null ? event.getPaid() : false);
        dto.setParticipantLimit(event.getParticipantLimit() != null ? event.getParticipantLimit() : 0);
        dto.setRequestModeration(event.getRequestModeration() != null ? event.getRequestModeration() : true);
        dto.setState(event.getState() != null ? event.getState() : EventState.PENDING);
        dto.setTitle(event.getTitle() != null ? event.getTitle() : "");

        if (event.getCategory() != null) {
            dto.setCategory(CategoryMapper.toCategoryDto(event.getCategory()));
        }

        if (event.getInitiator() != null) {
            dto.setInitiator(new UserShortDto(event.getInitiator().getId(), event.getInitiator().getName()));
        }

        if (event.getLocation() != null) {
            dto.setLocation(new LocationDto(event.getLocation().getLat(), event.getLocation().getLon()));
        }

        dto.setConfirmedRequests(event.getConfirmedRequests() != null ? event.getConfirmedRequests().longValue() : 0L);
        dto.setViews(event.getViews() != null ? event.getViews() : 0L);

        return dto;
    }

    public static EventShortDto toEventShortDto(Event event) {
        if (event == null) {
            return null;
        }

        EventShortDto dto = new EventShortDto();
        dto.setId(event.getId());
        dto.setAnnotation(event.getAnnotation() != null ? event.getAnnotation() : "");
        dto.setEventDate(event.getEventDate());
        dto.setPaid(event.getPaid() != null ? event.getPaid() : false);
        dto.setTitle(event.getTitle() != null ? event.getTitle() : "");

        if (event.getCategory() != null) {
            dto.setCategory(CategoryMapper.toCategoryDto(event.getCategory()));
        }

        if (event.getInitiator() != null) {
            dto.setInitiator(new UserShortDto(event.getInitiator().getId(),
                    event.getInitiator().getName()));
        }

        dto.setConfirmedRequests(event.getConfirmedRequests() != null ?
                event.getConfirmedRequests().longValue() : 0L);
        dto.setViews(event.getViews() != null ? event.getViews() : 0L);

        return dto;
    }
}