package com.myfinbank.customer.controller;

import com.myfinbank.customer.dto.*;
import com.myfinbank.customer.entity.Account;
import com.myfinbank.customer.entity.Customer;
import com.myfinbank.customer.entity.FixedDeposit;
import com.myfinbank.customer.entity.Loan;
import com.myfinbank.customer.entity.RecurringDeposit;
import com.myfinbank.customer.entity.TransactionRecord;
import com.myfinbank.customer.feign.AuthClient;
import com.myfinbank.customer.repository.LoanRepository;
import com.myfinbank.customer.repository.TransactionRecordRepository;
import com.myfinbank.customer.service.AccountService;
import com.myfinbank.customer.service.CustomerService;
import com.myfinbank.customer.service.InvestmentService;
import com.myfinbank.customer.service.LoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customers")
@CrossOrigin(origins = "http://localhost:8082")
@RequiredArgsConstructor
public class CustomerController {

	private final CustomerService customerService;
	private final AccountService accountService;
	private final InvestmentService investmentService;
	private final LoanService loanService;
	private final LoanRepository loanRepo;
	private final TransactionRecordRepository txRepo;

	private final AuthClient authClient;

	@PostMapping("/register")
	public ResponseEntity<?> register(@RequestBody CustomerRegisterRequest req) {
		return ResponseEntity.ok(customerService.register(req));
	}

	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody CustomerLoginRequest req) {

		customerService.verifyLogin(req);

		// get customer entity
		Customer c = customerService.findByEmail(req.getEmail());

		// Prepare request for AuthService
		CustomerLoginRequest authReq = new CustomerLoginRequest();
		authReq.setEmail(req.getEmail());
		authReq.setPassword(req.getPassword());
		authReq.setRole("CUSTOMER");

		// Call Auth service for JWT token
		CustomerLoginResponse token = authClient.login(authReq);

		// Add customer ID in response
		token.setUserId(c.getId());

		return ResponseEntity.ok(token);
	}

	/**
	 * Get a customer by id. - ADMIN can get any customer. - CUSTOMER can only get
	 * their own customer record.
	 */
	@GetMapping("/{id}")
	public ResponseEntity<?> getCustomer(@PathVariable Long id, Authentication authentication) {
		// authentication.getName() should be the subject (email) when token filter sets
		// Authentication
		String caller = (authentication != null) ? authentication.getName() : null;

		// if no auth info -> forbidden
		if (caller == null) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Not authenticated"));
		}

		// if caller is ADMIN, allow
		boolean isAdmin = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

		if (isAdmin) {
			Customer c = customerService.getCustomer(id);
			return ResponseEntity.ok(c);
		}

		// For customers: ensure the requested id matches the caller's id
		Customer callerCustomer = customerService.findByEmail(caller);
		if (callerCustomer == null || !callerCustomer.getId().equals(id)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN)
					.body(Map.of("message", "You are not allowed to access this resource"));
		}

		// allowed: return customer
		Customer c = customerService.getCustomer(id);
		return ResponseEntity.ok(c);
	}

	/**
	 * Only ADMIN can list all customers.
	 */
	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping
	public List<Customer> getAll() {
		return customerService.getAll();
	}

	/**
	 * Update customer: allow ADMIN to update any customer. Allow CUSTOMER to update
	 * only their own profile (we check identity).
	 */
	@PutMapping("/{id}")
	public ResponseEntity<?> update(@PathVariable Long id, @RequestBody CustomerUpdateRequest req,
			Authentication authentication) {

		String caller = (authentication != null) ? authentication.getName() : null;
		if (caller == null) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Not authenticated"));
		}

		boolean isAdmin = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

		if (!isAdmin) {
			// customer: must match id
			Customer callerCustomer = customerService.findByEmail(caller);
			if (callerCustomer == null || !callerCustomer.getId().equals(id)) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Not allowed"));
			}
		}

		return ResponseEntity.ok(customerService.updateCustomer(id, req));
	}

	/**
	 * Deactivate / Activate — only ADMIN. We also keep method-security as backup
	 * via @PreAuthorize.
	 */
	@PreAuthorize("hasRole('ADMIN')")
	@PutMapping("/{id}/deactivate")
	public ResponseEntity<?> deactivate(@PathVariable Long id) {
		customerService.deactivate(id);
		return ResponseEntity.ok(Map.of("message", "Customer deactivated"));
	}

	@PreAuthorize("hasRole('ADMIN')")
	@PutMapping("/{id}/activate")
	public ResponseEntity<?> activate(@PathVariable Long id) {
		customerService.activate(id);
		return ResponseEntity.ok(Map.of("message", "Customer activated"));
	}

	// ==== Accounts =====

	/**
	 * Create account for a customer: ADMIN may create for any customer. Customer
	 * may create only for themselves.
	 */
	@PostMapping("/accounts")
	public ResponseEntity<?> createAccount(@RequestBody Map<String, Object> body, Authentication authentication) {
		Long customerId = Long.valueOf(body.get("customerId").toString());
		String type = (String) body.getOrDefault("type", "SAVINGS");
		BigDecimal initial = body.get("initial") == null ? BigDecimal.ZERO
				: new BigDecimal(body.get("initial").toString());

		// auth check
		String caller = (authentication != null) ? authentication.getName() : null;
		if (caller == null) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Not authenticated"));
		}
		boolean isAdmin = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

		if (!isAdmin) {
			Customer callerCustomer = customerService.findByEmail(caller);
			if (callerCustomer == null || !callerCustomer.getId().equals(customerId)) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Not allowed"));
			}
		}

		var account = accountService.createAccountForCustomer(customerId, type, initial);
		return ResponseEntity.ok(account);
	}

	@GetMapping("/accountdetails/{customerId}")
	public ResponseEntity<?> getCustomerAccounts(@PathVariable Long customerId) {

		Customer customer = customerService.getCustomer(customerId);

		String loggedEmail = getLoggedInEmail();

		boolean owner = customer.getEmail().equalsIgnoreCase(loggedEmail);
		boolean admin = isAdmin();

		if (!owner && !admin) {
			return ResponseEntity.status(403)
					.body(Map.of("error", "Access denied. Only account owner or admin can view this data."));
		}

		return ResponseEntity.ok(accountService.getAccountsByCustomer(customerId));
	}

	/**
	 * deposit/withdraw/transfer: basic ownership check so customer can only operate
	 * on accounts they own (unless ADMIN).
	 *
	 * Here we expect accountService to throw meaningful exceptions if account
	 * missing. For ownership checks we ask accountService for the account ->owner
	 * id (assumed API).
	 */
	// DEPOSIT
	@PostMapping("/accounts/deposit")
	public ResponseEntity<?> depositByAccountNumber(@RequestBody Map<String, Object> body,
			Authentication authentication) {

		String acctNum = (String) body.get("accountNumber");
		String confirmAcct = (String) body.getOrDefault("confirmAccountNumber", null);

		if (acctNum == null || acctNum.isBlank()) {
			return ResponseEntity.badRequest().body(Map.of("message", "accountNumber is required"));
		}

		if (confirmAcct != null && !confirmAcct.equals(acctNum)) {
			return ResponseEntity.badRequest().body(Map.of("message", "confirmAccountNumber does not match"));
		}

		BigDecimal amount;
		try {
			amount = new BigDecimal(body.get("amount").toString());
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(Map.of("message", "Invalid amount"));
		}

		String remark = (String) body.getOrDefault("remark", "Deposit");

		// Fetch account using number
		Account account = accountService.getAccountByNumber(acctNum);

		// Ownership check
		if (!isAdminForAuth(authentication)) {
			String email = authentication.getName();
			Customer caller = customerService.findByEmail(email);
			if (!caller.getId().equals(account.getCustomerId())) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Not allowed"));
			}
		}

		// Call NEW accountService method
		TransactionRecord tx = accountService.deposit(acctNum, amount, remark);

		return ResponseEntity.ok(tx);
	}

	// WITHDRAW
	@PostMapping("/accounts/withdraw")
	public ResponseEntity<?> withdrawByAccountNumber(@RequestBody Map<String, Object> body,
			Authentication authentication) {

		String acctNum = (String) body.get("accountNumber");
		String confirmAcct = (String) body.get("confirmAccountNumber");

		if (acctNum == null || acctNum.isBlank()) {
			return ResponseEntity.badRequest().body(Map.of("message", "accountNumber is required"));
		}

		if (confirmAcct != null && !confirmAcct.equals(acctNum)) {
			return ResponseEntity.badRequest().body(Map.of("message", "confirmAccountNumber does not match"));
		}

		BigDecimal amount;
		try {
			amount = new BigDecimal(body.get("amount").toString());
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(Map.of("message", "Invalid amount"));
		}

		String remark = (String) body.getOrDefault("remark", "Withdraw");

		Account account = accountService.getAccountByNumber(acctNum);

		// Ownership check
		if (!isAdminForAuth(authentication)) {
			String email = authentication.getName();
			Customer caller = customerService.findByEmail(email);
			if (!caller.getId().equals(account.getCustomerId())) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Not allowed"));
			}
		}

		// New withdraw method
		TransactionRecord tx = accountService.withdraw(acctNum, amount, remark);

		return ResponseEntity.ok(tx);
	}

	// TRANSFER
	@PostMapping("/accounts/transfer")
	public ResponseEntity<?> transferByAccountNumber(@RequestBody Map<String, Object> body,
			Authentication authentication) {

		String fromAcct = (String) body.get("fromAccountNumber");
		String toAcct = (String) body.get("toAccountNumber");

		if (fromAcct == null || fromAcct.isBlank() || toAcct == null || toAcct.isBlank()) {
			return ResponseEntity.badRequest().body(Map.of("message", "Both account numbers are required"));
		}

		BigDecimal amount;
		try {
			amount = new BigDecimal(body.get("amount").toString());
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(Map.of("message", "Invalid amount"));
		}

		String remark = (String) body.getOrDefault("remark", "Transfer");

		Account from = accountService.getAccountByNumber(fromAcct);
		Account to = accountService.getAccountByNumber(toAcct);

		// Ownership check
		if (!isAdminForAuth(authentication)) {
			String email = authentication.getName();
			Customer caller = customerService.findByEmail(email);
			if (!caller.getId().equals(from.getCustomerId())) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Not allowed"));
			}
		}

		// NEW accountService.transfer()
		TransactionRecord tx = accountService.transfer(fromAcct, toAcct, amount, remark);

		return ResponseEntity.ok(tx);
	}
	
	@GetMapping("/accounts/{accountNumber}/transactions")
	public ResponseEntity<?> getTransactionsByAccountNumber(
	        @PathVariable String accountNumber,
	        Authentication authentication) {

	    // 1️ Authenticate
	    if (authentication == null) {
	        return ResponseEntity.status(HttpStatus.FORBIDDEN)
	                .body(Map.of("message", "Not authenticated"));
	    }

	    String callerEmail = authentication.getName();
	    Customer caller = customerService.findByEmail(callerEmail);
	    if (caller == null) {
	        return ResponseEntity.status(HttpStatus.FORBIDDEN)
	                .body(Map.of("message", "Not authenticated"));
	    }

	    // 2️ Validate account exists
	    Account acct = accountService.getAccountByNumber(accountNumber);
	    if (acct == null) {
	        return ResponseEntity.status(HttpStatus.NOT_FOUND)
	                .body(Map.of("message", "Account not found"));
	    }

	    // 3️ Customer can ONLY see **his own account** transactions  
	    if (!acct.getCustomerId().equals(caller.getId())) {
	        return ResponseEntity.status(HttpStatus.FORBIDDEN)
	                .body(Map.of("message", "You are not allowed to view this account"));
	    }

	    // 4️ Fetch transactions by accountNumber
	    List<TransactionRecord> txs =
	            txRepo.findByFromAccountNumberOrToAccountNumber(accountNumber, accountNumber);

	    return ResponseEntity.ok(txs);
	}


	// ==== Loans ======

	@PostMapping("/loans/apply")
	public ResponseEntity<?> applyLoan(@RequestBody Map<String, Object> body, Authentication authentication) {

		Long customerId = Long.valueOf(body.get("customerId").toString());
		BigDecimal amount = new BigDecimal(body.get("amount").toString());
		Double rate = Double.valueOf(body.get("annualInterestRate").toString());
		Integer months = Integer.valueOf(body.get("months").toString());

		// only customer themselves (or admin) can apply
		String caller = (authentication != null) ? authentication.getName() : null;
		if (caller == null) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Not authenticated"));
		}
		boolean isAdmin = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
		if (!isAdmin) {
			Customer callerCustomer = customerService.findByEmail(caller);
			if (callerCustomer == null || !callerCustomer.getId().equals(customerId)) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Not allowed"));
			}
		}

		Loan loan = loanService.applyLoan(customerId, amount, rate, months);
		double emi = loanService.calculateEMI(amount, rate, months);

		return ResponseEntity.ok(Map.of("loan", loan, "emi", emi));
	}

	@GetMapping("/loans/customer/{customerId}")
	public ResponseEntity<?> getLoans(@PathVariable Long customerId, Authentication authentication) {
		// allow admin or owner
		String caller = (authentication != null) ? authentication.getName() : null;
		if (caller == null) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Not authenticated"));
		}
		boolean isAdmin = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
		if (!isAdmin) {
			Customer callerCustomer = customerService.findByEmail(caller);
			if (callerCustomer == null || !callerCustomer.getId().equals(customerId)) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Not allowed"));
			}
		}

		List<Loan> loans = loanRepo.findByCustomerId(customerId);
		return ResponseEntity.ok(loans);
	}
	
    @PostMapping("/loans/{loanId}/pay")
    public ResponseEntity<?> payLoan(@PathVariable Long loanId,
                                     @RequestBody Map<String, Object> body,
                                     Authentication authentication) {

        String caller = (authentication != null) ? authentication.getName() : null;
        if (caller == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Not authenticated"));
        }

        Loan loan = loanRepo.findById(loanId).orElseThrow(() -> new RuntimeException("Loan not found"));
        // Only owner or admin
        boolean isAdmin = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            Customer callerCustomer = customerService.findByEmail(caller);
            if (callerCustomer == null || !callerCustomer.getId().equals(loan.getCustomerId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Not allowed"));
            }
        }

        // read amount from body
        BigDecimal amount;
        try {
            amount = new BigDecimal(body.get("amount").toString());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid amount"));
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid amount"));
        }

        // Find primary account for borrower
        List<Account> accounts = accountService.getAccountsByCustomer(loan.getCustomerId());
        if (accounts == null || accounts.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "No account for customer"));
        }
        Account primary = accounts.get(0);
        String acctNum = primary.getAccountNumber();

        // Check sufficient balance by attempting withdrawal (accountService.withdraw will throw if insufficient)
        TransactionRecord tx = accountService.withdraw(acctNum, amount, "Loan payment - Loan#" + loan.getId());

        // Update loan totals
        BigDecimal newTotalPaid = (loan.getTotalPaid() == null ? BigDecimal.ZERO : loan.getTotalPaid()).add(amount);
        BigDecimal newRemaining = (loan.getRemainingAmount() == null ? loan.getAmount() : loan.getRemainingAmount()).subtract(amount);

        loan.setTotalPaid(newTotalPaid);
        loan.setRemainingAmount(newRemaining.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : newRemaining);

        if (loan.getRemainingAmount().compareTo(BigDecimal.ZERO) <= 0) {
            loan.setStatus("CLOSED");
            loan.setClosedAt(java.time.LocalDateTime.now());
        }

        loanRepo.save(loan);

        return ResponseEntity.ok(Map.of("message", "Payment processed", "transaction", tx, "loan", loan));
    }


	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping("/loans")
	public ResponseEntity<?> getAllLoans() {

		List<Loan> loans = loanRepo.findAll();
		List<Map<String, Object>> result = new ArrayList<>();

		for (Loan loan : loans) {

			double emi = loanService.calculateEMI(loan.getAmount(), loan.getAnnualInterestRate(), loan.getMonths());

			List<Account> accounts = accountService.getAccountsByCustomer(loan.getCustomerId());
			String accountNumber = "No Account";

			if (accounts != null && !accounts.isEmpty()) {
				accountNumber = accounts.get(0).getAccountNumber();
			}

			Map<String, Object> map = new HashMap<>();
			map.put("id", loan.getId());
			map.put("customerId", loan.getCustomerId());
			map.put("accountNumber", accountNumber);
			map.put("amount", loan.getAmount());
			map.put("annualInterestRate", loan.getAnnualInterestRate());
			map.put("months", loan.getMonths());
			map.put("emi", emi);
			map.put("status", loan.getStatus());

			result.add(map);
		}

		return ResponseEntity.ok(result);
	}
	

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/loans/{loanId}/decide")
    public ResponseEntity<?> decideLoan(@PathVariable Long loanId,
                                        @RequestParam boolean approve,
                                        @RequestParam String remark) {

        Loan loan = loanRepo.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        if (approve) {
            // set APPROVED and disburse amount
            loan.setStatus("APPROVED");
            loan.setRemark(remark);
            loan.setDecidedAt(java.time.LocalDateTime.now());

            // find primary savings (or first account)
            List<Account> accounts = accountService.getAccountsByCustomer(loan.getCustomerId());
            if (accounts == null || accounts.isEmpty()) {
                // If no account exists, create a savings account and then disburse
                Account created = accountService.createAccountForCustomer(loan.getCustomerId(), "SAVINGS", BigDecimal.ZERO);
                accounts = List.of(created);
            }

            Account primary = accounts.get(0);
            // deposit loan amount into primary account (this creates a transaction record)
            accountService.deposit(primary.getAccountNumber(), loan.getAmount(), "Loan disbursement: Loan#" + loan.getId());

            loan.setDisbursedAt(java.time.LocalDateTime.now());
        } else {
            loan.setStatus("DENIED");
            loan.setRemark(remark);
            loan.setDecidedAt(java.time.LocalDateTime.now());
        }

        loanRepo.save(loan);

        return ResponseEntity.ok(Map.of("message", "Loan decision saved"));
    }

	
	// ==== Investments ======
	
		// create FD
		@PostMapping("/investments/fd")
		public ResponseEntity<?> createFd(@RequestBody CreateFdRequest req, Authentication authentication){
		    if (authentication == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message","Not authenticated"));
		    String email = authentication.getName();
		    Customer caller = customerService.findByEmail(email);

		    int months = req.getMonths() == null ? 6 : req.getMonths();
		    boolean useFull = Boolean.TRUE.equals(req.getUseFullBalance());
		    BigDecimal initial = req.getInitial();

		    FixedDeposit fd = investmentService.createFd(caller.getId(), req.getAccountNumber(), months, useFull, initial);
		    return ResponseEntity.ok(fd);
		}

		// create RD
		@PostMapping("/investments/rd")
		public ResponseEntity<?> createRd(@RequestBody CreateRdRequest req, Authentication authentication){
		    if (authentication == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message","Not authenticated"));
		    String email = authentication.getName();
		    Customer caller = customerService.findByEmail(email);

		    int months = req.getMonths() == null ? 6 : req.getMonths();
		    LocalDate first = req.getFirstInstallmentDate();
		    RecurringDeposit rd = investmentService.createRd(caller.getId(), req.getAccountNumber(), months, req.getMonthlyInstallment(), first);
		    return ResponseEntity.ok(rd);
		}

		// pay RD installment
		@PostMapping("/investments/rd/{rdId}/pay")
		public ResponseEntity<?> payRd(@PathVariable Long rdId, @RequestBody PayRdRequest req, Authentication authentication){
		    if (authentication == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message","Not authenticated"));
		    String email = authentication.getName();
		    Customer caller = customerService.findByEmail(email);

		    TransactionRecord tx = investmentService.payRdInstallment(caller.getId(), rdId, req.getAmount());
		    return ResponseEntity.ok(tx);
		}

		// list investments
		@GetMapping("/investments")
		public ResponseEntity<?> listInvestments(Authentication authentication){
		    if (authentication == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message","Not authenticated"));
		    String email = authentication.getName();
		    Customer caller = customerService.findByEmail(email);

		    var fds = investmentService.listFds(caller.getId());
		    var rds = investmentService.listRds(caller.getId());

		    return ResponseEntity.ok(Map.of("fds", fds, "rds", rds));
		}

	private boolean isAdminForAuth(Authentication authentication) {
		if (authentication == null)
			return false;
		return authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
	}

	private String getLoggedInEmail() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		return auth.getName(); // email extracted from TokenValidationFilter
	}

	private boolean isAdmin() {
		return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
				.anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
	}

}
