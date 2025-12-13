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
        dto.setId(event.getId() != null ? event.getId() : 0L);
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
            UserShortDto initiatorDto = new UserShortDto();
            initiatorDto.setId(event.getInitiator().getId() != null ? event.getInitiator().getId() : 0L);
            initiatorDto.setName(event.getInitiator().getName() != null ? event.getInitiator().getName() : "");
            dto.setInitiator(initiatorDto);
        }

        if (event.getLocation() != null) {
            LocationDto locationDto = new LocationDto();
            locationDto.setLat(event.getLocation().getLat() != null ? event.getLocation().getLat() : 0f);
            locationDto.setLon(event.getLocation().getLon() != null ? event.getLocation().getLon() : 0f);
            dto.setLocation(locationDto);
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
        dto.setId(event.getId() != null ? event.getId() : 0L);
        dto.setAnnotation(event.getAnnotation() != null ? event.getAnnotation() : "");
        dto.setEventDate(event.getEventDate());
        dto.setPaid(event.getPaid() != null ? event.getPaid() : false);
        dto.setTitle(event.getTitle() != null ? event.getTitle() : "");

        if (event.getCategory() != null) {
            dto.setCategory(CategoryMapper.toCategoryDto(event.getCategory()));
        }

        if (event.getInitiator() != null) {
            UserShortDto initiatorDto = new UserShortDto();
            initiatorDto.setId(event.getInitiator().getId() != null ? event.getInitiator().getId() : 0L);
            initiatorDto.setName(event.getInitiator().getName() != null ? event.getInitiator().getName() : "");
            dto.setInitiator(initiatorDto);
        }

        dto.setConfirmedRequests(event.getConfirmedRequests() != null ? event.getConfirmedRequests().longValue() : 0L);
        dto.setViews(event.getViews() != null ? event.getViews() : 0L);

        return dto;
    }
}