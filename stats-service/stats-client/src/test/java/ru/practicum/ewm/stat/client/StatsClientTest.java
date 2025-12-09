package ru.practicum.ewm.stat.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import ru.practicum.ewm.stat.dto.EndpointHitDto;
import ru.practicum.ewm.stat.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatsClientTest {
    @Mock
    private RestTemplate restTemplate;

    private StatsClient statsClient;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @BeforeEach
    void setUp() {
        // Создаем StatsClient и подменяем restTemplate через рефлексию
        statsClient = new StatsClient("http://localhost:9090");
        try {
            var field = StatsClient.class.getDeclaredField("rest");
            field.setAccessible(true);
            field.set(statsClient, restTemplate);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void shouldMakeHitRequest() {
        EndpointHitDto hitDto = new EndpointHitDto(
                null,
                "test-app",
                "/test",
                "192.168.1.1",
                LocalDateTime.now()
        );

        // Мокаем успешный ответ
        when(restTemplate.exchange(
                eq("http://localhost:9090/hit"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Object.class)
        )).thenReturn(ResponseEntity.ok().build());

        statsClient.hit(hitDto);

        verify(restTemplate, times(1)).exchange(
                eq("http://localhost:9090/hit"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Object.class)
        );
    }

    @Test
    void shouldGetStats() {
        ViewStatsDto[] mockResponse = {
                new ViewStatsDto("app1", "/uri1", 10L),
                new ViewStatsDto("app2", "/uri2", 20L)
        };

        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();

        String expectedUrl = "http://localhost:9090/stats?start=" +
                start.format(formatter) + "&end=" + end.format(formatter);

        when(restTemplate.getForEntity(
                eq(expectedUrl),
                eq(ViewStatsDto[].class),
                any(Map.class)
        )).thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        List<ViewStatsDto> result = statsClient.getStats(start, end, null, false);

        assertEquals(2, result.size());
        assertEquals("app1", result.get(0).getApp());
        assertEquals("/uri1", result.get(0).getUri());
        assertEquals(10L, result.get(0).getHits());
    }

    @Test
    void shouldGetStatsWithUris() {
        ViewStatsDto[] mockResponse = {
                new ViewStatsDto("app1", "/events/1", 10L),
                new ViewStatsDto("app1", "/events/2", 5L)
        };

        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();
        List<String> uris = List.of("/events/1", "/events/2");

        String expectedUrl = "http://localhost:9090/stats?start=" +
                start.format(formatter) + "&end=" + end.format(formatter) + "&uris={uris}";

        when(restTemplate.getForEntity(
                eq(expectedUrl),
                eq(ViewStatsDto[].class),
                any(Map.class)
        )).thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        List<ViewStatsDto> result = statsClient.getStats(start, end, uris, false);

        assertEquals(2, result.size());
        assertEquals("/events/1", result.get(0).getUri());
    }

    @Test
    void shouldGetStatsWithUnique() {
        ViewStatsDto[] mockResponse = {
                new ViewStatsDto("app1", "/events/1", 5L) // Уникальные посещения
        };

        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();

        String expectedUrl = "http://localhost:9090/stats?start=" +
                start.format(formatter) + "&end=" + end.format(formatter) + "&unique={unique}";

        when(restTemplate.getForEntity(
                eq(expectedUrl),
                eq(ViewStatsDto[].class),
                any(Map.class)
        )).thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        List<ViewStatsDto> result = statsClient.getStats(start, end, null, true);

        assertEquals(1, result.size());
        assertEquals(5L, result.get(0).getHits());
    }

    @Test
    void shouldHandleHttpErrorGracefully() {
        EndpointHitDto hitDto = new EndpointHitDto(
                null,
                "test-app",
                "/test",
                "192.168.1.1",
                LocalDateTime.now()
        );

        // Мокаем исключение при вызове exchange
        when(restTemplate.exchange(
                eq("http://localhost:9090/hit"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Object.class)
        )).thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> statsClient.hit(hitDto)
        );

        assertTrue(exception.getMessage().contains("Request failed with status:"));
        assertTrue(exception.getMessage().contains("400"));
    }

    @Test
    void shouldReturnEmptyListWhenResponseBodyIsNull() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();

        String expectedUrl = "http://localhost:9090/stats?start=" +
                start.format(formatter) + "&end=" + end.format(formatter);

        when(restTemplate.getForEntity(
                eq(expectedUrl),
                eq(ViewStatsDto[].class),
                any(Map.class)
        )).thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        List<ViewStatsDto> result = statsClient.getStats(start, end, null, false);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldHandleNonSuccessfulResponse() {
        EndpointHitDto hitDto = new EndpointHitDto(
                null,
                "test-app",
                "/test",
                "192.168.1.1",
                LocalDateTime.now()
        );

        // Мокаем ответ с ошибкой 500
        when(restTemplate.exchange(
                eq("http://localhost:9090/hit"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Object.class)
        )).thenReturn(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> statsClient.hit(hitDto)
        );

        assertTrue(exception.getMessage().contains("Request failed with status: 500"));
    }

    @Test
    void shouldCreateStatsClientWithServerUrl() {
        StatsClient client = new StatsClient("http://example.com:9090");
        assertNotNull(client);

        // Проверяем, что RestTemplate был создан
        try {
            var field = StatsClient.class.getDeclaredField("rest");
            field.setAccessible(true);
            RestTemplate restTemplate = (RestTemplate) field.get(client);
            assertNotNull(restTemplate);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}