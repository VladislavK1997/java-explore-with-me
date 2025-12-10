package ru.practicum.ewm.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.ewm.dto.CompilationDto;
import ru.practicum.ewm.model.Compilation;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@UtilityClass
public class CompilationMapper {
    public static CompilationDto toCompilationDto(Compilation compilation) {
        if (compilation == null) {
            return null;
        }

        List<ru.practicum.ewm.dto.EventShortDto> events = new ArrayList<>();
        if (compilation.getEvents() != null && !compilation.getEvents().isEmpty()) {
            events = compilation.getEvents().stream()
                    .map(EventMapper::toEventShortDto)
                    .collect(Collectors.toList());
        }

        return new CompilationDto(
                events.isEmpty() ? null : events,
                compilation.getId(),
                compilation.getPinned(),
                compilation.getTitle()
        );
    }
}