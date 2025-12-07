package com.myfinbank.notification.service;

import com.myfinbank.notification.dto.NotificationRequest;
import com.myfinbank.notification.entity.Notification;
import com.myfinbank.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repo;

    public Notification create(NotificationRequest req) {
        Notification n = Notification.builder()
                .customerId(req.getCustomerId())
                .customerName(req.getCustomerName())
                .accountNumber(req.getAccountNumber())
                .message(req.getMessage())
                .timestamp(LocalDateTime.now())
                .seen(false)
                .build();
        return repo.save(n);
    }

    public List<Notification> getUnseen() {
        return repo.findBySeenFalseOrderByTimestampDesc();
    }
    
    public List<Notification> getAll() {
        return repo.findAllByOrderByTimestampDesc();
    }

    public void markAsSeen(Long id) {
        Notification n = repo.findById(id).orElseThrow();
        n.setSeen(true);
        repo.save(n);
    }
}
