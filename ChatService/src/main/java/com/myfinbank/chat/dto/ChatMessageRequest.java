package com.myfinbank.chat.dto;

import lombok.Data;

@Data
public class ChatMessageRequest {
    private Long conversationId; // optional, server can create if null
    private Long fromId;
    private Long toId;
    private String content;
}
