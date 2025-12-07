package com.myfinbank.notification.repository;

import com.myfinbank.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findBySeenFalseOrderByTimestampDesc();
    List<Notification> findAllByOrderByTimestampDesc();

}
