package ru.practicum.ewm.service;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.dto.CommentDto;
import ru.practicum.ewm.dto.NewCommentDto;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.CommentMapper;
import ru.practicum.ewm.model.Comment;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.EventState;
import ru.practicum.ewm.model.User;
import ru.practicum.ewm.repository.CommentRepository;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {


    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;


    @Transactional
    public CommentDto addComment(Long userId, Long eventId, NewCommentDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));


        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));


        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Cannot comment unpublished event");
        }


        Comment comment = Comment.builder()
                .text(dto.getText())
                .author(user)
                .event(event)
                .created(LocalDateTime.now())
                .build();


        return CommentMapper.toDto(commentRepository.save(comment));
    }


    public List<CommentDto> getComments(Long eventId) {
        return commentRepository.findByEventIdOrderByCreatedDesc(eventId)
                .stream()
                .map(CommentMapper::toDto)
                .toList();
    }


    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment not found"));


        if (!comment.getAuthor().getId().equals(userId)) {
            throw new ConflictException("Only author can delete comment");
        }


        commentRepository.delete(comment);
    }
}