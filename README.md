# MyFin-Bank-Wipro-Capstone-Project
Repository For Capstone project developed during Wipro Pre-Skilling Training

A complete enterprise-level digital banking platform built using Spring Boot Microservices, Spring Security + JWT, Feign Clients, Eureka Service Discovery, and a clean HTML/CSS/JS frontend.
This system supports Customer Banking, Admin Management, Investments, Loans, Notifications, Chat, Notifications and Email Alerts.

🚀 Project Overview
MyFin Bank is an online banking system that enables customers to:
1. Perform basic banking operations
2. Apply for and manage loans
3. Create Fixed Deposits (FD) and Recurring Deposits (RD)
4. Make secure transactions
5. Chat with bank administrators
6. Receive notifications and email alerts

Administrators can:
1. Manage customers and their accounts
2. Approve/deny loans
3. Monitor transactions
4. Receive alerts when a customer’s balance reaches zero
5. Chat with customers

The project consists of the following independently deployable microservices:

MyFin-Bank/
 ├── CustomerService       → Customer operations, accounts, RD/FD, transactions  
 ├── AdminService          → Admin CRUD, loan approvals  
 ├── AuthService           → JWT authentication for Admin & Customer  
 ├── EmailService          → Sends email alerts (e.g., zero balance alerts)  
 ├── NotificationService   → Sends admin notifications (e.g., zero balance)  
 ├── ChatService           → Real-time chat (WebSocket)  
 ├── EurekaServer          → Service discovery  

🧱 Technologies Used
Backend
1. Spring Boot
2. Spring Security
3. Spring Cloud Netflix Eureka
4. OpenFeign
5. JWT
6. JPA / Hibernate
7. MySQL

Frontend
1. HTML
2. CSS
3. JavaScript
4. Bootstrap
5. Custom Chat Widget

📦 How to Run
1️⃣ Start Eureka Server
cd EurekaServer
mvn spring-boot:run

2️⃣ Start all microservices:
cd AuthService
cd CustomerService
cd AdminService
cd EmailService
cd NotificationService
cd ChatService
mvn spring-boot:run

3️⃣ Open Frontend:

Open /customer/login.html or /admin/login.html in browser.

 
