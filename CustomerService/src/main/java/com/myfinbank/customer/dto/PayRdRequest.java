package com.myfinbank.customer.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PayRdRequest {
    private Long rdId;
    private BigDecimal amount; // must equal monthlyInstallment or allow customizing
}
