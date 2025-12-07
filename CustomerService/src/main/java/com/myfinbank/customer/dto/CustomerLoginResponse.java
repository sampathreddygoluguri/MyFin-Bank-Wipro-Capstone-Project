package com.myfinbank.customer.dto;

import lombok.Data;

@Data
public class CustomerLoginResponse {
    private String token;
    private Long userId;
    private String role;
}
