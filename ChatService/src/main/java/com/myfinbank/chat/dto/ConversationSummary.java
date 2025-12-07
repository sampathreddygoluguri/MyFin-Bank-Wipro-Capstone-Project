package com.myfinbank.chat.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ConversationSummary {
    private Long conversationId;
    private Long customerId;
    private String latestMessage;
    private LocalDateTime latestTimestamp;
    private Long unreadCount;
}
