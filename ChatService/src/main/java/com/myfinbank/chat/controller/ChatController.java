package com.myfinbank.chat.controller;

import com.myfinbank.chat.dto.ChatMessageDto;
import com.myfinbank.chat.dto.ChatMessageRequest;
import com.myfinbank.chat.dto.ConversationSummary;
import com.myfinbank.chat.entity.ChatMessage;
import com.myfinbank.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ModelMapper mapper = new ModelMapper();

    @MessageMapping("/chat.send")
    public void sendMessage(ChatMessageRequest req) {
        // persist
        ChatMessage saved = chatService.saveMessage(req);
        // map to dto
        ChatMessageDto dto = mapper.map(saved, ChatMessageDto.class);

        // send to subscribers of the conversation topic
        messagingTemplate.convertAndSend("/topic/chat." + saved.getConversationId(), dto);
    }

    // REST to get history (latest first if you want)
    @GetMapping("/{conversationId}/messages")
    public List<ChatMessageDto> getMessages(@PathVariable Long conversationId,
                                            @RequestParam(defaultValue = "100") int limit) {
        List<ChatMessage> messages = chatService.getMessages(conversationId);
        return messages.stream()
                .map(m -> mapper.map(m, ChatMessageDto.class))
                .collect(Collectors.toList());
    }
    
 // get conversation list (admin only)
    @GetMapping("/conversations")
    public List<ConversationSummary> getConversationsForAdmin() {
        Long adminId = 1L; 
        return chatService.getAllConversationsForAdmin(adminId);
    }

    // mark all messages in a conversation as seen by the given user (when admin opens)
    @PutMapping("/{conversationId}/mark-all-seen")
    public void markAllAsSeen(@PathVariable Long conversationId, @RequestParam("userId") Long userId) {
        chatService.markAllAsSeenForRecipient(conversationId, userId);
    }
   


}
