package ru.practicum.ewm.stat.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.stat.dto.EndpointHitDto;
import ru.practicum.ewm.stat.dto.ViewStatsDto;
import ru.practicum.ewm.stat.mapper.StatsMapper;
import ru.practicum.ewm.stat.model.EndpointHit;
import ru.practicum.ewm.stat.repository.StatsRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatsServiceImplTest {

    @Mock
    private StatsRepository statsRepository;

    @InjectMocks
    private StatsServiceImpl statsService;

    private EndpointHitDto endpointHitDto;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();
        endpointHitDto = new EndpointHitDto(
                1L,
                "ewm-main-service",
                "/events/1",
                "192.168.1.1",
                now
        );
    }

    @Test
    void shouldSaveHit() {
        when(statsRepository.save(any(EndpointHit.class))).thenAnswer(invocation -> {
            EndpointHit hit = invocation.getArgument(0);
            hit.setId(1L);
            return hit;
        });

        statsService.saveHit(endpointHitDto);

        verify(statsRepository, times(1)).save(any(EndpointHit.class));
    }

    @Test
    void shouldGetStats() {
        LocalDateTime start = now.minusDays(1);
        LocalDateTime end = now.plusDays(1);
        List<String> uris = List.of("/events/1");
        Boolean unique = false;

        List<ViewStatsDto> expectedStats = List.of(
                new ViewStatsDto("ewm-main-service", "/events/1", 10L)
        );

        when(statsRepository.getStats(start, end, uris)).thenReturn(expectedStats);

        List<ViewStatsDto> actualStats = statsService.getStats(start, end, uris, unique);

        assertEquals(expectedStats, actualStats);
        verify(statsRepository, times(1)).getStats(start, end, uris);
        verify(statsRepository, never()).getStatsUnique(any(), any(), any());
    }

    @Test
    void shouldGetUniqueStats() {
        LocalDateTime start = now.minusDays(1);
        LocalDateTime end = now.plusDays(1);
        List<String> uris = List.of("/events/1");
        Boolean unique = true;

        List<ViewStatsDto> expectedStats = List.of(
                new ViewStatsDto("ewm-main-service", "/events/1", 5L)
        );

        when(statsRepository.getStatsUnique(start, end, uris)).thenReturn(expectedStats);

        List<ViewStatsDto> actualStats = statsService.getStats(start, end, uris, unique);

        assertEquals(expectedStats, actualStats);
        verify(statsRepository, times(1)).getStatsUnique(start, end, uris);
        verify(statsRepository, never()).getStats(any(), any(), any());
    }

    @Test
    void shouldGetStatsWithNullUris() {
        LocalDateTime start = now.minusDays(1);
        LocalDateTime end = now.plusDays(1);

        List<ViewStatsDto> expectedStats = List.of(
                new ViewStatsDto("ewm-main-service", "/events/1", 10L),
                new ViewStatsDto("ewm-main-service", "/events/2", 5L)
        );

        when(statsRepository.getStats(start, end, null)).thenReturn(expectedStats);

        List<ViewStatsDto> actualStats = statsService.getStats(start, end, null, false);

        assertEquals(expectedStats, actualStats);
        verify(statsRepository, times(1)).getStats(start, end, null);
    }

    @Test
    void shouldThrowExceptionWhenStartAfterEnd() {
        LocalDateTime start = now.plusDays(1);
        LocalDateTime end = now.minusDays(1);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> statsService.getStats(start, end, null, false)
        );

        assertEquals("Start date must be before end date", exception.getMessage());
        verify(statsRepository, never()).getStats(any(), any(), any());
        verify(statsRepository, never()).getStatsUnique(any(), any(), any());
    }

    @Test
    void shouldGetStatsWithEmptyUrisList() {
        LocalDateTime start = now.minusDays(1);
        LocalDateTime end = now.plusDays(1);
        List<String> uris = List.of();

        List<ViewStatsDto> expectedStats = List.of(
                new ViewStatsDto("ewm-main-service", "/events/1", 10L)
        );

        when(statsRepository.getStats(start, end, uris)).thenReturn(expectedStats);

        List<ViewStatsDto> actualStats = statsService.getStats(start, end, uris, false);

        assertEquals(expectedStats, actualStats);
        verify(statsRepository, times(1)).getStats(start, end, uris);
    }

    @Test
    void shouldHandleNullUniqueParameter() {
        LocalDateTime start = now.minusDays(1);
        LocalDateTime end = now.plusDays(1);

        List<ViewStatsDto> expectedStats = List.of(
                new ViewStatsDto("ewm-main-service", "/events/1", 10L)
        );

        when(statsRepository.getStats(start, end, null)).thenReturn(expectedStats);

        List<ViewStatsDto> actualStats = statsService.getStats(start, end, null, null);

        assertEquals(expectedStats, actualStats);
        verify(statsRepository, times(1)).getStats(start, end, null);
    }
}