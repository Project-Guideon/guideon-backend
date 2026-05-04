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

    @Query("SELECT COUNT(m) AS total, " +
           "SUM(CASE WHEN m.answerFound = true THEN 1 ELSE 0 END) AS found " +
           "FROM ChatMessage m WHERE m.siteId = :siteId")
    AnswerRateProjection countAnswerRateForSite(@Param("siteId") Long siteId);

    @Query(value = "SELECT CAST(EXTRACT(HOUR FROM created_at) AS INTEGER) AS \"hour\", COUNT(*) AS \"count\" " +
                   "FROM tb_chat_message " +
                   "WHERE site_id = :siteId AND CAST(created_at AS DATE) = CURRENT_DATE " +
                   "GROUP BY \"hour\" ORDER BY \"hour\"", nativeQuery = true)
    List<HourCountProjection> countByHourForSite(@Param("siteId") Long siteId);

    @Query(value = "SELECT m.site_id AS \"siteId\", s.name AS \"siteName\", COUNT(*) AS \"count\" " +
                   "FROM tb_chat_message m " +
                   "JOIN tb_site s ON s.site_id = m.site_id " +
                   "GROUP BY m.site_id, s.name " +
                   "ORDER BY \"count\" DESC LIMIT 5", nativeQuery = true)
    List<SiteTrafficProjection> countTop5BySite();

    interface CategoryCountProjection {
        String getCategory();
        Long getCount();
    }

    interface AnswerRateProjection {
        Long getTotal();
        Long getFound();
    }

    interface HourCountProjection {
        Integer getHour();
        Long getCount();
    }

    interface SiteTrafficProjection {
        Long getSiteId();
        String getSiteName();
        Long getCount();
    }
}
