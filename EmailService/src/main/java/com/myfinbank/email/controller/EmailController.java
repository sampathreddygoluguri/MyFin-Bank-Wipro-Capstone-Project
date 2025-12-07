package com.myfinbank.email.controller;

import com.myfinbank.email.dto.EmailRequest;
import com.myfinbank.email.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/email")
@RequiredArgsConstructor
public class EmailController {

    private final EmailService emailService;

    @PostMapping("/send-balance-alert")
    public String sendBalanceAlert(@RequestBody EmailRequest request) {

        String subject = "ALERT: Account Balance Zero";

        String customerBody = "Dear " + request.getCustomerName() +
                ",\nYour account (" + request.getAccountNumber() + ") balance has reached ZERO.";

        String adminBody = "Admin,\nCustomer: " + request.getCustomerName() +
                " \nAccount balance has reached ZERO.\nAccount Number: " + request.getAccountNumber();


        emailService.sendEmail(request.getCustomerEmail(), subject, customerBody);
        emailService.sendEmail(request.getAdminEmail(), subject, adminBody);
        
        return "Emails sent to Customer & Admin";
    }
}
