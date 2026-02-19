package ru.practicum.ewm.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.dto.CommentDto;
import ru.practicum.ewm.dto.NewCommentDto;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.model.*;
import ru.practicum.ewm.repository.CommentRepository;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private CommentService commentService;

    @Test
    void addComment_success() {
        User user = User.builder().id(1L).build();
        Event event = Event.builder().id(2L).state(EventState.PUBLISHED).build();
        NewCommentDto dto = new NewCommentDto();
        dto.setText("text");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(eventRepository.findById(2L)).thenReturn(Optional.of(event));
        when(commentRepository.save(any(Comment.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CommentDto result = commentService.addComment(1L, 2L, dto);

        assertEquals("text", result.getText());
        verify(commentRepository, times(1)).save(any(Comment.class));
    }

    @Test
    void addComment_userNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        NewCommentDto dto = new NewCommentDto();
        dto.setText("text");

        assertThrows(NotFoundException.class, () ->
                commentService.addComment(1L, 2L, dto)
        );
    }

    @Test
    void addComment_eventNotPublished() {
        User user = User.builder().id(1L).build();
        Event event = Event.builder()
                .id(2L)
                .state(EventState.PENDING)
                .build();

        NewCommentDto dto = new NewCommentDto();
        dto.setText("text");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(eventRepository.findById(2L)).thenReturn(Optional.of(event));

        assertThrows(ConflictException.class, () ->
                commentService.addComment(1L, 2L, dto));
    }

    @Test
    void getComments_success() {
        User author = User.builder()
                .id(1L)
                .name("John")
                .build();

        Comment comment = Comment.builder()
                .id(1L)
                .text("text")
                .author(author)
                .created(LocalDateTime.now())
                .build();


        when(commentRepository.findByEventIdOrderByCreatedDesc(1L))
                .thenReturn(List.of(comment));

        List<CommentDto> result = commentService.getComments(1L);

        assertEquals(1, result.size());
        assertEquals("text", result.get(0).getText());
    }

    @Test
    void deleteComment_success() {
        User user = User.builder().id(1L).build();
        Comment comment = Comment.builder()
                .id(1L)
                .author(user)
                .build();

        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        commentService.deleteComment(1L, 1L);

        verify(commentRepository).delete(comment);
    }

    @Test
    void deleteComment_notAuthor() {
        User author = User.builder().id(1L).build();
        Comment comment = Comment.builder()
                .id(1L)
                .author(author)
                .build();

        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        assertThrows(ConflictException.class, () ->
                commentService.deleteComment(2L, 1L));
    }
}
