package com.finance.notification.controller;

import com.finance.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Notification Controller - Essential endpoints only
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notifications", description = "APIs for managing notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Get user notifications")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Notifications retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping
    public ResponseEntity<Map<String, Object>> getNotifications(
            @Parameter(description = "User ID")
            @RequestParam UUID userId,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Filter by read status")
            @RequestParam(required = false) Boolean isRead,
            @Parameter(description = "Filter by notification type")
            @RequestParam(required = false) String type) {
        
        Pageable pageable = PageRequest.of(page, size);
        Map<String, Object> response = notificationService.getNotifications(userId, page, size, isRead, type, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Mark notification as read")
    @SecurityRequirement(name = "Bearer Authentication")
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(
            @Parameter(description = "Notification ID")
            @PathVariable UUID notificationId) {
        
        Map<String, Object> response = notificationService.markAsRead(notificationId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Mark all notifications as read")
    @SecurityRequirement(name = "Bearer Authentication")
    @PutMapping("/read-all")
    public ResponseEntity<Map<String, Object>> markAllAsRead(
            @Parameter(description = "User ID")
            @RequestParam UUID userId) {
        
        Map<String, Object> response = notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete notification")
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Map<String, Object>> deleteNotification(
            @Parameter(description = "Notification ID")
            @PathVariable UUID notificationId) {
        
        Map<String, Object> response = notificationService.deleteNotification(notificationId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get notification statistics")
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getNotificationStatistics(
            @Parameter(description = "User ID")
            @RequestParam UUID userId,
            @Parameter(description = "Period for statistics")
            @RequestParam(defaultValue = "LAST_30_DAYS") String period) {
        
        Map<String, Object> stats = notificationService.getStatistics(userId, period);
        return ResponseEntity.ok(stats);
    }
}