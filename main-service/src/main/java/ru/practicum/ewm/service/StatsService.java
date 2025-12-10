package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stat.client.StatsClient;
import ru.practicum.ewm.stat.dto.EndpointHitDto;
import ru.practicum.ewm.stat.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsService {
    private final StatsClient statsClient;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String APP_NAME = "ewm-main-service";

    public void saveHit(String uri, String ip) {
        EndpointHitDto hitDto = new EndpointHitDto(null, APP_NAME, uri, ip, LocalDateTime.now());
        try {
            statsClient.hit(hitDto);
            log.debug("Statistics saved for URI: {}", uri);
        } catch (Exception e) {
            log.error("Failed to save statistics for URI: {}", uri, e);
        }
    }

    public Map<Long, Long> getViews(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return new HashMap<>();
        }

        List<String> uris = eventIds.stream()
                .map(id -> "/events/" + id)
                .collect(Collectors.toList());

        LocalDateTime start = LocalDateTime.now().minusYears(1);
        LocalDateTime end = LocalDateTime.now().plusDays(1);

        try {
            List<ViewStatsDto> stats = statsClient.getStats(start, end, uris, true);

            Map<Long, Long> viewsMap = new HashMap<>();

            eventIds.forEach(id -> viewsMap.put(id, 0L));

            for (ViewStatsDto stat : stats) {
                String uri = stat.getUri();
                if (uri.startsWith("/events/")) {
                    String[] parts = uri.split("/");
                    if (parts.length > 2) {
                        try {
                            Long eventId = Long.parseLong(parts[2]);
                            if (eventIds.contains(eventId)) {
                                viewsMap.put(eventId, stat.getHits());
                            }
                        } catch (NumberFormatException e) {
                            log.debug("Invalid event ID in URI: {}", uri);
                        }
                    }
                }
            }

            return viewsMap;
        } catch (Exception e) {
            log.error("Failed to get views statistics: {}", e.getMessage());
            return new HashMap<>();
        }
    }
}