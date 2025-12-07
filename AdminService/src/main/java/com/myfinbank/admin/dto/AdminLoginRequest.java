package com.myfinbank.admin.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminLoginRequest {
    private String email;
    private String password;
    private String role;
}
