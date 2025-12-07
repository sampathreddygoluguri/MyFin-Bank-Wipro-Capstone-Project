package com.myfinbank.chat.service;

import com.myfinbank.chat.entity.ChatRoom;
import com.myfinbank.chat.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;

    public ChatRoom startChat(Long adminId, Long customerId) {

        return chatRoomRepository
                .findByAdminIdAndCustomerId(adminId, customerId)
                .orElseGet(() -> {

                    ChatRoom room = new ChatRoom();
                    room.setAdminId(adminId);
                    room.setCustomerId(customerId);
                    room.setCreatedAt(LocalDateTime.now());

                    return chatRoomRepository.save(room);
                });
    }

    public ChatRoom getRoom(Long roomId) {
        return chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Chat Room Not Found"));
    }
}
