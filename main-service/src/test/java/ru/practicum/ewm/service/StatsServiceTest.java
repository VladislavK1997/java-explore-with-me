package ru.practicum.ewm.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.stat.client.StatsClient;
import ru.practicum.ewm.stat.dto.EndpointHitDto;
import ru.practicum.ewm.stat.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    @Mock
    private StatsClient statsClient;

    @InjectMocks
    private StatsService statsService;

    private final LocalDateTime now = LocalDateTime.now();

    @Test
    void saveHit_ValidData_CallsStatsClient() {
        // Given
        String uri = "/events";
        String ip = "192.168.1.1";

        doNothing().when(statsClient).hit(any(EndpointHitDto.class));

        // When
        statsService.saveHit(uri, ip);

        // Then
        verify(statsClient, times(1)).hit(argThat(dto ->
                dto.getApp().equals("ewm-main-service") &&
                        dto.getUri().equals(uri) &&
                        dto.getIp().equals(ip) &&
                        dto.getTimestamp() != null
        ));
    }

    @Test
    void saveHit_StatsClientThrowsException_LogsError() {
        // Given
        String uri = "/events";
        String ip = "192.168.1.1";

        doThrow(new RuntimeException("Network error")).when(statsClient).hit(any());

        // When & Then - не должно выбрасывать исключение
        assertDoesNotThrow(() -> statsService.saveHit(uri, ip));
        verify(statsClient, times(1)).hit(any());
    }

    @Test
    void getViews_WithEventIds_ReturnsViewsMap() {
        // Given
        List<Long> eventIds = List.of(1L, 2L, 3L);
        List<String> expectedUris = List.of("/events/1", "/events/2", "/events/3");

        List<ViewStatsDto> stats = List.of(
                new ViewStatsDto("ewm-main-service", "/events/1", 100L),
                new ViewStatsDto("ewm-main-service", "/events/2", 200L),
                new ViewStatsDto("ewm-main-service", "/events/3", 300L)
        );

        when(statsClient.getStats(any(LocalDateTime.class), any(LocalDateTime.class), eq(expectedUris), eq(false)))
                .thenReturn(stats);

        // When
        Map<Long, Long> views = statsService.getViews(eventIds);

        // Then
        assertNotNull(views);
        assertEquals(3, views.size());
        assertEquals(100L, views.get(1L));
        assertEquals(200L, views.get(2L));
        assertEquals(300L, views.get(3L));
        verify(statsClient, times(1)).getStats(any(), any(), eq(expectedUris), eq(false));
    }

    @Test
    void getViews_EmptyEventIds_ReturnsEmptyMap() {
        // Given
        List<Long> eventIds = List.of();

        // When
        Map<Long, Long> views = statsService.getViews(eventIds);

        // Then
        assertNotNull(views);
        assertTrue(views.isEmpty());
        verify(statsClient, never()).getStats(any(), any(), any(), any());
    }

    @Test
    void getViews_NullEventIds_ReturnsEmptyMap() {
        // When
        Map<Long, Long> views = statsService.getViews(null);

        // Then
        assertNotNull(views);
        assertTrue(views.isEmpty());
        verify(statsClient, never()).getStats(any(), any(), any(), any());
    }

    @Test
    void getViews_StatsClientThrowsException_ReturnsEmptyMap() {
        // Given
        List<Long> eventIds = List.of(1L, 2L);

        when(statsClient.getStats(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Network error"));

        // When
        Map<Long, Long> views = statsService.getViews(eventIds);

        // Then
        assertNotNull(views);
        assertTrue(views.isEmpty());
        verify(statsClient, times(1)).getStats(any(), any(), any(), any());
    }

    @Test
    void getViews_InvalidUriInResponse_HandlesGracefully() {
        // Given
        List<Long> eventIds = List.of(1L);

        List<ViewStatsDto> stats = List.of(
                new ViewStatsDto("ewm-main-service", "invalid-uri", 100L), // Некорректный URI
                new ViewStatsDto("ewm-main-service", "/events/1", 200L)   // Корректный URI
        );

        when(statsClient.getStats(any(), any(), any(), any())).thenReturn(stats);

        // When
        Map<Long, Long> views = statsService.getViews(eventIds);

        // Then
        assertEquals(1, views.size());
        assertEquals(200L, views.get(1L));
    }

    @Test
    void getViews_EventIdParsingError_HandlesGracefully() {
        // Given
        List<Long> eventIds = List.of(1L);

        List<ViewStatsDto> stats = List.of(
                new ViewStatsDto("ewm-main-service", "/events/not-a-number", 100L) // Не число
        );

        when(statsClient.getStats(any(), any(), any(), any())).thenReturn(stats);

        // When
        Map<Long, Long> views = statsService.getViews(eventIds);

        // Then
        assertTrue(views.isEmpty());
    }

    @Test
    void getViews_UriWithoutEventId_HandlesGracefully() {
        // Given
        List<Long> eventIds = List.of(1L);

        List<ViewStatsDto> stats = List.of(
                new ViewStatsDto("ewm-main-service", "/events/", 100L) // Без ID
        );

        when(statsClient.getStats(any(), any(), any(), any())).thenReturn(stats);

        // When
        Map<Long, Long> views = statsService.getViews(eventIds);

        // Then
        assertTrue(views.isEmpty());
    }

    @Test
    void getViews_MultipleEventsSameUri_ReturnsCorrectCounts() {
        // Given
        List<Long> eventIds = List.of(1L, 2L, 3L);

        List<ViewStatsDto> stats = List.of(
                new ViewStatsDto("ewm-main-service", "/events/1", 100L),
                new ViewStatsDto("ewm-main-service", "/events/1", 50L), // Дубликат
                new ViewStatsDto("ewm-main-service", "/events/2", 200L)
        );

        when(statsClient.getStats(any(), any(), any(), any())).thenReturn(stats);

        // When
        Map<Long, Long> views = statsService.getViews(eventIds);

        // Then
        assertEquals(2, views.size());
        assertEquals(100L, views.get(1L)); // Берется первое значение
        assertEquals(200L, views.get(2L));
        assertNull(views.get(3L)); // Нет статистики для eventId=3
    }

    @Test
    void saveHit_DifferentUris_CallsWithCorrectUri() {
        // Given
        String uri1 = "/events";
        String uri2 = "/events/123";
        String ip = "192.168.1.1";

        doNothing().when(statsClient).hit(any(EndpointHitDto.class));

        // When
        statsService.saveHit(uri1, ip);
        statsService.saveHit(uri2, ip);

        // Then
        verify(statsClient, times(2)).hit(any());
        verify(statsClient).hit(argThat(dto -> dto.getUri().equals("/events")));
        verify(statsClient).hit(argThat(dto -> dto.getUri().equals("/events/123")));
    }
}