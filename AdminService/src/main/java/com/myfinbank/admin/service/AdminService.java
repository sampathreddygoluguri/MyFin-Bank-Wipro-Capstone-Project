package com.myfinbank.admin.service;

import com.myfinbank.admin.dto.AccountDto;
import com.myfinbank.admin.dto.AdminLoginRequest;
import com.myfinbank.admin.dto.AdminRegisterRequest;
import com.myfinbank.admin.dto.CustomerDto;
import com.myfinbank.admin.dto.CustomerWithAccountsDto;
import com.myfinbank.admin.entity.Admin;
import com.myfinbank.admin.exception.AuthenticationException;
import com.myfinbank.admin.exception.BadRequestException;
import com.myfinbank.admin.feign.CustomerClient;
import com.myfinbank.admin.repository.AdminRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminRepository repo;
    private final BCryptPasswordEncoder encoder;
    private final CustomerClient customerClient;

    public Map<String, String> register(AdminRegisterRequest req) {

        repo.findByEmail(req.getEmail())
                .ifPresent(a -> { throw new BadRequestException("Email already exists"); });

        Admin admin = Admin.builder()
                .name(req.getName())
                .email(req.getEmail())
                .passwordHash(encoder.encode(req.getPassword()))
                .build();

        repo.save(admin);

        return Map.of("message", "Admin registered successfully");
    }

    public void verifyLogin(AdminLoginRequest req) {
        Admin admin = repo.findByEmail(req.getEmail())
                .orElseThrow(() -> new AuthenticationException("Invalid credentials"));

        if (!encoder.matches(req.getPassword(), admin.getPasswordHash())) {
            throw new AuthenticationException("Invalid credentials");
        }
    }
    
    public Admin findByEmail(String email){
        return repo.findByEmail(email).orElseThrow();
    }
    
    public List<CustomerWithAccountsDto> getAllCustomersWithAccounts() {
        List<CustomerDto> customers = customerClient.getAll();
        List<CustomerWithAccountsDto> result = new ArrayList<>();

        for (CustomerDto c : customers) {
            List<AccountDto> accounts = customerClient.getAccounts(c.getId());
            result.add(new CustomerWithAccountsDto(
                c.getId(),
                c.getFirstName(),
                c.getLastName(),
                c.getEmail(),
                c.getPhone(),
                c.getStatus(),
                accounts
            ));
        }

        return result;
    }

}
