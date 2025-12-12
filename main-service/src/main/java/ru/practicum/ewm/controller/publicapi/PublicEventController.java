package ru.practicum.ewm.controller.publicapi;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.EventFullDto;
import ru.practicum.ewm.dto.EventShortDto;
import ru.practicum.ewm.service.EventService;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/events")
public class PublicEventController {
    private final EventService eventService;

    @GetMapping
    public List<EventShortDto> getEvents(@RequestParam(required = false)
                                         @Size(min = 1, max = 7000, message = "Text length must be between 1 and 7000 characters")
                                         String text,
                                         @RequestParam(required = false) List<Long> categories,
                                         @RequestParam(required = false) Boolean paid,
                                         @RequestParam(required = false)
                                         @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}", message = "Invalid date format. Expected format: yyyy-MM-dd HH:mm:ss")
                                         String rangeStart,
                                         @RequestParam(required = false)
                                         @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}", message = "Invalid date format. Expected format: yyyy-MM-dd HH:mm:ss")
                                         String rangeEnd,
                                         @RequestParam(required = false) Boolean onlyAvailable,
                                         @RequestParam(required = false)
                                         @Pattern(regexp = "EVENT_DATE|VIEWS", message = "Invalid sort parameter. Must be EVENT_DATE or VIEWS")
                                         String sort,
                                         @RequestParam(required = false, defaultValue = "0") @Min(0) Integer from,
                                         @RequestParam(required = false, defaultValue = "10") @Min(1) Integer size,
                                         HttpServletRequest request) {
        log.info("Getting events with filters: text={}, categories={}, paid={}, " +
                        "rangeStart={}, rangeEnd={}, onlyAvailable={}, sort={}, from={}, size={}",
                text, categories, paid, rangeStart, rangeEnd, onlyAvailable, sort, from, size);

        String ip = request.getRemoteAddr();
        return eventService.getEventsPublic(text, categories, paid, rangeStart, rangeEnd,
                onlyAvailable != null ? onlyAvailable : false, sort, from, size, ip);
    }

    @GetMapping("/{id}")
    public EventFullDto getEvent(@PathVariable Long id, HttpServletRequest request) {
        log.info("Getting event with id: {}", id);

        String ip = request.getRemoteAddr();
        return eventService.getEventPublic(id, ip);
    }
}