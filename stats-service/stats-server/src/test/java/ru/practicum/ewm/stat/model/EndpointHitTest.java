package ru.practicum.ewm.stat.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EndpointHitTest {

    @Test
    void shouldCreateEndpointHitWithBuilder() {
        LocalDateTime timestamp = LocalDateTime.now();
        EndpointHit hit = EndpointHit.builder()
                .id(1L)
                .app("ewm-main-service")
                .uri("/events/1")
                .ip("192.168.1.1")
                .timestamp(timestamp)
                .build();

        assertEquals(1L, hit.getId());
        assertEquals("ewm-main-service", hit.getApp());
        assertEquals("/events/1", hit.getUri());
        assertEquals("192.168.1.1", hit.getIp());
        assertEquals(timestamp, hit.getTimestamp());
    }

    @Test
    void shouldCreateEndpointHitWithAllArgsConstructor() {
        LocalDateTime timestamp = LocalDateTime.now();
        EndpointHit hit = new EndpointHit(1L, "app", "/uri", "127.0.0.1", timestamp);

        assertEquals(1L, hit.getId());
        assertEquals("app", hit.getApp());
        assertEquals("/uri", hit.getUri());
        assertEquals("127.0.0.1", hit.getIp());
        assertEquals(timestamp, hit.getTimestamp());
    }

    @Test
    void shouldCreateEndpointHitWithNoArgsConstructor() {
        EndpointHit hit = new EndpointHit();
        assertNotNull(hit);
    }

    @Test
    void shouldHaveWorkingSettersAndGetters() {
        EndpointHit hit = new EndpointHit();
        LocalDateTime timestamp = LocalDateTime.now();

        hit.setId(1L);
        hit.setApp("test-app");
        hit.setUri("/test");
        hit.setIp("192.168.0.1");
        hit.setTimestamp(timestamp);

        assertEquals(1L, hit.getId());
        assertEquals("test-app", hit.getApp());
        assertEquals("/test", hit.getUri());
        assertEquals("192.168.0.1", hit.getIp());
        assertEquals(timestamp, hit.getTimestamp());
    }

    @Test
    void shouldBeEqualWithSameId() {
        LocalDateTime timestamp = LocalDateTime.now();
        EndpointHit hit1 = new EndpointHit(1L, "app", "/uri", "ip", timestamp);
        EndpointHit hit2 = new EndpointHit(1L, "app", "/uri", "ip", timestamp);

        assertEquals(hit1, hit2);
        assertEquals(hit1.hashCode(), hit2.hashCode());
    }

    @Test
    void shouldHaveCorrectToString() {
        LocalDateTime timestamp = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
        EndpointHit hit = new EndpointHit(1L, "app", "/uri", "ip", timestamp);

        String toString = hit.toString();
        assertTrue(toString.contains("id=1"));
        assertTrue(toString.contains("app=app"));
        assertTrue(toString.contains("uri=/uri"));
        assertTrue(toString.contains("ip=ip"));
    }
}