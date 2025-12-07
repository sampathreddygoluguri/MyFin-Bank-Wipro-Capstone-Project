package com.myfinbank.auth.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;


@FeignClient(name = "admin-service", url = "http://localhost:8082")
public interface AdminAuthClient {
    @PostMapping("/api/admin/validate")
    Map<String, Object> validate(@RequestBody Map<String, String> payload);
}
