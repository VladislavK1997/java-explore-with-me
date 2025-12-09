package ru.practicum.ewm.stat.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;
import ru.practicum.ewm.stat.model.EndpointHit;
import ru.practicum.ewm.stat.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class StatsRepositoryTest {

    @Autowired
    private StatsRepository statsRepository;

    private EndpointHit hit1;
    private EndpointHit hit2;
    private EndpointHit hit3;
    private EndpointHit hit4;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();

        hit1 = EndpointHit.builder()
                .app("ewm-main-service")
                .uri("/events/1")
                .ip("192.168.1.1")
                .timestamp(now.minusHours(1))
                .build();

        hit2 = EndpointHit.builder()
                .app("ewm-main-service")
                .uri("/events/1")
                .ip("192.168.1.2")
                .timestamp(now.minusMinutes(30))
                .build();

        hit3 = EndpointHit.builder()
                .app("ewm-main-service")
                .uri("/events/2")
                .ip("192.168.1.1")
                .timestamp(now.minusMinutes(15))
                .build();

        hit4 = EndpointHit.builder()
                .app("another-service")
                .uri("/events/1")
                .ip("192.168.1.3")
                .timestamp(now)
                .build();

        statsRepository.save(hit1);
        statsRepository.save(hit2);
        statsRepository.save(hit3);
        statsRepository.save(hit4);
    }

    @Test
    void shouldSaveAndFindEndpointHit() {
        EndpointHit newHit = EndpointHit.builder()
                .app("test-app")
                .uri("/test")
                .ip("127.0.0.1")
                .timestamp(LocalDateTime.now())
                .build();

        EndpointHit saved = statsRepository.save(newHit);
        assertNotNull(saved.getId());
        assertEquals("test-app", saved.getApp());
        assertEquals("/test", saved.getUri());
        assertEquals("127.0.0.1", saved.getIp());
    }

    @Test
    void shouldGetStatsForAllUris() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(1);

        List<ViewStatsDto> stats = statsRepository.getStats(start, end, null);

        assertEquals(3, stats.size());

        assertEquals("/events/1", stats.get(0).getUri());
        assertEquals(3, stats.get(0).getHits());

        assertEquals("/events/2", stats.get(1).getUri());
        assertEquals(1, stats.get(1).getHits());

        assertEquals("another-service", stats.get(2).getApp());
        assertEquals("/events/1", stats.get(2).getUri());
        assertEquals(1, stats.get(2).getHits());
    }

    @Test
    void shouldGetStatsForSpecificUris() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        List<String> uris = List.of("/events/1");

        List<ViewStatsDto> stats = statsRepository.getStats(start, end, uris);

        assertEquals(2, stats.size());

        boolean foundEwmMainService = false;
        boolean foundAnotherService = false;

        for (ViewStatsDto stat : stats) {
            if (stat.getApp().equals("ewm-main-service") && stat.getUri().equals("/events/1")) {
                assertEquals(2, stat.getHits());
                foundEwmMainService = true;
            } else if (stat.getApp().equals("another-service") && stat.getUri().equals("/events/1")) {
                assertEquals(1, stat.getHits());
                foundAnotherService = true;
            }
        }

        assertTrue(foundEwmMainService);
        assertTrue(foundAnotherService);
    }

    @Test
    void shouldGetUniqueStats() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(1);

        List<ViewStatsDto> stats = statsRepository.getStatsUnique(start, end, null);

        assertEquals(3, stats.size());

        for (ViewStatsDto stat : stats) {
            if (stat.getApp().equals("ewm-main-service") && stat.getUri().equals("/events/1")) {
                assertEquals(2, stat.getHits());
            } else if (stat.getApp().equals("ewm-main-service") && stat.getUri().equals("/events/2")) {
                assertEquals(1, stat.getHits());
            } else if (stat.getApp().equals("another-service") && stat.getUri().equals("/events/1")) {
                assertEquals(1, stat.getHits());
            }
        }
    }

    @Test
    void shouldReturnEmptyListWhenNoDataInTimeRange() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(2);

        List<ViewStatsDto> stats = statsRepository.getStats(start, end, null);
        assertTrue(stats.isEmpty());
    }

    @Test
    void shouldReturnEmptyListForUnknownUri() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        List<String> uris = List.of("/unknown");

        List<ViewStatsDto> stats = statsRepository.getStats(start, end, uris);
        assertTrue(stats.isEmpty());
    }
}