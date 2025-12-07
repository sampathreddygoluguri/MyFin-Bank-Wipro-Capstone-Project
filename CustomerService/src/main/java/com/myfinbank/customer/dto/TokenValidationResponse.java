package com.myfinbank.customer.dto;

import lombok.Data;
import java.util.List;

@Data
public class TokenValidationResponse {
    private boolean valid;
    private String subject;      // e.g. customerId or username
    private List<String> roles;  // e.g. ["ROLE_CUSTOMER"]
    // optional: expiry, issuedAt
}
