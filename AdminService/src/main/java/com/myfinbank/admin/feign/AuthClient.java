package com.myfinbank.admin.feign;

import com.myfinbank.admin.dto.AdminLoginRequest;
import com.myfinbank.admin.dto.LoginResponse;
import com.myfinbank.admin.dto.TokenValidationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "auth-service")
public interface AuthClient {

    @PostMapping("/auth/login")
    LoginResponse login(@RequestBody AdminLoginRequest req);

    @GetMapping("/auth/validate")
    TokenValidationResponse validateToken(@RequestParam("token") String token);
}
