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
        if (newEventDto == null) {
            return null;
        }

        return Event.builder()
                .annotation(newEventDto.getAnnotation())
                .description(newEventDto.getDescription())
                .eventDate(newEventDto.getEventDate())
                .location(newEventDto.getLocation() != null ?
                        new Location(newEventDto.getLocation().getLat(), newEventDto.getLocation().getLon()) : null)
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
        return new EventFullDto(
                event.getId(),
                event.getAnnotation(),
                event.getCategory() != null ? CategoryMapper.toCategoryDto(event.getCategory()) : null,
                event.getConfirmedRequests() != null ? event.getConfirmedRequests().longValue() : 0L,
                event.getCreatedOn(),
                event.getDescription(),
                event.getEventDate(),
                event.getInitiator() != null ? new UserShortDto(event.getInitiator().getId(), event.getInitiator().getName()) : null,
                event.getLocation() != null ? new LocationDto(event.getLocation().getLat(), event.getLocation().getLon()) : null,
                event.getPaid() != null ? event.getPaid() : false,
                event.getParticipantLimit() != null ? event.getParticipantLimit() : 0,
                event.getPublishedOn(),
                event.getRequestModeration() != null ? event.getRequestModeration() : true,
                event.getState() != null ? event.getState() : EventState.PENDING,
                event.getTitle(),
                event.getViews() != null ? event.getViews() : 0L
        );
    }

    public static EventShortDto toEventShortDto(Event event) {
        if (event == null) {
            return null;
        }
        return new EventShortDto(
                event.getId(),
                event.getAnnotation(),
                event.getCategory() != null ? CategoryMapper.toCategoryDto(event.getCategory()) : null,
                event.getConfirmedRequests() != null ? event.getConfirmedRequests().longValue() : 0L,
                event.getEventDate(),
                event.getInitiator() != null ? new UserShortDto(event.getInitiator().getId(), event.getInitiator().getName()) : null,
                event.getPaid() != null ? event.getPaid() : false,
                event.getTitle(),
                event.getViews() != null ? event.getViews() : 0L
        );
    }
}