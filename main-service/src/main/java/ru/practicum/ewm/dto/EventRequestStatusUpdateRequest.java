package ru.practicum.ewm.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventRequestStatusUpdateRequest {
    @NotEmpty
    private List<Long> requestIds;

    @NotNull
    @Pattern(regexp = "CONFIRMED|REJECTED", message = "Status must be CONFIRMED or REJECTED")
    private String status;
}