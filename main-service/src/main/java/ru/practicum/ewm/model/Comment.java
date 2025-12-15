package ru.practicum.ewm.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "comments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(nullable = false, length = 2000)
    private String text;


    @ManyToOne(optional = false)
    @JoinColumn(name = "event_id")
    private Event event;


    @ManyToOne(optional = false)
    @JoinColumn(name = "author_id")
    private User author;


    @Column(nullable = false)
    private LocalDateTime created;
}