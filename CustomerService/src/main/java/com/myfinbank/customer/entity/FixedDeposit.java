package com.myfinbank.customer.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "fixed_deposit")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FixedDeposit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long customerId;
    private String accountNumber; // savings account that funded this FD

    private BigDecimal principal;
    private Double annualRatePercent;
    private Integer months;
    private LocalDate startDate;
    private LocalDate maturityDate;
    private BigDecimal maturityAmount; // computed at creation
    private String status; // ACTIVE, MATURED, CANCELLED

    private LocalDate createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDate.now();
    }
}
