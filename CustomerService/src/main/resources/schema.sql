CREATE TABLE customer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);


CREATE TABLE account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    account_type VARCHAR(50) NOT NULL,
    balance DECIMAL(15,2) DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_account_customer FOREIGN KEY (customer_id) REFERENCES customer(id)
);

CREATE TABLE transaction_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL,
    transaction_type VARCHAR(50),
    amount DECIMAL(15,2),
    remark VARCHAR(255),
    transaction_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tr_account FOREIGN KEY (account_id) REFERENCES account(id)
);

CREATE TABLE loan (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    amount DECIMAL(15,2),
    annual_rate DOUBLE,
    months INT,
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    decision_remark VARCHAR(255),
    CONSTRAINT fk_loan_customer FOREIGN KEY (customer_id) REFERENCES customer(id)
);

CREATE TABLE fixed_deposit (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  customer_id BIGINT,
  account_number VARCHAR(255),
  principal DECIMAL(19,2),
  annual_rate_percent DOUBLE,
  months INT,
  start_date DATE,
  maturity_date DATE,
  maturity_amount DECIMAL(19,2),
  status VARCHAR(20),
  created_at DATE
);

CREATE TABLE recurring_deposit (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  customer_id BIGINT,
  account_number VARCHAR(255),
  monthly_installment DECIMAL(19,2),
  months INT,
  annual_rate_percent DOUBLE,
  start_date DATE,
  next_installment_date DATE,
  total_paid DECIMAL(19,2),
  status VARCHAR(20),
  created_at DATE
);
