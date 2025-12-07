package com.myfinbank.customer.service;

import com.myfinbank.customer.dto.CustomerLoginRequest;
import com.myfinbank.customer.dto.CustomerRegisterRequest;
import com.myfinbank.customer.dto.CustomerUpdateRequest;
import com.myfinbank.customer.entity.Customer;
import com.myfinbank.customer.exception.AuthenticationException;
import com.myfinbank.customer.exception.BadRequestException;
import com.myfinbank.customer.exception.ResourceNotFoundException;
import com.myfinbank.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.cloud.openfeign.EnableFeignClients;


import jakarta.transaction.Transactional;
import java.util.Map;
import java.util.List;

@Service
@RequiredArgsConstructor
@EnableFeignClients(basePackages = "com.myfinbank.customer.feign")
public class CustomerService {

    private final CustomerRepository customerRepo;
    private final BCryptPasswordEncoder encoder;

    public Map<String, String> register(CustomerRegisterRequest req) {

        customerRepo.findByEmail(req.getEmail())
                .ifPresent(c -> { throw new BadRequestException("Email already exists"); });

        Customer c = Customer.builder()
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .email(req.getEmail())
                .passwordHash(encoder.encode(req.getPassword()))
                .phone(req.getPhone())
                .status("ACTIVE")
                .build();

        customerRepo.save(c);
        return Map.of("message", "Customer registered successfully");
    }

    // used before calling AuthService
    public void verifyLogin(CustomerLoginRequest req) {

        Customer c = customerRepo.findByEmail(req.getEmail())
                .orElseThrow(() -> new AuthenticationException("Invalid credentials"));

        if (!encoder.matches(req.getPassword(), c.getPasswordHash()))
            throw new AuthenticationException("Invalid credentials");
        
        if (!"ACTIVE".equalsIgnoreCase(c.getStatus())) {
            throw new AuthenticationException("Your account is deactivated. Contact admin for activation.");
        }
    }

    public Customer getCustomer(Long id) {
        return customerRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
    }

    @Transactional
    public Customer updateCustomer(Long id, CustomerUpdateRequest req) {
        Customer c = getCustomer(id);

        if (req.getFirstName() != null) c.setFirstName(req.getFirstName());
        if (req.getLastName() != null) c.setLastName(req.getLastName());
        if (req.getPhone() != null) c.setPhone(req.getPhone());

        return customerRepo.save(c);
    }

    public void deactivate(Long id) {
        Customer c = getCustomer(id);
        c.setStatus("DEACTIVATED");
        customerRepo.save(c);
    }

    public void activate(Long id) {
        Customer c = getCustomer(id);
        c.setStatus("ACTIVE");
        customerRepo.save(c);
    }

    public List<Customer> getAll() {
        return customerRepo.findAll();
    }
    public Customer findByEmail(String email){
        return customerRepo.findByEmail(email).orElseThrow();
    }

}
