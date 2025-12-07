package com.myfinbank.chat.service;

import com.myfinbank.chat.dto.ChatMessageRequest;
import com.myfinbank.chat.dto.ConversationSummary;
import com.myfinbank.chat.entity.ChatMessage;
import com.myfinbank.chat.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository repo;

    public ChatMessage saveMessage(ChatMessageRequest req) {

        if (req.getConversationId() == null) {
            throw new IllegalArgumentException("conversationId must not be null. It must be ChatRoom.id");
        }

        ChatMessage m = ChatMessage.builder()
                .conversationId(req.getConversationId())   
                .fromId(req.getFromId())
                .toId(req.getToId())
                .content(req.getContent())
                .seen(false)
                .timestamp(LocalDateTime.now())
                .build();

        return repo.save(m);
    }

    public List<ChatMessage> getMessages(Long conversationId) {
        return repo.findByConversationIdOrderByTimestampAsc(conversationId);
    }
    
 // return list of conversation summaries for admin
    public List<ConversationSummary> getAllConversationsForAdmin(Long adminId) {
        List<Long> convIds = repo.findDistinctConversationIdsOrderByLatestMessageDesc();
        List<ConversationSummary> summaries = new ArrayList<>();
        for(Long convId : convIds) {
            ChatMessage latest = repo.findLatestForConversation(convId, PageRequest.of(0,1)).stream().findFirst().orElse(null);
            if (latest == null) continue;
            // Determine participant (customerId) — since admin is single, conversation is between adminId and customerId.
            Long customerId = (latest.getFromId().equals(adminId) ? latest.getToId() : latest.getFromId());
            Long unread = repo.countUnseenForConversation(convId, adminId);
            summaries.add(new ConversationSummary(convId, customerId, latest.getContent(), latest.getTimestamp(), unread));
        }
        return summaries;
    }
    
    @Transactional
    public void markAllAsSeenForRecipient(Long convId, Long recipientId) {
        List<ChatMessage> list = repo.findByConversationIdOrderByTimestampAsc(convId);
        for (ChatMessage m : list) {
            if (m.getToId().equals(recipientId) && !m.isSeen()) {
                m.setSeen(true);
            }
        }
        repo.saveAll(list);
    }


}
