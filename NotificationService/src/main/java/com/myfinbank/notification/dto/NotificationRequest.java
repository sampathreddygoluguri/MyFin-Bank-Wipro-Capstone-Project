package com.myfinbank.notification.dto;

import lombok.Data;

@Data
public class NotificationRequest {
    private Long customerId;
    private String customerName;
    private String accountNumber;
    private String message;
}
