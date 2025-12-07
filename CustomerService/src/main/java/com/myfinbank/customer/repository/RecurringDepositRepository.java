package com.myfinbank.customer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.myfinbank.customer.entity.RecurringDeposit;
import java.util.List;
import java.time.LocalDate;

public interface RecurringDepositRepository extends JpaRepository<RecurringDeposit, Long> {
    List<RecurringDeposit> findByCustomerId(Long customerId);
    List<RecurringDeposit> findByStatusAndNextInstallmentDateLessThanEqual(String status, LocalDate date);
}
