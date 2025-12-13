package ru.practicum.ewm.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.ewm.stat.client.StatsClient;
import ru.practicum.ewm.stat.dto.EndpointHitDto;
import ru.practicum.ewm.stat.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    @Mock
    private StatsClient statsClient;

    @InjectMocks
    private StatsService statsService;

    private final LocalDateTime now = LocalDateTime.now();

    @Test
    void saveHit_ValidData_CallsStatsClient() {
        String uri = "/events";
        String ip = "192.168.1.1";

        doNothing().when(statsClient).hit(any(EndpointHitDto.class));

        statsService.saveHit(uri, ip);

        verify(statsClient, times(1)).hit(argThat(dto ->
                dto.getApp().equals("ewm-main-service") &&
                        dto.getUri().equals(uri) &&
                        dto.getIp().equals(ip) &&
                        dto.getTimestamp() != null
        ));
    }

    @Test
    void saveHit_StatsClientThrowsException_LogsError() {
        String uri = "/events";
        String ip = "192.168.1.1";

        doThrow(new RuntimeException("Network error")).when(statsClient).hit(any());

        assertDoesNotThrow(() -> statsService.saveHit(uri, ip));
        verify(statsClient, times(1)).hit(any());
    }

    @Test
    void getViews_WithEventIds_ReturnsViewsMap() {
        List<Long> eventIds = List.of(1L, 2L, 3L);
        List<String> expectedUris = List.of("/events/1", "/events/2", "/events/3");

        List<ViewStatsDto> stats = List.of(
                new ViewStatsDto("ewm-main-service", "/events/1", 100L),
                new ViewStatsDto("ewm-main-service", "/events/2", 200L),
                new ViewStatsDto("ewm-main-service", "/events/3", 300L)
        );

        when(statsClient.getStats(any(LocalDateTime.class), any(LocalDateTime.class), eq(expectedUris), eq(true)))
                .thenReturn(stats);

        Map<Long, Long> views = statsService.getViews(eventIds);

        assertNotNull(views);
        assertEquals(3, views.size());
        assertEquals(100L, views.get(1L));
        assertEquals(200L, views.get(2L));
        assertEquals(300L, views.get(3L));
        verify(statsClient, times(1)).getStats(any(), any(), eq(expectedUris), eq(true));
    }

    @Test
    void getViews_EmptyEventIds_ReturnsEmptyMap() {
        List<Long> eventIds = List.of();

        Map<Long, Long> views = statsService.getViews(eventIds);

        assertNotNull(views);
        assertTrue(views.isEmpty());
        verify(statsClient, never()).getStats(any(), any(), any(), any());
    }

    @Test
    void getViews_NullEventIds_ReturnsEmptyMap() {
        Map<Long, Long> views = statsService.getViews(null);

        assertNotNull(views);
        assertTrue(views.isEmpty());
        verify(statsClient, never()).getStats(any(), any(), any(), any());
    }

    @Test
    void getViews_StatsClientThrowsException_ReturnsMapWithZeros() {
        List<Long> eventIds = List.of(1L, 2L);

        when(statsClient.getStats(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Network error"));

        Map<Long, Long> views = statsService.getViews(eventIds);

        assertNotNull(views);
        assertEquals(2, views.size());
        assertEquals(0L, views.get(1L));
        assertEquals(0L, views.get(2L));
        verify(statsClient, times(1)).getStats(any(), any(), any(), any());
    }

    @Test
    void getViews_InvalidUriInResponse_HandlesGracefully() {
        List<Long> eventIds = List.of(1L);

        List<ViewStatsDto> stats = List.of(
                new ViewStatsDto("ewm-main-service", "invalid-uri", 100L),
                new ViewStatsDto("ewm-main-service", "/events/1", 200L)
        );

        when(statsClient.getStats(any(), any(), any(), any())).thenReturn(stats);

        Map<Long, Long> views = statsService.getViews(eventIds);

        assertEquals(1, views.size());
        assertEquals(200L, views.get(1L));
    }

    @Test
    void getViews_EventIdParsingError_HandlesGracefully() {
        List<Long> eventIds = List.of(1L);

        List<ViewStatsDto> stats = List.of(
                new ViewStatsDto("ewm-main-service", "/events/not-a-number", 100L)
        );

        when(statsClient.getStats(any(), any(), any(), any())).thenReturn(stats);

        Map<Long, Long> views = statsService.getViews(eventIds);

        assertEquals(1, views.size());
        assertEquals(0L, views.get(1L));
    }

    @Test
    void getViews_UriWithoutEventId_HandlesGracefully() {
        List<Long> eventIds = List.of(1L);

        List<ViewStatsDto> stats = List.of(
                new ViewStatsDto("ewm-main-service", "/events/", 100L)
        );

        when(statsClient.getStats(any(), any(), any(), any())).thenReturn(stats);

        Map<Long, Long> views = statsService.getViews(eventIds);

        assertEquals(1, views.size());
        assertEquals(0L, views.get(1L));
    }

    @Test
    void getViews_MultipleEventsSameUri_ReturnsCorrectCounts() {
        List<Long> eventIds = List.of(1L, 2L, 3L);

        List<ViewStatsDto> stats = List.of(
                new ViewStatsDto("ewm-main-service", "/events/1", 100L),
                new ViewStatsDto("ewm-main-service", "/events/1", 50L),
                new ViewStatsDto("ewm-main-service", "/events/2", 200L)
        );

        when(statsClient.getStats(any(), any(), any(), any())).thenReturn(stats);

        Map<Long, Long> views = statsService.getViews(eventIds);

        assertEquals(3, views.size());
        assertEquals(100L, views.get(1L));
        assertEquals(200L, views.get(2L));
        assertEquals(0L, views.get(3L));
    }

    @Test
    void saveHit_DifferentUris_CallsWithCorrectUri() {
        String uri1 = "/events";
        String uri2 = "/events/123";
        String ip = "192.168.1.1";

        doNothing().when(statsClient).hit(any(EndpointHitDto.class));

        statsService.saveHit(uri1, ip);
        statsService.saveHit(uri2, ip);

        verify(statsClient, times(2)).hit(any());
        verify(statsClient).hit(argThat(dto -> dto.getUri().equals("/events")));
        verify(statsClient).hit(argThat(dto -> dto.getUri().equals("/events/123")));
    }

    @Test
    void getViews_StatsClientReturnsEmptyList_ReturnsMapWithZeros() {
        List<Long> eventIds = List.of(1L, 2L, 3L);

        when(statsClient.getStats(any(), any(), any(), any())).thenReturn(List.of());

        Map<Long, Long> views = statsService.getViews(eventIds);

        assertNotNull(views);
        assertEquals(3, views.size());
        assertEquals(0L, views.get(1L));
        assertEquals(0L, views.get(2L));
        assertEquals(0L, views.get(3L));
        verify(statsClient, times(1)).getStats(any(), any(), any(), any());
    }

    @Test
    void getViews_StatsClientReturnsNull_ReturnsMapWithZeros() {
        List<Long> eventIds = List.of(1L, 2L);

        when(statsClient.getStats(any(), any(), any(), any())).thenReturn(null);

        Map<Long, Long> views = statsService.getViews(eventIds);

        assertNotNull(views);
        assertEquals(2, views.size());
        assertEquals(0L, views.get(1L));
        assertEquals(0L, views.get(2L));
        verify(statsClient, times(1)).getStats(any(), any(), any(), any());
    }
}