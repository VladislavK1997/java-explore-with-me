package ru.practicum.ewm.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
class DtoValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void newUserRequest_validData_shouldPassValidation() {
        NewUserRequest request = new NewUserRequest("John Doe", "john@example.com");

        Set<ConstraintViolation<NewUserRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void newUserRequest_invalidData_shouldFailValidation() {
        NewUserRequest request = new NewUserRequest("J", "invalid-email");

        Set<ConstraintViolation<NewUserRequest>> violations = validator.validate(request);

        assertEquals(2, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("2")));
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("email")));
    }

    @Test
    void newCategoryDto_validData_shouldPassValidation() {
        NewCategoryDto dto = new NewCategoryDto("Concerts");

        Set<ConstraintViolation<NewCategoryDto>> violations = validator.validate(dto);

        assertTrue(violations.isEmpty());
    }

    @Test
    void newCategoryDto_invalidData_shouldFailValidation() {
        NewCategoryDto dto = new NewCategoryDto("");

        Set<ConstraintViolation<NewCategoryDto>> violations = validator.validate(dto);

        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("blank"));
    }

    @Test
    void newEventDto_validData_shouldPassValidation() {
        NewEventDto dto = new NewEventDto(
                "Valid annotation with at least 20 characters",
                1L,
                "Valid description with at least 20 characters",
                LocalDateTime.now().plusDays(1),
                new LocationDto(55.754167f, 37.62f),
                true,
                100,
                true,
                "Valid Title"
        );

        Set<ConstraintViolation<NewEventDto>> violations = validator.validate(dto);

        assertTrue(violations.isEmpty());
    }

    @Test
    void newEventDto_invalidData_shouldFailValidation() {
        NewEventDto dto = new NewEventDto(
                "Too short",
                null,
                "Too short",
                LocalDateTime.now().minusDays(1),
                null,
                null,
                null,
                null,
                "AB"
        );

        Set<ConstraintViolation<NewEventDto>> violations = validator.validate(dto);

        assertTrue(violations.size() > 0);
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("annotation")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("category")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("description")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("eventDate")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("location")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("title")));
    }

    @Test
    void updateEventUserRequest_validPartialUpdate_shouldPassValidation() {
        UpdateEventUserRequest request = new UpdateEventUserRequest();
        request.setTitle("Updated Title");

        Set<ConstraintViolation<UpdateEventUserRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void updateEventUserRequest_invalidData_shouldFailValidation() {
        UpdateEventUserRequest request = new UpdateEventUserRequest();
        request.setTitle("AB");
        request.setAnnotation("Short");

        Set<ConstraintViolation<UpdateEventUserRequest>> violations = validator.validate(request);

        assertEquals(2, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("title")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("annotation")));
    }

    @Test
    void newCompilationDto_validData_shouldPassValidation() {
        NewCompilationDto dto = new NewCompilationDto();
        dto.setTitle("Valid Title");

        Set<ConstraintViolation<NewCompilationDto>> violations = validator.validate(dto);

        assertTrue(violations.isEmpty());
    }

    @Test
    void newCompilationDto_invalidData_shouldFailValidation() {
        NewCompilationDto dto = new NewCompilationDto();
        dto.setTitle("");

        Set<ConstraintViolation<NewCompilationDto>> violations = validator.validate(dto);

        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("blank"));
    }

    @Test
    void locationDto_validData_shouldPassValidation() {
        LocationDto dto = new LocationDto(55.754167f, 37.62f);

        Set<ConstraintViolation<LocationDto>> violations = validator.validate(dto);

        assertTrue(violations.isEmpty());
    }

    @Test
    void participationRequestDto_validData_shouldPassValidation() {
        ParticipationRequestDto dto = new ParticipationRequestDto(
                LocalDateTime.now(),
                1L,
                1L,
                2L,
                "PENDING"
        );

        Set<ConstraintViolation<ParticipationRequestDto>> violations = validator.validate(dto);

        assertTrue(violations.isEmpty());
    }
}