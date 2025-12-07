package com.myfinbank.admin.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.stereotype.Component;

@Component
public class FeignAuthInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        // Forward token from current request
        String token = TokenContextHolder.getToken();
        if (token != null) {
            template.header("Authorization", "Bearer " + token);
        }
    }
}
