package com.myfinbank.admin.feign;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PutMapping;

import com.myfinbank.admin.dto.AccountDto;
import com.myfinbank.admin.dto.CustomerDto;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "customer-service", contextId = "customerClient")
public interface CustomerClient {

    @PutMapping("/api/customers/{id}/deactivate")
    void deactivateCustomer(@PathVariable Long id);
    
    @PutMapping("/api/customers/{id}/activate")
    void activateCustomer(@PathVariable Long id);

    @GetMapping("/api/customers")
    List<CustomerDto> getAll();
    
    @GetMapping("/api/customers/accountdetails/{customerId}")
    List<AccountDto> getAccounts(@PathVariable Long customerId);
    


}
