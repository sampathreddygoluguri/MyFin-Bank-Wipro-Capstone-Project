package com.myfinbank.customer.feign;

import com.myfinbank.customer.dto.CustomerLoginRequest;
import com.myfinbank.customer.dto.CustomerLoginResponse;
import com.myfinbank.customer.dto.TokenValidationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "auth-service")
public interface AuthClient {

    @PostMapping("/auth/login")
    CustomerLoginResponse login(@RequestBody CustomerLoginRequest req);

    @GetMapping("/auth/validate")
    TokenValidationResponse validateToken(@RequestParam("token") String token);
}
