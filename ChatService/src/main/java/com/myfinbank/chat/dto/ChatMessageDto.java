package com.myfinbank.chat.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatMessageDto {
    private Long id;
    private Long conversationId;
    private Long fromId;
    private Long toId;
    private String content;
    private boolean seen;
    private LocalDateTime timestamp;
}
