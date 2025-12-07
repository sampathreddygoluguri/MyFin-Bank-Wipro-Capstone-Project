package com.myfinbank.customer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.myfinbank.customer.entity.FixedDeposit;
import java.util.List;

public interface FixedDepositRepository extends JpaRepository<FixedDeposit, Long> {
    List<FixedDeposit> findByCustomerId(Long customerId);
    List<FixedDeposit> findByStatusAndMaturityDateLessThanEqual(String status, java.time.LocalDate date);
}
