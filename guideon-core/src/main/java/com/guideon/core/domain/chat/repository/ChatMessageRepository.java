package com.guideon.core.domain.chat.repository;

import com.guideon.core.domain.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    @Query("SELECT COALESCE(m.category, 'GENERAL') AS category, COUNT(m) AS count " +
           "FROM ChatMessage m " +
           "WHERE m.siteId = :siteId " +
           "GROUP BY COALESCE(m.category, 'GENERAL')")
    List<CategoryCountProjection> countByCategoryForSite(@Param("siteId") Long siteId);

    interface CategoryCountProjection {
        String getCategory();
        Long getCount();
    }
}
