package com.myfinbank.auth.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;


@FeignClient(name = "customer-service", url = "http://localhost:8081")
public interface CustomerAuthClient {
    @PostMapping("/api/customers/validate")
    Map<String, Object> validate(@RequestBody Map<String, String> payload);
}
