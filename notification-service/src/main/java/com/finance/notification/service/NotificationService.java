package com.finance.notification.service;

import com.finance.notification.entity.Notification;
import com.finance.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public Map<String, Object> getNotifications(UUID userId, int page, int size, Boolean isRead, String type, Pageable pageable) {
        Page<Notification> notifications;
        
        if (isRead != null && type != null) {
            notifications = notificationRepository.findByUserIdAndIsReadAndTypeOrderByCreatedAtDesc(userId, isRead, type, pageable);
        } else if (isRead != null) {
            notifications = notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(userId, isRead, pageable);
        } else if (type != null) {
            notifications = notificationRepository.findByUserIdAndTypeOrderByCreatedAtDesc(userId, type, pageable);
        } else {
            notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        }
        
        long unreadCount = notificationRepository.countByUserIdAndIsRead(userId, false);
        
        List<Map<String, Object>> notificationList = notifications.getContent().stream()
            .map(this::mapToResponse)
            .toList();
        
        Map<String, Object> response = new HashMap<>();
        response.put("notifications", notificationList);
        response.put("totalElements", notifications.getTotalElements());
        response.put("totalPages", notifications.getTotalPages());
        response.put("currentPage", page);
        response.put("pageSize", size);
        response.put("unreadCount", unreadCount);
        
        return response;
    }

    public Map<String, Object> markAsRead(UUID notificationId) {
        Optional<Notification> notificationOpt = notificationRepository.findById(notificationId);
        
        if (notificationOpt.isPresent()) {
            Notification notification = notificationOpt.get();
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", notificationId);
            response.put("isRead", true);
            response.put("readAt", notification.getReadAt());
            response.put("message", "Notification marked as read");
            return response;
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("id", notificationId);
        response.put("message", "Notification not found");
        return response;
    }

    public Map<String, Object> markAllAsRead(UUID userId) {
        LocalDateTime now = LocalDateTime.now();
        int markedCount = notificationRepository.markAllAsReadByUserId(userId, now, now);
        
        Map<String, Object> response = new HashMap<>();
        response.put("markedCount", markedCount);
        response.put("message", "All notifications marked as read");
        response.put("timestamp", now);
        
        return response;
    }

    public Map<String, Object> deleteNotification(UUID notificationId) {
        boolean exists = notificationRepository.existsById(notificationId);
        
        if (exists) {
            notificationRepository.deleteById(notificationId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", notificationId);
            response.put("message", "Notification deleted successfully");
            response.put("timestamp", LocalDateTime.now());
            return response;
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("id", notificationId);
        response.put("message", "Notification not found");
        response.put("timestamp", LocalDateTime.now());
        return response;
    }

    public Map<String, Object> getStatistics(UUID userId, String period) {
        LocalDateTime startDate = getStartDateForPeriod(period);
        
        List<Object[]> typeStats = notificationRepository.getNotificationStatsByType(userId, startDate);
        long totalCount = notificationRepository.getTotalNotificationCount(userId, startDate);
        long readCount = notificationRepository.getReadNotificationCount(userId, startDate);
        long unreadCount = totalCount - readCount;
        
        Map<String, Object> counts = new HashMap<>();
        counts.put("total", totalCount);
        counts.put("read", readCount);
        counts.put("unread", unreadCount);
        counts.put("deleted", 0);
        
        Map<String, Object> byType = new HashMap<>();
        typeStats.forEach(stat -> byType.put((String) stat[0], stat[1]));
        
        double readRate = totalCount > 0 ? (double) readCount / totalCount * 100 : 0.0;
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("period", period);
        stats.put("userId", userId);
        stats.put("counts", counts);
        stats.put("byType", byType);
        stats.put("readRate", Math.round(readRate * 100.0) / 100.0);
        
        return stats;
    }

    private LocalDateTime getStartDateForPeriod(String period) {
        return switch (period) {
            case "LAST_7_DAYS" -> LocalDateTime.now().minusDays(7);
            case "LAST_3_MONTHS" -> LocalDateTime.now().minusMonths(3);
            default -> LocalDateTime.now().minusDays(30);
        };
    }

    private Map<String, Object> mapToResponse(Notification notification) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", notification.getId());
        response.put("title", notification.getTitle());
        response.put("message", notification.getMessage());
        response.put("type", notification.getType());
        response.put("priority", notification.getPriority());
        response.put("isRead", notification.getIsRead());
        response.put("createdAt", notification.getCreatedAt());
        response.put("category", notification.getCategory());
        return response;
    }
}