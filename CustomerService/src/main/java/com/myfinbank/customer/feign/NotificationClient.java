package com.myfinbank.customer.feign;

import com.myfinbank.customer.dto.NotificationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "notification-service")
public interface NotificationClient {

    @PostMapping("/notifications/zero-balance")
    void saveZeroBalanceNotification(@RequestBody NotificationRequest req);
}
