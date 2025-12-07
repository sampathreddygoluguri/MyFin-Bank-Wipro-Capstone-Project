package com.myfinbank.admin.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final TokenValidationFilter tokenFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http.csrf(cs -> cs.disable());
        http.cors(cors -> {});

        http.authorizeHttpRequests(auth -> auth

                // ALLOW STATIC RESOURCES
                .requestMatchers(
                        "/assets/**",
                        "/admin/login.html",
                        "/admin/register.html",
                        "/admin/dashboard.html",
                        "/index.html",
                        "/favicon.ico",
                        "/**/*.css",
                        "/**/*.js",
                        "/**/*.png",
                        "/**/*.jpg",
                        "/**/*.jpeg",
                        "/**/*.svg",
                        "/**/*.html"
                ).permitAll()

                // PUBLIC API ENDPOINTS
                .requestMatchers(
                        "/api/admin/login",
                        "/api/admin/register"
                ).permitAll()

                // EVERYTHING ELSE NEEDS JWT TOKEN
                .anyRequest().authenticated()
        );

        // Add JWT filter
        http.addFilterBefore(tokenFilter, UsernamePasswordAuthenticationFilter.class);

        // Allow H2 console, if needed
        http.headers(headers -> headers.frameOptions(frame -> frame.disable()));

        return http.build();
    }
}
