package com.myfinbank.admin.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class LoanDto {
    private Long id;
    private Long customerId;
    private String accountNumber;  
    private BigDecimal amount;
    private Double annualInterestRate;
    private Integer months;
    private String status;
    private Double emi;
}
