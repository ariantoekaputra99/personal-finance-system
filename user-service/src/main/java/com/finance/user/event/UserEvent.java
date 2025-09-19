package com.finance.user.event;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * User Event for Kafka messaging
 * Demonstrates event-driven architecture
 */
@Data
@Builder
public class UserEvent {
    private UUID userId;
    private String eventType;
    private String eventSource;
    private LocalDateTime timestamp;
    private Map<String, Object> eventData;
    
    public enum EventType {
        USER_REGISTERED,
        USER_UPDATED,
        USER_ACTIVATED,
        USER_DEACTIVATED,
        USER_LOGIN,
        USER_LOGOUT,
        PASSWORD_CHANGED,
        PROFILE_UPDATED
    }
}