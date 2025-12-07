package com.myfinbank.admin.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AccountDto {
    private Long id;
    private String accountNumber;
    private BigDecimal balance;
}
