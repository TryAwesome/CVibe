package com.cvibe.biz.analytics.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * AnalyticsEvent Entity
 * 
 * Stores aggregated analytics events for platform-wide metrics.
 */
@Entity
@Table(name = "analytics_events", indexes = {
        @Index(name = "idx_analytics_events_type", columnList = "event_type"),
        @Index(name = "idx_analytics_events_name", columnList = "event_name"),
        @Index(name = "idx_analytics_events_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class AnalyticsEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Event type
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private EventType eventType;

    /**
     * Event name (specific event identifier)
     */
    @Column(name = "event_name", nullable = false)
    private String eventName;

    /**
     * Event value (numeric, e.g., count, duration)
     */
    @Column(name = "event_value")
    private Double eventValue;

    /**
     * Event properties (JSON)
     */
    @Column(name = "properties", columnDefinition = "TEXT")
    private String properties;

    /**
     * User ID if applicable
     */
    @Column(name = "user_id")
    private UUID userId;

    /**
     * Session ID
     */
    @Column(name = "session_id")
    private String sessionId;

    /**
     * Source of the event
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "source")
    @Builder.Default
    private EventSource source = EventSource.WEB;

    /**
     * Environment
     */
    @Column(name = "environment")
    @Builder.Default
    private String environment = "production";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // ================== Enums ==================

    public enum EventType {
        USER,           // User-related events
        SYSTEM,         // System events
        BUSINESS,       // Business metrics
        PERFORMANCE,    // Performance metrics
        ERROR,          // Error events
        CONVERSION      // Conversion funnel events
    }

    public enum EventSource {
        WEB,
        MOBILE_IOS,
        MOBILE_ANDROID,
        API,
        SYSTEM,
        SCHEDULER
    }

    // ================== Static Factory ==================

    public static AnalyticsEvent business(String eventName, Double value) {
        return AnalyticsEvent.builder()
                .eventType(EventType.BUSINESS)
                .eventName(eventName)
                .eventValue(value)
                .build();
    }

    public static AnalyticsEvent user(String eventName, UUID userId) {
        return AnalyticsEvent.builder()
                .eventType(EventType.USER)
                .eventName(eventName)
                .userId(userId)
                .build();
    }

    public static AnalyticsEvent system(String eventName, String properties) {
        return AnalyticsEvent.builder()
                .eventType(EventType.SYSTEM)
                .eventName(eventName)
                .properties(properties)
                .source(EventSource.SYSTEM)
                .build();
    }

    public static AnalyticsEvent conversion(String eventName, UUID userId, Double value) {
        return AnalyticsEvent.builder()
                .eventType(EventType.CONVERSION)
                .eventName(eventName)
                .userId(userId)
                .eventValue(value)
                .build();
    }
}
