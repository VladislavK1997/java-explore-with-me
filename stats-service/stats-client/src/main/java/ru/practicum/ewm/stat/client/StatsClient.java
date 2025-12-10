package ru.practicum.ewm.stat.client;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import ru.practicum.ewm.stat.dto.EndpointHitDto;
import ru.practicum.ewm.stat.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatsClient {
    private final RestTemplate rest;
    private final String serverUrl;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public StatsClient(String serverUrl) {
        this.rest = new RestTemplate();
        this.serverUrl = serverUrl;
    }

    public void hit(EndpointHitDto endpointHitDto) {
        makeAndSendRequest(HttpMethod.POST, "/hit", null, endpointHitDto);
    }

    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end,
                                       @Nullable List<String> uris,
                                       @Nullable Boolean unique) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("start", start.format(FORMATTER));
        parameters.put("end", end.format(FORMATTER));

        StringBuilder url = new StringBuilder("/stats?start={start}&end={end}");

        if (uris != null && !uris.isEmpty()) {
            parameters.put("uris", String.join(",", uris));
            url.append("&uris={uris}");
        }

        if (unique != null) {
            parameters.put("unique", unique);
            url.append("&unique={unique}");
        }

        ResponseEntity<ViewStatsDto[]> response = rest.getForEntity(
                serverUrl + url,
                ViewStatsDto[].class,
                parameters
        );

        return response.getBody() != null ? List.of(response.getBody()) : List.of();
    }

    private <T> ResponseEntity<Object> makeAndSendRequest(HttpMethod method, String path,
                                                          @Nullable Map<String, Object> parameters,
                                                          @Nullable T body) {
        HttpEntity<T> requestEntity = new HttpEntity<>(body);

        ResponseEntity<Object> response;
        try {
            if (parameters != null) {
                response = rest.exchange(serverUrl + path, method, requestEntity, Object.class, parameters);
            } else {
                response = rest.exchange(serverUrl + path, method, requestEntity, Object.class);
            }
        } catch (HttpStatusCodeException e) {
            throw new RuntimeException("Request failed with status: " + e.getStatusCode(), e);
        }

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Request failed with status: " + response.getStatusCode());
        }

        return response;
    }
}