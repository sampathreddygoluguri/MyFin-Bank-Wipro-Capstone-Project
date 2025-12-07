package com.myfinbank.customer.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.myfinbank.customer.entity.TransactionRecord;

public interface TransactionRecordRepository extends JpaRepository<TransactionRecord, Long> {

    Optional<TransactionRecord> findByTransactionId(String transactionId);

    List<TransactionRecord> findByFromAccountNumberOrToAccountNumber(
            String fromAcc, String toAcc
    );
}
