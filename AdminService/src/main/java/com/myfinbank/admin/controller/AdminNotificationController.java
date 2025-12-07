package com.myfinbank.admin.controller;

import com.myfinbank.admin.dto.NotificationDto;
import com.myfinbank.admin.service.AdminNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final AdminNotificationService service;

    @GetMapping("/unseen")
    public List<NotificationDto> getUnseenNotifications() {
        return service.getUnseen();
    }

    @GetMapping("/all")
    public List<NotificationDto> getAllNotifications() {
        return service.getAll();
    }

    @PutMapping("/{id}/mark-seen")
    public void markAsSeen(@PathVariable Long id) {
        service.markAsSeen(id);
    }
}
