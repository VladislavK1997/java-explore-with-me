package ru.practicum.ewm.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.ewm.dto.CompilationDto;
import ru.practicum.ewm.model.Compilation;

import java.util.stream.Collectors;

@UtilityClass
public class CompilationMapper {
    public static CompilationDto toCompilationDto(Compilation compilation) {
        return new CompilationDto(
                compilation.getEvents() != null ?
                        compilation.getEvents().stream()
                                .map(EventMapper::toEventShortDto)
                                .collect(Collectors.toList()) : null,
                compilation.getId(),
                compilation.getPinned(),
                compilation.getTitle()
        );
    }
}