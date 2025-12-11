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
        return Event.builder()
                .annotation(newEventDto.getAnnotation())
                .description(newEventDto.getDescription())
                .eventDate(newEventDto.getEventDate())
                .location(new Location(newEventDto.getLocation().getLat(), newEventDto.getLocation().getLon()))
                .paid(newEventDto.getPaid())
                .participantLimit(newEventDto.getParticipantLimit())
                .requestModeration(newEventDto.getRequestModeration())
                .title(newEventDto.getTitle())
                .state(EventState.PENDING)
                .createdOn(LocalDateTime.now())
                .confirmedRequests(0)
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
                event.getPaid(),
                event.getParticipantLimit(),
                event.getPublishedOn(),
                event.getRequestModeration(),
                event.getState(),
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
                event.getPaid(),
                event.getTitle(),
                event.getViews() != null ? event.getViews() : 0L
        );
    }
}