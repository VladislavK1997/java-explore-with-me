package ru.practicum.ewm.controller.comment;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.CommentDto;
import ru.practicum.ewm.dto.NewCommentDto;
import ru.practicum.ewm.service.CommentService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping
public class CommentController {


    private final CommentService commentService;


    @PostMapping("/users/{userId}/events/{eventId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto addComment(@PathVariable Long userId,
                                 @PathVariable Long eventId,
                                 @RequestBody @Valid NewCommentDto dto) {
        return commentService.addComment(userId, eventId, dto);
    }


    @GetMapping("/events/{eventId}/comments")
    public List<CommentDto> getComments(@PathVariable Long eventId) {
        return commentService.getComments(eventId);
    }


    @DeleteMapping("/users/{userId}/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long userId,
                       @PathVariable Long commentId) {
        commentService.deleteComment(userId, commentId);
    }
}