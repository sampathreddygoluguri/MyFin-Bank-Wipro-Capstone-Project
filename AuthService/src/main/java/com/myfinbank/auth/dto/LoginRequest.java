package com.myfinbank.auth.dto;
import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String password;
    // optional: "CUSTOMER" or "ADMIN"
    private String role;
}
