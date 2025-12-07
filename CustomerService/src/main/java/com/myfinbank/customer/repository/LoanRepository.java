package com.myfinbank.customer.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.myfinbank.customer.entity.Loan;

public interface LoanRepository extends JpaRepository<Loan, Long> {
    List<Loan> findByCustomerId(Long customerId);
    List<Loan> findByStatus(String status);
}