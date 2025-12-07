package com.myfinbank.admin.feign;

import com.myfinbank.admin.dto.NotificationDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "notification-service")
public interface NotificationClient {

    @GetMapping("/notifications/unseen")
    List<NotificationDto> getUnseen();

    @GetMapping("/notifications/all")
    List<NotificationDto> getAll();

    @PutMapping("/notifications/{id}/mark-seen")
    void markSeen(@PathVariable("id") Long id);
}
