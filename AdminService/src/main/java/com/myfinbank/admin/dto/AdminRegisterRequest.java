package com.myfinbank.admin.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminRegisterRequest {
    private String name;
    private String email;
    private String password;
}
