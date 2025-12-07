package com.myfinbank.customer.service;

import com.myfinbank.customer.entity.*;
import com.myfinbank.customer.repository.*;
import com.myfinbank.customer.exception.BadRequestException;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InvestmentService {

    private final FixedDepositRepository fdRepo;
    private final RecurringDepositRepository rdRepo;
    private final AccountService accountService;
    private final TransactionRecordRepository txRepo;

    // config/rates (could be externalized)
    private final double FD_RATE_6M = 6.0;   // percent p.a.
    private final double FD_RATE_12M = 7.0;
    private final double RD_RATE_6M = 5.0;
    private final double RD_RATE_12M = 6.0;

    private double fdRateForMonths(int months) {
        return (months == 6) ? FD_RATE_6M : FD_RATE_12M;
    }
    private double rdRateForMonths(int months) {
        return (months == 6) ? RD_RATE_6M : RD_RATE_12M;
    }

    // utility: compound monthly maturity calculation
    private BigDecimal calcFdMaturity(BigDecimal principal, double annualRatePercent, int months) {
        double r = annualRatePercent / 12.0 / 100.0;
        double P = principal.doubleValue();
        double A = P * Math.pow(1 + r, months);
        return BigDecimal.valueOf(A).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    // RD maturity (future value of series): approximate formula for monthly compounding
    private BigDecimal calcRdMaturity(BigDecimal monthly, double annualRatePercent, int months) {
        double r = annualRatePercent / 12.0 / 100.0;
        double m = monthly.doubleValue();
        double n = months;
        // future value of an ordinary annuity: M * [ ( (1+r)^n - 1 ) / r ]
        if (r == 0) {
            double A = m * n;
            return BigDecimal.valueOf(A).setScale(2, java.math.RoundingMode.HALF_UP);
        }
        double A = m * (Math.pow(1 + r, n) - 1) / r;
        // approximate adding interest for last compounding
        return BigDecimal.valueOf(A).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    @Transactional
    public FixedDeposit createFd(Long customerId, String accountNumber, int months, boolean useFullBalance, BigDecimal initial) {
        // validate account exists & ownership
        Account acc = accountService.getAccountByNumber(accountNumber);
        if (!acc.getCustomerId().equals(customerId)) throw new BadRequestException("Not allowed");

        BigDecimal principal;
        if (useFullBalance) {
            principal = acc.getBalance();
            if (principal.compareTo(BigDecimal.ZERO) <= 0) throw new BadRequestException("Insufficient balance");
        } else {
            if (initial == null || initial.compareTo(BigDecimal.ZERO) <= 0) throw new BadRequestException("Invalid amount");
            principal = initial;
            if (acc.getBalance().compareTo(principal) < 0) throw new BadRequestException("Insufficient funds");
        }

        // debit savings account
        accountService.withdraw(accountNumber, principal, "FD creation debit");

        double rate = fdRateForMonths(months);
        LocalDate start = LocalDate.now();
        LocalDate maturity = start.plusMonths(months);
        BigDecimal maturityAmount = calcFdMaturity(principal, rate, months);

        FixedDeposit fd = FixedDeposit.builder()
                .customerId(customerId)
                .accountNumber(accountNumber)
                .principal(principal)
                .annualRatePercent(rate)
                .months(months)
                .startDate(start)
                .maturityDate(maturity)
                .maturityAmount(maturityAmount)
                .status("ACTIVE")
                .build();

        FixedDeposit saved = fdRepo.save(fd);

        // record tx (to special FD-<id>)
        txRepo.save(TransactionRecord.builder()
                .transactionId("TXN-FD-" + saved.getId() + "-" + java.util.UUID.randomUUID().toString().substring(0,6))
                .fromAccountNumber(accountNumber)
                .toAccountNumber("FD-" + saved.getId())
                .amount(principal)
                .type("FD_CREATE")
                .remark("FD created id:" + saved.getId())
                .build());

        return saved;
    }

    @Transactional
    public RecurringDeposit createRd(Long customerId, String accountNumber, int months, BigDecimal monthlyInstallment, LocalDate firstDate) {
        Account acc = accountService.getAccountByNumber(accountNumber);
        if (!acc.getCustomerId().equals(customerId)) throw new BadRequestException("Not allowed");

        if (monthlyInstallment == null || monthlyInstallment.compareTo(BigDecimal.ZERO) <= 0)
            throw new BadRequestException("Invalid monthly installment");

        double rate = rdRateForMonths(months);
        LocalDate start = LocalDate.now();
        LocalDate nextDate = (firstDate != null) ? firstDate : start;

        RecurringDeposit rd = RecurringDeposit.builder()
                .customerId(customerId)
                .accountNumber(accountNumber)
                .monthlyInstallment(monthlyInstallment)
                .months(months)
                .annualRatePercent(rate)
                .startDate(start)
                .nextInstallmentDate(nextDate)
                .totalPaid(BigDecimal.ZERO)
                .status("ACTIVE")
                .build();

        RecurringDeposit saved = rdRepo.save(rd);
        return saved;
    }

    @Transactional
    public TransactionRecord payRdInstallment(Long customerId, Long rdId, BigDecimal amount) {
        RecurringDeposit rd = rdRepo.findById(rdId).orElseThrow(() -> new BadRequestException("RD not found"));
        if (!rd.getCustomerId().equals(customerId)) throw new BadRequestException("Not allowed");
        if (!"ACTIVE".equals(rd.getStatus())) throw new BadRequestException("RD not active");

        if (amount == null) amount = rd.getMonthlyInstallment();
        if (amount.compareTo(rd.getMonthlyInstallment()) != 0) {
            // We require exact payment equal to scheduled installment for simplicity
            throw new BadRequestException("Installment must equal monthlyInstallment");
        }

        // check balance
        Account acc = accountService.getAccountByNumber(rd.getAccountNumber());
        if (acc.getBalance().compareTo(amount) < 0) throw new BadRequestException("Insufficient funds");

        // debit savings account
        accountService.withdraw(acc.getAccountNumber(), amount, "RD installment debit for RD:" + rdId);

        // update RD
        BigDecimal newTotal = rd.getTotalPaid().add(amount);
        rd.setTotalPaid(newTotal);

        // calculate next installment date
        rd.setNextInstallmentDate(rd.getNextInstallmentDate().plusMonths(1));

        // if totalPaid reached expected total -> mark COMPLETE (approx check)
        BigDecimal expectedTotal = rd.getMonthlyInstallment().multiply(BigDecimal.valueOf(rd.getMonths()));
        if (newTotal.compareTo(expectedTotal) >= 0) {
            rd.setStatus("COMPLETED");
        }

        rdRepo.save(rd);

        TransactionRecord tx = txRepo.save(TransactionRecord.builder()
                .transactionId("TXN-RD-" + rd.getId() + "-" + java.util.UUID.randomUUID().toString().substring(0,6))
                .fromAccountNumber(acc.getAccountNumber())
                .toAccountNumber("RD-" + rd.getId())
                .amount(amount)
                .type("RD_INSTALLMENT")
                .remark("RD installment id:" + rd.getId())
                .build());

        return tx;
    }

    // Get all investments for a customer
    public List<FixedDeposit> listFds(Long customerId) {
        return fdRepo.findByCustomerId(customerId);
    }
    public List<RecurringDeposit> listRds(Long customerId) {
        return rdRepo.findByCustomerId(customerId);
    }

    // Maturation routine: mature FDs that are due and credit savings account
    @Transactional
    public void matureDueFds() {
        LocalDate today = LocalDate.now();
        var due = fdRepo.findByStatusAndMaturityDateLessThanEqual("ACTIVE", today);
        for (FixedDeposit fd : due) {
            // credit the maturityAmount into the savings account
            accountService.deposit(fd.getAccountNumber(), fd.getMaturityAmount(), "FD matured id:" + fd.getId());
            fd.setStatus("MATURED");
            fdRepo.save(fd);

            txRepo.save(TransactionRecord.builder()
                    .transactionId("TXN-FD-MAT-" + fd.getId() + "-" + java.util.UUID.randomUUID().toString().substring(0,6))
                    .fromAccountNumber("FD-" + fd.getId())
                    .toAccountNumber(fd.getAccountNumber())
                    .amount(fd.getMaturityAmount())
                    .type("FD_MATURE")
                    .remark("FD matured id:" + fd.getId())
                    .build());
        }
    }

    // Maturation for RD (if any finalization needed) - simplified: if completed, compute matured amount and credit
    @Transactional
    public void finalizeCompletedRds() {
        LocalDate today = LocalDate.now();
        var due = rdRepo.findByStatusAndNextInstallmentDateLessThanEqual("ACTIVE", today);
        // This method is left intentionally simple — real RD maturity calculation requires schedule
    }
}
