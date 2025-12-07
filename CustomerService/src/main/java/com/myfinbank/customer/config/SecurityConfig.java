package com.myfinbank.customer.config;

import com.myfinbank.customer.security.TokenValidationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;


@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    private final TokenValidationFilter tokenFilter;

    @Bean
    public SecurityFilterChain filter(HttpSecurity http) throws Exception {

        http.csrf(cs -> cs.disable());
        http.cors(cors -> {});

        http.authorizeHttpRequests(auth -> auth

                // 🔥 Allow ALL static frontend files
                .requestMatchers(
                        "/customer/**",
                        "/admin/**",
                        "/assets/**",
                        "/favicon.ico",
                        "/index.html",
                        "/**/*.html",
                        "/**/*.css",
                        "/**/*.js"
                ).permitAll()

                // 🔥 Public backend APIs
                .requestMatchers(
                        "/api/customers/register",
                        "/api/customers/login",
                        "/h2-console/**"
                ).permitAll()

                // 🔥 Admin endpoints
                .requestMatchers("/api/customers/*/deactivate").hasRole("ADMIN")
                .requestMatchers("/api/customers/*/activate").hasRole("ADMIN")

                // 🔥 All other customer APIs
                .requestMatchers("/api/customers/**").authenticated()

                .anyRequest().permitAll()
        );

        // Add JWT filter
        http.addFilterBefore(tokenFilter, UsernamePasswordAuthenticationFilter.class);

        // Allow H2 console
        http.headers(headers -> headers.frameOptions(frame -> frame.disable()));

        return http.build();
    }
    
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOrigin("http://localhost:8082");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setAllowCredentials(true);
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }

}
