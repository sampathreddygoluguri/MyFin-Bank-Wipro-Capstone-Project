package com.myfinbank.customer.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "loan")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Loan {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long customerId;
    private BigDecimal amount;
    private Double annualInterestRate; // percent
    private Integer months;

    // Derived/stored fields
    private Double emi; // monthly EMI (double is ok)
    private BigDecimal remainingAmount;
    private BigDecimal totalPaid;

    private String status; // PENDING, APPROVED, DENIED, CLOSED
    private String remark;

    private LocalDateTime appliedAt;
    private LocalDateTime decidedAt;
    private LocalDateTime disbursedAt;
    private LocalDateTime closedAt;

    @PrePersist
    public void onApply() {
        appliedAt = LocalDateTime.now();
        if (status == null) status = "PENDING";
        if (totalPaid == null) totalPaid = BigDecimal.ZERO;
        if (remainingAmount == null && amount != null) remainingAmount = amount;
    }
}
