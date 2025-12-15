package ru.practicum.ewm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NewCommentDto {
    @NotBlank
    private String text;
}