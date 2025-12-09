package ru.practicum.ewm.stat.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class EndpointHitDtoTest {
    private Validator validator;
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    public void shouldCreateValidEndpointHitDto() {
        EndpointHitDto dto = new EndpointHitDto(
                1L,
                "ewm-main-service",
                "/events/1",
                "192.168.1.1",
                LocalDateTime.now()
        );

        Set<ConstraintViolation<EndpointHitDto>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }

    @Test
    public void shouldFailValidationWhenAppIsBlank() {
        EndpointHitDto dto = new EndpointHitDto(
                1L,
                "",
                "/events/1",
                "192.168.1.1",
                LocalDateTime.now()
        );

        Set<ConstraintViolation<EndpointHitDto>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertEquals(1, violations.size());
        assertEquals("App cannot be blank", violations.iterator().next().getMessage());
    }

    @Test
    public void shouldFailValidationWhenUriIsBlank() {
        EndpointHitDto dto = new EndpointHitDto(
                1L,
                "ewm-main-service",
                "",
                "192.168.1.1",
                LocalDateTime.now()
        );

        Set<ConstraintViolation<EndpointHitDto>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertEquals(1, violations.size());
        assertEquals("URI cannot be blank", violations.iterator().next().getMessage());
    }

    @Test
    public void shouldFailValidationWhenIpIsBlank() {
        EndpointHitDto dto = new EndpointHitDto(
                1L,
                "ewm-main-service",
                "/events/1",
                "",
                LocalDateTime.now()
        );

        Set<ConstraintViolation<EndpointHitDto>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertEquals(1, violations.size());
        assertEquals("IP cannot be blank", violations.iterator().next().getMessage());
    }

    @Test
    public void shouldFailValidationWhenTimestampIsNull() {
        EndpointHitDto dto = new EndpointHitDto(
                1L,
                "ewm-main-service",
                "/events/1",
                "192.168.1.1",
                null
        );

        Set<ConstraintViolation<EndpointHitDto>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertEquals(1, violations.size());
        assertEquals("Timestamp cannot be null", violations.iterator().next().getMessage());
    }

    @Test
    public void shouldSerializeAndDeserializeJson() throws Exception {
        LocalDateTime timestamp = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
        EndpointHitDto original = new EndpointHitDto(
                1L,
                "ewm-main-service",
                "/events/1",
                "192.168.1.1",
                timestamp
        );

        String json = objectMapper.writeValueAsString(original);
        assertNotNull(json);

        EndpointHitDto deserialized = objectMapper.readValue(json, EndpointHitDto.class);
        assertEquals(original.getId(), deserialized.getId());
        assertEquals(original.getApp(), deserialized.getApp());
        assertEquals(original.getUri(), deserialized.getUri());
        assertEquals(original.getIp(), deserialized.getIp());
        assertEquals(original.getTimestamp(), deserialized.getTimestamp());
    }

    @Test
    public void shouldHaveCorrectGettersAndSetters() {
        EndpointHitDto dto = new EndpointHitDto();

        dto.setId(1L);
        dto.setApp("test-app");
        dto.setUri("/test");
        dto.setIp("127.0.0.1");
        LocalDateTime timestamp = LocalDateTime.now();
        dto.setTimestamp(timestamp);

        assertEquals(1L, dto.getId());
        assertEquals("test-app", dto.getApp());
        assertEquals("/test", dto.getUri());
        assertEquals("127.0.0.1", dto.getIp());
        assertEquals(timestamp, dto.getTimestamp());
    }
}