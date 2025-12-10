package ru.practicum.ewm.stat.mapper;

import org.junit.jupiter.api.Test;
import ru.practicum.ewm.stat.dto.EndpointHitDto;
import ru.practicum.ewm.stat.model.EndpointHit;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class StatsMapperTest {

    @Test
    void shouldConvertDtoToEntity() {
        LocalDateTime timestamp = LocalDateTime.now();
        EndpointHitDto dto = new EndpointHitDto(
                1L,
                "ewm-main-service",
                "/events/1",
                "192.168.1.1",
                timestamp
        );

        EndpointHit entity = StatsMapper.toEntity(dto);

        assertNull(entity.getId());
        assertEquals(dto.getApp(), entity.getApp());
        assertEquals(dto.getUri(), entity.getUri());
        assertEquals(dto.getIp(), entity.getIp());
        assertEquals(dto.getTimestamp(), entity.getTimestamp());
    }

    @Test
    void shouldConvertEntityToDto() {
        LocalDateTime timestamp = LocalDateTime.now();
        EndpointHit entity = EndpointHit.builder()
                .id(1L)
                .app("ewm-main-service")
                .uri("/events/1")
                .ip("192.168.1.1")
                .timestamp(timestamp)
                .build();

        EndpointHitDto dto = StatsMapper.toDto(entity);

        assertEquals(entity.getId(), dto.getId());
        assertEquals(entity.getApp(), dto.getApp());
        assertEquals(entity.getUri(), dto.getUri());
        assertEquals(entity.getIp(), dto.getIp());
        assertEquals(entity.getTimestamp(), dto.getTimestamp());
    }

    @Test
    void shouldReturnNullWhenDtoIsNull() {
        EndpointHit entity = StatsMapper.toEntity(null);
        assertNull(entity);
    }

    @Test
    void shouldReturnNullWhenEntityIsNull() {
        EndpointHitDto dto = StatsMapper.toDto(null);
        assertNull(dto);
    }

    @Test
    void shouldHandleDtoWithNullId() {
        LocalDateTime timestamp = LocalDateTime.now();
        EndpointHitDto dto = new EndpointHitDto(
                null,
                "ewm-main-service",
                "/events/1",
                "192.168.1.1",
                timestamp
        );

        EndpointHit entity = StatsMapper.toEntity(dto);
        assertNull(entity.getId());
        assertEquals(dto.getApp(), entity.getApp());
    }
}