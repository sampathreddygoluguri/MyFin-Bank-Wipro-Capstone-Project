package com.myfinbank.customer.dto;

import lombok.Data;

@Data
public class EmailRequest {
    private String customerEmail;
    private String adminEmail;
    private String customerName;
    private String accountNumber;
    private double balance;
}
