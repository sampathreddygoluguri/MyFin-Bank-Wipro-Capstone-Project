package com.myfinbank.customer.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CreateRdRequest {
    private String accountNumber;
    private Integer months; // 6 or 12
    private BigDecimal monthlyInstallment; // required
    private java.time.LocalDate firstInstallmentDate; // optional - defaults to today
}
