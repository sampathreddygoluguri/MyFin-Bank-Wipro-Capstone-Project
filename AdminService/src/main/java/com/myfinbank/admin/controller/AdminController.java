package com.myfinbank.admin.controller;

import com.myfinbank.admin.config.TokenContextHolder;
import com.myfinbank.admin.dto.AdminLoginRequest;
import com.myfinbank.admin.dto.AdminRegisterRequest;
import com.myfinbank.admin.dto.LoginResponse;
import com.myfinbank.admin.entity.Admin;
import com.myfinbank.admin.feign.AuthClient;
import com.myfinbank.admin.feign.CustomerClient;
import com.myfinbank.admin.feign.CustomerLoanClient;
import com.myfinbank.admin.service.AdminService;

import lombok.RequiredArgsConstructor;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final AuthClient authClient;
    private final CustomerClient customerClient;
    private final CustomerLoanClient customerLoanClient;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AdminRegisterRequest req) {
        return ResponseEntity.ok(adminService.register(req));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AdminLoginRequest req) {

        adminService.verifyLogin(req);
        
     // get admin entity
        Admin a = adminService.findByEmail(req.getEmail());


        AdminLoginRequest authReq = new AdminLoginRequest();
        authReq.setEmail(req.getEmail());
        authReq.setPassword(req.getPassword());
        authReq.setRole("ADMIN");

        LoginResponse token = authClient.login(authReq);
        
     // Add Admin ID in response
        token.setUserId(a.getId());

        return ResponseEntity.ok(token);
    }


    @PutMapping("/customer/{id}/deactivate")
    public ResponseEntity<?> deactivate(@PathVariable Long id,
                                        @RequestHeader("Authorization") String token) {

        TokenContextHolder.setToken(token.replace("Bearer ", ""));

        customerClient.deactivateCustomer(id);

        TokenContextHolder.clear();

        return ResponseEntity.ok(Map.of("message", "Customer deactivated"));
    }
    
    @PutMapping("/customer/{id}/activate")
    public ResponseEntity<?> activateCustomer(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {

        TokenContextHolder.setToken(token.replace("Bearer ", ""));

        customerClient.activateCustomer(id);

        TokenContextHolder.clear();

        return ResponseEntity.ok(Map.of("message", "Customer activated"));
    }
    
    @GetMapping("/customers")
    public ResponseEntity<?> getAllCustomers(@RequestHeader("Authorization") String token) {
        TokenContextHolder.setToken(token.replace("Bearer ", ""));
        var customers = adminService.getAllCustomersWithAccounts();
        TokenContextHolder.clear();
        return ResponseEntity.ok(customers);
    }



    @PutMapping("/loans/{loanId}/decide")
    public ResponseEntity<?> decideLoan(@PathVariable Long loanId,
                                        @RequestParam boolean approve,
                                        @RequestParam String remark, @RequestHeader("Authorization") String token) {

    	TokenContextHolder.setToken(token.replace("Bearer ", ""));
    	customerLoanClient.decideLoan(loanId, approve, remark);
    	TokenContextHolder.clear();
        return ResponseEntity.ok("Admin decision processed");
    }
    
    @GetMapping("/loans")
    public ResponseEntity<?> getAllLoans(@RequestHeader("Authorization") String token) {
        TokenContextHolder.setToken(token.replace("Bearer ", ""));
        var loans = customerLoanClient.getAllLoans();
        TokenContextHolder.clear();
        return ResponseEntity.ok(loans);
    }

}
