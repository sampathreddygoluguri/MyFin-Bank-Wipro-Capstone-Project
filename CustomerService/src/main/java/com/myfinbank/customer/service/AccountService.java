package com.myfinbank.customer.service;

import com.myfinbank.customer.dto.EmailRequest;
import com.myfinbank.customer.dto.NotificationRequest;
import com.myfinbank.customer.entity.*;
import com.myfinbank.customer.exception.BadRequestException;
import com.myfinbank.customer.feign.EmailClient;
import com.myfinbank.customer.feign.NotificationClient;
import com.myfinbank.customer.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepo;
    private final TransactionRecordRepository txRepo;
    private final CustomerRepository customerRepo;
    private final EmailClient emailClient; 
    private final NotificationClient notificationClient;


    private String genAccountNumber() {
        return "ACCT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String genTxnId() {
        return "TXN-" + java.time.LocalDate.now().toString().replace("-", "") + "-"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    @Transactional
    public Account createAccountForCustomer(Long customerId, String type, BigDecimal initial) {
        Account a = Account.builder()
                .customerId(customerId)
                .accountNumber(genAccountNumber())
                .accountType(type)
                .balance(initial == null ? BigDecimal.ZERO : initial)
                .build();
        return accountRepo.save(a);
    }

    public Account getAccountByNumber(String accountNumber) {
        return accountRepo.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new BadRequestException("Account not found: " + accountNumber));
    }

    @Transactional
    public TransactionRecord deposit(String accountNumber, BigDecimal amount, String remark) {

        if (amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new BadRequestException("Invalid amount");

        Account acc = getAccountByNumber(accountNumber);

        acc.setBalance(acc.getBalance().add(amount));
        accountRepo.save(acc);

        TransactionRecord tx = TransactionRecord.builder()
                .transactionId(genTxnId())
                .fromAccountNumber(null)
                .toAccountNumber(accountNumber)
                .amount(amount)
                .type("DEPOSIT")
                .remark(remark)
                .build();

        return txRepo.save(tx);
    }

    @Transactional
    public TransactionRecord withdraw(String accountNumber, BigDecimal amount, String remark) {

        if (amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new BadRequestException("Invalid amount");

        Account acc = getAccountByNumber(accountNumber);

        if (acc.getBalance().compareTo(amount) < 0)
            throw new BadRequestException("Insufficient funds");

        acc.setBalance(acc.getBalance().subtract(amount));
        accountRepo.save(acc);
        
        if (acc.getBalance().compareTo(BigDecimal.ZERO) == 0) {
            sendZeroBalanceEmail(acc);
        }

        TransactionRecord tx = TransactionRecord.builder()
                .transactionId(genTxnId())
                .fromAccountNumber(accountNumber)
                .toAccountNumber(null)
                .amount(amount)
                .type("WITHDRAW")
                .remark(remark)
                .build();

        return txRepo.save(tx);
    }

    @Transactional
    public TransactionRecord transfer(String fromNumber, String toNumber, BigDecimal amount, String remark) {

        if (amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new BadRequestException("Invalid amount");

        Account src = getAccountByNumber(fromNumber);
        Account dst = getAccountByNumber(toNumber);

        if (src.getBalance().compareTo(amount) < 0)
            throw new BadRequestException("Insufficient funds");

        src.setBalance(src.getBalance().subtract(amount));
        dst.setBalance(dst.getBalance().add(amount));

        accountRepo.save(src);
        accountRepo.save(dst);
        if (src.getBalance().compareTo(BigDecimal.ZERO) == 0) {
            sendZeroBalanceEmail(src);
        }

        TransactionRecord tx = TransactionRecord.builder()
                .transactionId(genTxnId())
                .fromAccountNumber(fromNumber)
                .toAccountNumber(toNumber)
                .amount(amount)
                .type("TRANSFER")
                .remark(remark)
                .build();

        return txRepo.save(tx);
    }

    public List<Account> getAccountsByCustomer(Long customerId) {
        return accountRepo.findByCustomerId(customerId);
    }

    public Long getOwnerCustomerIdForAccountNumber(String accountNumber) {
        return getAccountByNumber(accountNumber).getCustomerId();
    }
    
    private void sendZeroBalanceEmail(Account acc) {
        try {
            Customer customer = customerRepo.findById(acc.getCustomerId())
                    .orElse(null);

            // --- 1. SEND EMAIL ---
            EmailRequest req = new EmailRequest();
            req.setCustomerEmail(customer != null ? customer.getEmail() : "customer@mail.com");
            req.setAdminEmail("rijarej723@httpsu.com");  //set the email of admin
            req.setCustomerName(customer != null ? customer.getFirstName() : "Customer");
            req.setAccountNumber(acc.getAccountNumber());
            req.setBalance(acc.getBalance().doubleValue());

            emailClient.sendBalanceAlert(req);
            
         // --- 2. SEND NOTIFICATION TO ADMIN DASHBOARD ---
            NotificationRequest nreq = new NotificationRequest();
            nreq.setCustomerId(acc.getCustomerId());
            nreq.setCustomerName(customer != null ? customer.getFirstName() : "Customer");
            nreq.setAccountNumber(acc.getAccountNumber());
            nreq.setMessage("Balance for account " + acc.getAccountNumber() + " has reached ZERO.");

            notificationClient.saveZeroBalanceNotification(nreq);

        } catch (Exception e) {
            System.out.println("Email/Notification service error: " + e.getMessage());
        }
    }

}
