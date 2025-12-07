package com.myfinbank.customer.service;

import com.myfinbank.customer.entity.Loan;
import com.myfinbank.customer.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class LoanService {
    private final LoanRepository loanRepo;

    @Transactional
    public Loan applyLoan(Long customerId, BigDecimal amount, Double annualRate, Integer months) {
        double emi = calculateEMI(amount, annualRate, months);
        BigDecimal totalPayable = BigDecimal.valueOf(emi * months);
        Loan loan = Loan.builder()
                .customerId(customerId)
                .amount(amount)
                .annualInterestRate(annualRate)
                .months(months)
                .emi(emi)
                .remainingAmount(totalPayable)
                .totalPaid(BigDecimal.ZERO)
                .status("PENDING")
                .build();
        return loanRepo.save(loan);
    }

    public Loan getLoan(Long id) { return loanRepo.findById(id).orElseThrow(); }

    @Transactional
    public Loan decideLoan(Long loanId, boolean approve, String remark) {
        Loan l = loanRepo.findById(loanId).orElseThrow();
        l.setStatus(approve ? "APPROVED" : "DENIED");
        l.setRemark(remark);
        l.setDecidedAt(java.time.LocalDateTime.now());
        // NOTE: disbursement handled by controller so accountService is available there
        return loanRepo.save(l);
    }

    public double calculateEMI(BigDecimal principal, double annualRatePercent, int months) {
        double r = annualRatePercent / 12.0 / 100.0;
        double P = principal.doubleValue();
        if (r == 0) return P / months;
        double emi = (P * r * Math.pow(1 + r, months)) / (Math.pow(1 + r, months) - 1);
        return emi;
    }
}
