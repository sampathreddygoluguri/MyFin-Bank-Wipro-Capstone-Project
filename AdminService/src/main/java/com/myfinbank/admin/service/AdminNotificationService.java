package com.myfinbank.admin.service;

import com.myfinbank.admin.dto.NotificationDto;
import com.myfinbank.admin.feign.NotificationClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminNotificationService {

    private final NotificationClient notificationClient;

    public List<NotificationDto> getUnseen() {
        return notificationClient.getUnseen();
    }

    public List<NotificationDto> getAll() {
        return notificationClient.getAll();
    }

    public void markAsSeen(Long id) {
        notificationClient.markSeen(id);
    }
}
