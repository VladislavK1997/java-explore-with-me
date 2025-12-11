package ru.practicum.ewm.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.practicum.ewm.dto.ApiError;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNotFoundException(NotFoundException e) {
        log.error("Not found: {}", e.getMessage(), e);
        return new ApiError(
                List.of(e.toString()),
                e.getMessage(),
                "The required object was not found.",
                "NOT_FOUND",
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleValidationException(ValidationException e) {
        log.error("Validation error: {}", e.getMessage(), e);
        return new ApiError(
                List.of(e.toString()),
                e.getMessage(),
                "Incorrectly made request.",
                "BAD_REQUEST",
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleConflictException(ConflictException e) {
        log.error("Conflict: {}", e.getMessage(), e);
        return new ApiError(
                List.of(e.toString()),
                e.getMessage(),
                "For the requested operation the conditions are not met.",
                "CONFLICT",
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleConstraintViolationException(ConstraintViolationException e) {
        log.error("Constraint violation: {}", e.getMessage(), e);
        return new ApiError(
                List.of(e.toString()),
                "Validation failed for some fields",
                "Incorrectly made request.",
                "BAD_REQUEST",
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error("Validation error: {}", e.getMessage(), e);
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> String.format("Field: %s. Error: %s. Value: %s",
                        error.getField(), error.getDefaultMessage(), error.getRejectedValue()))
                .findFirst()
                .orElse("Validation failed");
        return new ApiError(
                List.of(e.toString()),
                message,
                "Incorrectly made request.",
                "BAD_REQUEST",
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        log.error("Data integrity violation: {}", e.getMessage(), e);
        return new ApiError(
                List.of(e.toString()),
                "Integrity constraint has been violated.",
                "Integrity constraint has been violated.",
                "CONFLICT",
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.error("Method argument type mismatch: {}", e.getMessage(), e);
        String message = String.format("Parameter '%s' should be of type %s",
                e.getName(), e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "unknown");
        return new ApiError(
                List.of(e.toString()),
                message,
                "Incorrectly made request.",
                "BAD_REQUEST",
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        log.error("Missing servlet request parameter: {}", e.getMessage(), e);
        return new ApiError(
                List.of(e.toString()),
                e.getMessage(),
                "Incorrectly made request.",
                "BAD_REQUEST",
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(DateTimeParseException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleDateTimeParseException(DateTimeParseException e) {
        log.error("Date time parse error: {}", e.getMessage(), e);
        return new ApiError(
                List.of(e.toString()),
                "Invalid date format. Expected format: yyyy-MM-dd HH:mm:ss",
                "Incorrectly made request.",
                "BAD_REQUEST",
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.error("HTTP message not readable: {}", e.getMessage(), e);
        return new ApiError(
                List.of(e.toString()),
                "Invalid request body format",
                "Incorrectly made request.",
                "BAD_REQUEST",
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("Illegal argument: {}", e.getMessage(), e);
        return new ApiError(
                List.of(e.toString()),
                e.getMessage(),
                "Incorrectly made request.",
                "BAD_REQUEST",
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleIllegalStateException(IllegalStateException e) {
        log.error("Illegal state: {}", e.getMessage(), e);
        return new ApiError(
                List.of(e.toString()),
                e.getMessage(),
                "Incorrectly made request.",
                "BAD_REQUEST",
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleOtherExceptions(Exception e) {
        log.error("Internal server error: {}", e.getMessage(), e);
        return new ApiError(
                List.of(e.toString()),
                e.getMessage(),
                "Internal server error",
                "INTERNAL_SERVER_ERROR",
                LocalDateTime.now()
        );
    }
}