package com.myfinbank.admin.feign;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.myfinbank.admin.dto.LoanDto;

@FeignClient(name = "customer-service", contextId = "customerLoanClient")
public interface CustomerLoanClient {
	@PutMapping("/api/customers/loans/{loanId}/decide")
	void decideLoan(@PathVariable Long loanId, @RequestParam boolean approve, @RequestParam String remark);

	@GetMapping("/loans/pending")
	List<Object> getPendingLoans();
	
	@GetMapping("/api/customers/loans")
    List<LoanDto> getAllLoans();
}
