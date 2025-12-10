package ru.practicum.ewm.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.ewm.dto.CompilationDto;
import ru.practicum.ewm.model.Compilation;

import java.util.Collections;
import java.util.stream.Collectors;

@UtilityClass
public class CompilationMapper {
    public static CompilationDto toCompilationDto(Compilation compilation) {
        if (compilation == null) {
            return null;
        }
        return new CompilationDto(
                compilation.getEvents() != null ?
                        compilation.getEvents().stream()
                                .map(EventMapper::toEventShortDto)
                                .collect(Collectors.toList()) : Collections.emptyList(),
                compilation.getId(),
                compilation.getPinned(),
                compilation.getTitle()
        );
    }
}