package com.myfinbank.chat.controller;

import com.myfinbank.chat.dto.StartChatRequest;
import com.myfinbank.chat.entity.ChatRoom;
import com.myfinbank.chat.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomRepository chatRoomRepository;

    // Start a chat (admin or customer can call)
    @PostMapping("/start")
    public ResponseEntity<ChatRoom> startChat(@RequestBody StartChatRequest req) {

        Long adminId = req.getAdminId();
        Long customerId = req.getCustomerId();

        if (adminId == null || customerId == null) {
            return ResponseEntity.badRequest().build();
        }

        // 1. Check existing chat room
        Optional<ChatRoom> existing = chatRoomRepository.findByAdminIdAndCustomerId(adminId, customerId);
        if (existing.isPresent()) {
            return ResponseEntity.ok(existing.get());
        }

        // 2. Create a new chat room
        ChatRoom room = new ChatRoom();
        room.setAdminId(adminId);
        room.setCustomerId(customerId);
        room.setCreatedAt(LocalDateTime.now());

        ChatRoom saved = chatRoomRepository.save(room);
        return ResponseEntity.ok(saved);
    }


    // Get room for customer (returns 404 if not present)
    @GetMapping("/room/customer/{customerId}")
    public ResponseEntity<ChatRoom> getRoomForCustomer(@PathVariable Long customerId) {
        return chatRoomRepository.findByCustomerId(customerId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

}

