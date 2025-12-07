package com.myfinbank.notification.controller;

import com.myfinbank.notification.dto.NotificationRequest;
import com.myfinbank.notification.entity.Notification;
import com.myfinbank.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService service;

    @PostMapping("/zero-balance")
    public Notification createZeroBalanceNotification(@RequestBody NotificationRequest req) {
        return service.create(req);
    }

    @GetMapping("/unseen")
    public List<Notification> getUnseenNotifications() {
        return service.getUnseen();
    }
    
    @GetMapping("/all")
    public List<Notification> getAllNotifications() {
        return service.getAll();
    }



    @PutMapping("/{id}/mark-seen")
    public void markAsSeen(@PathVariable Long id) {
        service.markAsSeen(id);
    }
}
