package com.finance.notification.repository;

import com.finance.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    
    Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    
    Page<Notification> findByUserIdAndIsReadOrderByCreatedAtDesc(UUID userId, Boolean isRead, Pageable pageable);
    
    Page<Notification> findByUserIdAndTypeOrderByCreatedAtDesc(UUID userId, String type, Pageable pageable);
    
    Page<Notification> findByUserIdAndIsReadAndTypeOrderByCreatedAtDesc(UUID userId, Boolean isRead, String type, Pageable pageable);
    
    long countByUserIdAndIsRead(UUID userId, Boolean isRead);
    
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt, n.updatedAt = :updatedAt WHERE n.userId = :userId AND n.isRead = false")
    int markAllAsReadByUserId(@Param("userId") UUID userId, @Param("readAt") LocalDateTime readAt, @Param("updatedAt") LocalDateTime updatedAt);
    
    @Query("SELECT n.type, COUNT(n) FROM Notification n WHERE n.userId = :userId AND n.createdAt >= :startDate GROUP BY n.type")
    List<Object[]> getNotificationStatsByType(@Param("userId") UUID userId, @Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.userId = :userId AND n.createdAt >= :startDate")
    long getTotalNotificationCount(@Param("userId") UUID userId, @Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.userId = :userId AND n.isRead = true AND n.createdAt >= :startDate")
    long getReadNotificationCount(@Param("userId") UUID userId, @Param("startDate") LocalDateTime startDate);
}