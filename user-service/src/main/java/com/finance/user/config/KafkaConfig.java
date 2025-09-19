package com.finance.user.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka Configuration for Event-Driven Architecture
 * Demonstrates Spring IoC for Kafka topic management
 */
@Configuration
public class KafkaConfig {

    public static final String USER_EVENTS_TOPIC = "user-events";
    public static final String USER_NOTIFICATIONS_TOPIC = "user-notifications";
    public static final String USER_ANALYTICS_TOPIC = "user-analytics";

    /**
     * User events topic for user lifecycle events
     */
    @Bean
    public NewTopic userEventsTopic() {
        return TopicBuilder.name(USER_EVENTS_TOPIC)
            .partitions(3)
            .replicas(1)
            .build();
    }

    /**
     * User notifications topic for real-time notifications
     */
    @Bean
    public NewTopic userNotificationsTopic() {
        return TopicBuilder.name(USER_NOTIFICATIONS_TOPIC)
            .partitions(3)
            .replicas(1)
            .build();
    }

    /**
     * User analytics topic for analytics processing
     */
    @Bean
    public NewTopic userAnalyticsTopic() {
        return TopicBuilder.name(USER_ANALYTICS_TOPIC)
            .partitions(3)
            .replicas(1)
            .build();
    }
}