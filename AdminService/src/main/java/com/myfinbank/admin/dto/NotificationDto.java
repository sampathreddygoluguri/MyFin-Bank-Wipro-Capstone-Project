package com.myfinbank.admin.dto;

import lombok.Data;

@Data
public class NotificationDto {
    private Long id;
    private Long customerId;
    private String customerName;
    private String accountNumber;
    private String message;
    private boolean seen;
    private String timestamp;
}
