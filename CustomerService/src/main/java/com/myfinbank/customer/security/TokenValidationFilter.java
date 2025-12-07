package com.myfinbank.customer.security;

import com.myfinbank.customer.dto.TokenValidationResponse;
import com.myfinbank.customer.feign.AuthClient;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TokenValidationFilter extends OncePerRequestFilter {

    private final AuthClient authClient;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {

            String token = header.substring(7);

            try {
                TokenValidationResponse resp = authClient.validateToken(token);

                if (resp != null && resp.isValid()) {

                    var authorities =
                            resp.getRoles() == null
                                    ? java.util.List.<SimpleGrantedAuthority>of()
                                    : resp.getRoles().stream()
                                        .map(SimpleGrantedAuthority::new)
                                        .collect(Collectors.toList());

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    resp.getSubject(),
                                    null,
                                    authorities
                            );

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }

            } catch (Exception ignored) {}
        }

        filterChain.doFilter(request, response);
    }
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {

        String path = request.getRequestURI();

        return path.startsWith("/assets/")
                || path.startsWith("/customer/")
                || path.startsWith("/admin/")
                || path.equals("/favicon.ico")
                || path.endsWith(".html")
                || path.endsWith(".css")
                || path.endsWith(".js")
                || path.startsWith("/h2-console/")
                || path.startsWith("/api/customers/login")
                || path.startsWith("/api/customers/register")
                || path.startsWith("/api/admin/login")
                || path.startsWith("/api/admin/register");
    }

}
