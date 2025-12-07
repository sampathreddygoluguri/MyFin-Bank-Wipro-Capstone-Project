package com.myfinbank.chat.dto;

import lombok.Data;

@Data
public class StartChatRequest {
    private Long adminId;
    private Long customerId;
}
