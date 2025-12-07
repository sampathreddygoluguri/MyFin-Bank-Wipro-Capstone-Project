package com.myfinbank.customer.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CreateFdRequest {
    private String accountNumber;
    private Integer months; // 6 or 12
    private Boolean useFullBalance = false; // if true, take whole savings balance as principal
    private BigDecimal initial; // optional, used if useFullBalance=false
}
