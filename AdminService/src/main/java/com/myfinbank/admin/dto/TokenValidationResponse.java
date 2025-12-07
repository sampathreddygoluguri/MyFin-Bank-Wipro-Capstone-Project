package com.myfinbank.admin.dto;

import lombok.Data;
import java.util.List;

@Data
public class TokenValidationResponse {
    private boolean valid;
    private String subject;
    private List<String> roles;
}
