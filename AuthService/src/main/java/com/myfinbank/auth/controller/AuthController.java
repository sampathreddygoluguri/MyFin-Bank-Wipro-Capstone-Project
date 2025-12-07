package com.myfinbank.auth.controller;

import com.myfinbank.auth.dto.AuthResponse;
import com.myfinbank.auth.dto.LoginRequest;
import com.myfinbank.auth.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {

        String role = req.getRole() == null ? "CUSTOMER" : req.getRole().toUpperCase();

        String subject = req.getEmail();
        String jwtRole = role.equals("ADMIN") ? "ROLE_ADMIN" : "ROLE_CUSTOMER";

        String token = jwtUtil.generateToken(subject, jwtRole);

        return ResponseEntity.ok(new AuthResponse(token, subject, jwtRole));
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestParam String token) {

        if (token == null || !jwtUtil.validate(token)) {
            return ResponseEntity.ok(Map.of("valid", false));
        }

        return ResponseEntity.ok(Map.of(
                "valid", true,
                "subject", jwtUtil.extractSubject(token),
                "roles", List.of(jwtUtil.extractRole(token))
        ));
    }
}
