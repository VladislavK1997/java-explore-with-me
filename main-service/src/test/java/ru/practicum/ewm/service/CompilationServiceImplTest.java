package ru.practicum.ewm.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.ewm.dto.CompilationDto;
import ru.practicum.ewm.dto.NewCompilationDto;
import ru.practicum.ewm.dto.UpdateCompilationRequest;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.CompilationMapper;
import ru.practicum.ewm.model.Compilation;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.repository.CompilationRepository;
import ru.practicum.ewm.repository.EventRepository;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class CompilationServiceImplTest {

    @Mock
    private CompilationRepository compilationRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private StatsService statsService;

    @InjectMocks
    private CompilationServiceImpl compilationService;

    private final LocalDateTime now = LocalDateTime.now();

    @Test
    void createCompilation_ValidData_ReturnsCompilationDto() {
        // Given
        NewCompilationDto newCompilationDto = new NewCompilationDto(
                List.of(1L, 2L, 3L),
                true,
                "Summer Events"
        );

        Event event1 = Event.builder().id(1L).title("Event 1").build();
        Event event2 = Event.builder().id(2L).title("Event 2").build();
        Event event3 = Event.builder().id(3L).title("Event 3").build();
        Set<Event> events = new HashSet<>(List.of(event1, event2, event3));

        Compilation compilation = Compilation.builder()
                .id(1L)
                .events(events)
                .pinned(true)
                .title("Summer Events")
                .build();

        when(eventRepository.findByIdIn(List.of(1L, 2L, 3L))).thenReturn(List.of(event1, event2, event3));
        when(compilationRepository.save(any(Compilation.class))).thenReturn(compilation);
        when(statsService.getViews(List.of(1L, 2L, 3L))).thenReturn(
                Map.of(1L, 100L, 2L, 200L, 3L, 300L));

        // When
        CompilationDto result = compilationService.createCompilation(newCompilationDto);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Summer Events", result.getTitle());
        assertTrue(result.getPinned());
        assertNotNull(result.getEvents());
        assertEquals(3, result.getEvents().size());
        verify(eventRepository, times(1)).findByIdIn(List.of(1L, 2L, 3L));
        verify(compilationRepository, times(1)).save(any(Compilation.class));
        verify(statsService, times(1)).getViews(List.of(1L, 2L, 3L));
    }

    @Test
    void createCompilation_NoEvents_ReturnsCompilationDto() {
        // Given
        NewCompilationDto newCompilationDto = new NewCompilationDto(
                null,
                false,
                "Empty Compilation"
        );

        Compilation compilation = Compilation.builder()
                .id(1L)
                .events(new HashSet<>())
                .pinned(false)
                .title("Empty Compilation")
                .build();

        when(compilationRepository.save(any(Compilation.class))).thenReturn(compilation);

        // When
        CompilationDto result = compilationService.createCompilation(newCompilationDto);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Empty Compilation", result.getTitle());
        assertFalse(result.getPinned());
        assertNotNull(result.getEvents());
        assertTrue(result.getEvents().isEmpty());
        verify(eventRepository, never()).findByIdIn(any());
        verify(compilationRepository, times(1)).save(any(Compilation.class));
        verify(statsService, never()).getViews(any());
    }

    @Test
    void deleteCompilation_ExistingId_DeletesSuccessfully() {
        // Given
        Long compilationId = 1L;
        when(compilationRepository.existsById(compilationId)).thenReturn(true);

        // When
        compilationService.deleteCompilation(compilationId);

        // Then
        verify(compilationRepository, times(1)).existsById(compilationId);
        verify(compilationRepository, times(1)).deleteById(compilationId);
    }

    @Test
    void deleteCompilation_NonExistingId_ThrowsNotFoundException() {
        // Given
        Long compilationId = 999L;
        when(compilationRepository.existsById(compilationId)).thenReturn(false);

        // When & Then
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> compilationService.deleteCompilation(compilationId));
        assertEquals("Compilation with id=999 was not found", exception.getMessage());
        verify(compilationRepository, times(1)).existsById(compilationId);
        verify(compilationRepository, never()).deleteById(compilationId);
    }

    @Test
    void updateCompilation_ValidUpdate_ReturnsUpdatedCompilation() {
        // Given
        Long compilationId = 1L;

        Compilation existingCompilation = Compilation.builder()
                .id(compilationId)
                .events(new HashSet<>())
                .pinned(false)
                .title("Old Title")
                .build();

        UpdateCompilationRequest updateRequest = new UpdateCompilationRequest();
        updateRequest.setEvents(List.of(1L, 2L));
        updateRequest.setPinned(true);
        updateRequest.setTitle("New Title");

        Event event1 = Event.builder().id(1L).title("Event 1").build();
        Event event2 = Event.builder().id(2L).title("Event 2").build();

        when(compilationRepository.findById(compilationId)).thenReturn(Optional.of(existingCompilation));
        when(eventRepository.findByIdIn(List.of(1L, 2L))).thenReturn(List.of(event1, event2));
        when(compilationRepository.save(any(Compilation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(statsService.getViews(List.of(1L, 2L))).thenReturn(Map.of(1L, 100L, 2L, 200L));

        // When
        CompilationDto result = compilationService.updateCompilation(compilationId, updateRequest);

        // Then
        assertNotNull(result);
        assertEquals("New Title", result.getTitle());
        assertTrue(result.getPinned());
        assertEquals(2, result.getEvents().size());
        verify(compilationRepository, times(1)).findById(compilationId);
        verify(eventRepository, times(1)).findByIdIn(List.of(1L, 2L));
        verify(compilationRepository, times(1)).save(any(Compilation.class));
        verify(statsService, times(1)).getViews(List.of(1L, 2L));
    }

    @Test
    void updateCompilation_PartialUpdate_ReturnsPartiallyUpdatedCompilation() {
        // Given
        Long compilationId = 1L;

        Event existingEvent = Event.builder().id(10L).title("Existing Event").build();
        Compilation existingCompilation = Compilation.builder()
                .id(compilationId)
                .events(new HashSet<>(List.of(existingEvent)))
                .pinned(false)
                .title("Old Title")
                .build();

        UpdateCompilationRequest updateRequest = new UpdateCompilationRequest();
        updateRequest.setTitle("New Title");
        // events и pinned остаются null, значит не меняются

        when(compilationRepository.findById(compilationId)).thenReturn(Optional.of(existingCompilation));
        when(compilationRepository.save(any(Compilation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        CompilationDto result = compilationService.updateCompilation(compilationId, updateRequest);

        // Then
        assertNotNull(result);
        assertEquals("New Title", result.getTitle());
        assertFalse(result.getPinned()); // осталось false
        assertEquals(1, result.getEvents().size()); // остался 1 event
        verify(compilationRepository, times(1)).findById(compilationId);
        verify(eventRepository, never()).findByIdIn(any());
        verify(compilationRepository, times(1)).save(any(Compilation.class));
    }

    @Test
    void updateCompilation_NonExistingId_ThrowsNotFoundException() {
        // Given
        Long compilationId = 999L;
        UpdateCompilationRequest updateRequest = new UpdateCompilationRequest();

        when(compilationRepository.findById(compilationId)).thenReturn(Optional.empty());

        // When & Then
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> compilationService.updateCompilation(compilationId, updateRequest));
        assertEquals("Compilation with id=999 was not found", exception.getMessage());
        verify(compilationRepository, times(1)).findById(compilationId);
        verify(compilationRepository, never()).save(any());
    }

    @Test
    void getCompilations_WithPinnedFilter_ReturnsFilteredCompilations() {
        // Given
        Boolean pinned = true;

        Event event1 = Event.builder().id(1L).title("Event 1").build();
        Event event2 = Event.builder().id(2L).title("Event 2").build();

        Compilation compilation1 = Compilation.builder()
                .id(1L)
                .events(new HashSet<>(List.of(event1)))
                .pinned(true)
                .title("Pinned Compilation 1")
                .build();

        Compilation compilation2 = Compilation.builder()
                .id(2L)
                .events(new HashSet<>(List.of(event2)))
                .pinned(true)
                .title("Pinned Compilation 2")
                .build();

        when(compilationRepository.findByPinned(pinned, PageRequest.of(0, 10)))
                .thenReturn(List.of(compilation1, compilation2));
        when(statsService.getViews(List.of(1L))).thenReturn(Map.of(1L, 100L));
        when(statsService.getViews(List.of(2L))).thenReturn(Map.of(2L, 200L));

        // When
        List<CompilationDto> result = compilationService.getCompilations(pinned, 0, 10);

        // Then
        assertEquals(2, result.size());
        assertEquals("Pinned Compilation 1", result.get(0).getTitle());
        assertEquals("Pinned Compilation 2", result.get(1).getTitle());
        assertTrue(result.get(0).getPinned());
        assertTrue(result.get(1).getPinned());
        verify(compilationRepository, times(1)).findByPinned(pinned, PageRequest.of(0, 10));
        verify(statsService, times(2)).getViews(any());
    }

    @Test
    void getCompilations_WithoutPinnedFilter_ReturnsAllCompilations() {
        // Given
        Compilation compilation1 = Compilation.builder()
                .id(1L)
                .events(new HashSet<>())
                .pinned(true)
                .title("Pinned Compilation")
                .build();

        Compilation compilation2 = Compilation.builder()
                .id(2L)
                .events(new HashSet<>())
                .pinned(false)
                .title("Not Pinned Compilation")
                .build();

        Page<Compilation> page = new PageImpl<>(List.of(compilation1, compilation2));

        when(compilationRepository.findAll(PageRequest.of(0, 10))).thenReturn(page);

        // When
        List<CompilationDto> result = compilationService.getCompilations(null, 0, 10);

        // Then
        assertEquals(2, result.size());
        assertEquals("Pinned Compilation", result.get(0).getTitle());
        assertEquals("Not Pinned Compilation", result.get(1).getTitle());
        verify(compilationRepository, times(1)).findAll(PageRequest.of(0, 10));
        verify(compilationRepository, never()).findByPinned(any(), any());
    }

    @Test
    void getCompilation_ExistingId_ReturnsCompilationDto() {
        // Given
        Long compilationId = 1L;

        Event event1 = Event.builder().id(1L).title("Event 1").build();
        Event event2 = Event.builder().id(2L).title("Event 2").build();

        Compilation compilation = Compilation.builder()
                .id(compilationId)
                .events(new HashSet<>(List.of(event1, event2)))
                .pinned(true)
                .title("Test Compilation")
                .build();

        when(compilationRepository.findById(compilationId)).thenReturn(Optional.of(compilation));
        when(statsService.getViews(List.of(1L, 2L))).thenReturn(Map.of(1L, 100L, 2L, 200L));

        // When
        CompilationDto result = compilationService.getCompilation(compilationId);

        // Then
        assertNotNull(result);
        assertEquals(compilationId, result.getId());
        assertEquals("Test Compilation", result.getTitle());
        assertTrue(result.getPinned());
        assertEquals(2, result.getEvents().size());
        verify(compilationRepository, times(1)).findById(compilationId);
        verify(statsService, times(1)).getViews(List.of(1L, 2L));
    }

    @Test
    void getCompilation_NonExistingId_ThrowsNotFoundException() {
        // Given
        Long compilationId = 999L;

        when(compilationRepository.findById(compilationId)).thenReturn(Optional.empty());

        // When & Then
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> compilationService.getCompilation(compilationId));
        assertEquals("Compilation with id=999 was not found", exception.getMessage());
        verify(compilationRepository, times(1)).findById(compilationId);
        verify(statsService, never()).getViews(any());
    }

    @Test
    void getCompilations_EmptyResult_ReturnsEmptyList() {
        // Given
        Page<Compilation> emptyPage = new PageImpl<>(List.of());
        when(compilationRepository.findAll(PageRequest.of(0, 10))).thenReturn(emptyPage);

        // When
        List<CompilationDto> result = compilationService.getCompilations(null, 0, 10);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(compilationRepository, times(1)).findAll(PageRequest.of(0, 10));
        verify(statsService, never()).getViews(any());
    }

    @Test
    void getCompilations_WithPagination_ReturnsCorrectPage() {
        // Given
        Compilation compilation3 = Compilation.builder()
                .id(3L)
                .events(new HashSet<>())
                .pinned(false)
                .title("Compilation 3")
                .build();

        Compilation compilation4 = Compilation.builder()
                .id(4L)
                .events(new HashSet<>())
                .pinned(false)
                .title("Compilation 4")
                .build();

        Page<Compilation> page = new PageImpl<>(List.of(compilation3, compilation4));

        when(compilationRepository.findAll(PageRequest.of(1, 2))).thenReturn(page);

        // When
        List<CompilationDto> result = compilationService.getCompilations(null, 2, 2);

        // Then
        assertEquals(2, result.size());
        assertEquals(3L, result.get(0).getId());
        assertEquals(4L, result.get(1).getId());
        verify(compilationRepository, times(1)).findAll(PageRequest.of(1, 2));
    }

    @Test
    void createCompilation_EmptyEventsList_ReturnsCompilationWithoutEvents() {
        // Given
        NewCompilationDto newCompilationDto = new NewCompilationDto(
                List.of(),
                false,
                "Empty Events Compilation"
        );

        Compilation compilation = Compilation.builder()
                .id(1L)
                .events(new HashSet<>())
                .pinned(false)
                .title("Empty Events Compilation")
                .build();

        when(compilationRepository.save(any(Compilation.class))).thenReturn(compilation);

        // When
        CompilationDto result = compilationService.createCompilation(newCompilationDto);

        // Then
        assertNotNull(result);
        assertEquals("Empty Events Compilation", result.getTitle());
        verify(eventRepository, times(1)).findByIdIn(List.of());
        verify(compilationRepository, times(1)).save(any(Compilation.class));
    }
}