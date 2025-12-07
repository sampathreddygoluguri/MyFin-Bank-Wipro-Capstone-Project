package com.myfinbank.customer.dto;

import lombok.Data;

@Data
public class CustomerUpdateRequest {
    private String firstName;
    private String lastName;
    private String phone;
}
