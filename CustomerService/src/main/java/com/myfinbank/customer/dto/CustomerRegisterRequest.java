package com.myfinbank.customer.dto;

import lombok.Data;

@Data
public class CustomerRegisterRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private String phone;
}
