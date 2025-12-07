package com.myfinbank.customer.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "recurring_deposit")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecurringDeposit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long customerId;
    private String accountNumber; // savings account used for installments

    private BigDecimal monthlyInstallment;
    private Integer months;
    private Double annualRatePercent;
    private LocalDate startDate;
    private LocalDate nextInstallmentDate;
    private BigDecimal totalPaid; // total amount paid into RD so far
    private String status; // ACTIVE, COMPLETED, DEFAULTED

    private LocalDate createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDate.now();
    }
}
