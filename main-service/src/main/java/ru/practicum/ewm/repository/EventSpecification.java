package ru.practicum.ewm.repository;

import org.springframework.data.jpa.domain.Specification;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.EventState;

import java.time.LocalDateTime;
import java.util.List;

public class EventSpecification {

    public static Specification<Event> hasState(EventState state) {
        return (root, query, cb) ->
                cb.equal(root.get("state"), state);
    }

    public static Specification<Event> initiatorIn(List<Long> users) {
        return (root, query, cb) ->
                root.get("initiator").get("id").in(users);
    }

    public static Specification<Event> stateIn(List<EventState> states) {
        return (root, query, cb) ->
                root.get("state").in(states);
    }

    public static Specification<Event> categoryIn(List<Long> categories) {
        return (root, query, cb) ->
                root.get("category").get("id").in(categories);
    }

    public static Specification<Event> paid(Boolean paid) {
        return (root, query, cb) ->
                cb.equal(root.get("paid"), paid);
    }

    public static Specification<Event> dateFrom(LocalDateTime start) {
        return (root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("eventDate"), start);
    }

    public static Specification<Event> dateTo(LocalDateTime end) {
        return (root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("eventDate"), end);
    }

    public static Specification<Event> textSearch(String text) {
        String pattern = "%" + text.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("annotation")), pattern),
                cb.like(cb.lower(root.get("description")), pattern)
        );
    }
}
