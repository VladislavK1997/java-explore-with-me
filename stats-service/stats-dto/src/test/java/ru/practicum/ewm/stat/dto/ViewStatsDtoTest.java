package ru.practicum.ewm.stat.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ViewStatsDtoTest {

    @Test
    public void shouldCreateValidViewStatsDto() {
        ViewStatsDto dto = new ViewStatsDto(
                "ewm-main-service",
                "/events/1",
                100L
        );

        assertEquals("ewm-main-service", dto.getApp());
        assertEquals("/events/1", dto.getUri());
        assertEquals(100L, dto.getHits());
    }

    @Test
    public void shouldHaveDefaultConstructor() {
        ViewStatsDto dto = new ViewStatsDto();
        assertNotNull(dto);
    }

    @Test
    public void shouldHaveCorrectGettersAndSetters() {
        ViewStatsDto dto = new ViewStatsDto();

        dto.setApp("test-app");
        dto.setUri("/test/uri");
        dto.setHits(50L);

        assertEquals("test-app", dto.getApp());
        assertEquals("/test/uri", dto.getUri());
        assertEquals(50L, dto.getHits());
    }

    @Test
    public void shouldBeEqualWithSameValues() {
        ViewStatsDto dto1 = new ViewStatsDto("app", "/uri", 10L);
        ViewStatsDto dto2 = new ViewStatsDto("app", "/uri", 10L);

        assertEquals(dto1.getApp(), dto2.getApp());
        assertEquals(dto1.getUri(), dto2.getUri());
        assertEquals(dto1.getHits(), dto2.getHits());
    }
}