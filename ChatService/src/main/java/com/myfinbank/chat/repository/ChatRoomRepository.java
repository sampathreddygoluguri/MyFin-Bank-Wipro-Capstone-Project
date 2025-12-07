package com.myfinbank.chat.repository;

import com.myfinbank.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findByAdminIdAndCustomerId(Long adminId, Long customerId);
    Optional<ChatRoom> findByCustomerId(Long customerId);
}
