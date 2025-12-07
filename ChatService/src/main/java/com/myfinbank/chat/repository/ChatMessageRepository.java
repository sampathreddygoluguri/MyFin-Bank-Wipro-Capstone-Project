package com.myfinbank.chat.repository;

import com.myfinbank.chat.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByConversationIdOrderByTimestampAsc(Long conversationId);

    List<ChatMessage> findTop100ByConversationIdOrderByTimestampDesc(Long conversationId);

    // FIXED: simple distinct conversations
    @Query("SELECT DISTINCT c.conversationId FROM ChatMessage c ORDER BY c.conversationId DESC")
    List<Long> findDistinctConversationIdsOrderByLatestMessageDesc();

    // FIXED pageable import
    @Query("SELECT c FROM ChatMessage c WHERE c.conversationId = :convId ORDER BY c.timestamp DESC")
    List<ChatMessage> findLatestForConversation(
            @Param("convId") Long conversationId,
            Pageable pageable);

    @Query("SELECT COUNT(c) FROM ChatMessage c WHERE c.conversationId = :convId AND c.seen = false AND c.toId = :recipientId")
    Long countUnseenForConversation(
            @Param("convId") Long convId,
            @Param("recipientId") Long recipientId);
}
