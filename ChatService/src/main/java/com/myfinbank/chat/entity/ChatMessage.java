package com.myfinbank.chat.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_message")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long conversationId; // you can generate conversation id as combination of user ids or separate table
    private Long fromId;
    private Long toId;

    @Column(length = 2000)
    private String content;

    private boolean seen = false;

    private LocalDateTime timestamp;
}
