package ru.practicum.ewm.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.validation.BindException;
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
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNotFoundException(NotFoundException e) {
        log.error("Not found: {}", e.getMessage());
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
        log.error("Validation error: {}", e.getMessage());
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
        log.error("Conflict: {}", e.getMessage());
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
        log.error("Constraint violation error: {}", e.getMessage());

        String message = e.getConstraintViolations().stream()
                .map(violation -> {
                    String field = violation.getPropertyPath().toString();
                    String msg = violation.getMessage();
                    Object value = violation.getInvalidValue();
                    return String.format("Field: %s. Error: %s. Value: %s",
                            field, msg, value != null ? value : "null");
                })
                .collect(Collectors.joining("; "));

        return new ApiError(
                List.of(e.toString()),
                message,
                "Incorrectly made request.",
                "BAD_REQUEST",
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error("Method argument not valid error: {}", e.getMessage());

        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> String.format("Field: %s. Error: %s. Value: %s",
                        error.getField(),
                        error.getDefaultMessage(),
                        error.getRejectedValue() != null ? error.getRejectedValue() : "null"))
                .collect(Collectors.joining("; "));

        return new ApiError(
                List.of(e.toString()),
                message,
                "Incorrectly made request.",
                "BAD_REQUEST",
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleBindException(BindException e) {
        log.error("Bind error: {}", e.getMessage());

        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> String.format("Field: %s. Error: %s. Value: %s",
                        error.getField(),
                        error.getDefaultMessage(),
                        error.getRejectedValue() != null ? error.getRejectedValue() : "null"))
                .findFirst()
                .orElse("Bind failed");

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
        log.error("Data integrity violation: {}", e.getMessage());
        String message = "Integrity constraint has been violated.";
        if (e.getMessage() != null) {
            if (e.getMessage().contains("uq_category_name")) {
                message = "Category name must be unique";
            } else if (e.getMessage().contains("uq_email")) {
                message = "Email must be unique";
            } else if (e.getMessage().contains("uq_compilation_name")) {
                message = "Compilation title must be unique";
            } else if (e.getMessage().contains("uq_request")) {
                message = "Duplicate request";
            }
        }
        return new ApiError(
                List.of(e.toString()),
                message,
                "Integrity constraint has been violated.",
                "CONFLICT",
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.error("Method argument type mismatch: {}", e.getMessage());
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
        log.error("Missing servlet request parameter: {}", e.getMessage());
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
        log.error("Date time parse error: {}", e.getMessage());
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
        log.error("HTTP message not readable: {}", e.getMessage());
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
        log.error("Illegal argument: {}", e.getMessage());
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
        log.error("Illegal state: {}", e.getMessage());
        return new ApiError(
                List.of(e.toString()),
                e.getMessage(),
                "Incorrectly made request.",
                "BAD_REQUEST",
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(UnexpectedRollbackException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleUnexpectedRollbackException(UnexpectedRollbackException e) {
        log.error("Transaction rollback error: {}", e.getMessage());
        String message = "Invalid request data";
        if (e.getMessage() != null && e.getMessage().contains("rollback-only")) {
            message = "Invalid request data";
        }
        return new ApiError(
                List.of(e.toString()),
                message,
                "Incorrectly made request.",
                "BAD_REQUEST",
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleOtherExceptions(Exception e) {
        log.error("Internal server error: {}", e.getMessage(), e);
        String message = "Internal server error occurred";
        if (e.getMessage() != null) {
            message = e.getMessage();
        }
        return new ApiError(
                List.of(e.toString()),
                message,
                "Internal server error",
                "INTERNAL_SERVER_ERROR",
                LocalDateTime.now()
        );
    }
}