package com.myfinbank.customer.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.myfinbank.customer.dto.EmailRequest;

@FeignClient(name = "email-service")
public interface EmailClient {

    @PostMapping("/email/send-balance-alert")
    String sendBalanceAlert(@RequestBody EmailRequest request);
}
