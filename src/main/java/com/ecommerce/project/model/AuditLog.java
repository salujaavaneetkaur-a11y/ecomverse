package com.ecommerce.project.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Audit Log Entity
 *
 * ============================================================
 * 🎓 WHY AUDIT LOGGING?
 * ============================================================
 *
 * 1. COMPLIANCE
 *    - Required for PCI-DSS, GDPR, SOC2
 *    - Track who did what and when
 *
 * 2. SECURITY
 *    - Detect suspicious activity
 *    - Investigate breaches
 *
 * 3. DEBUGGING
 *    - Trace issues in production
 *    - Understand user behavior
 *
 * 4. ANALYTICS
 *    - Track feature usage
 *    - Identify bottlenecks
 *
 * ============================================================
 * 📋 INTERVIEW TIP:
 * "I implemented audit logging using AOP to capture all important
 * actions. This provides a complete trail for compliance and
 * security without cluttering business logic."
 * ============================================================
 */
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_user", columnList = "userId"),
    @Index(name = "idx_audit_action", columnList = "action"),
    @Index(name = "idx_audit_timestamp", columnList = "timestamp")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Who performed the action
    private Long userId;
    private String username;

    // What action was performed
    @Column(nullable = false)
    private String action; // CREATE, UPDATE, DELETE, LOGIN, etc.

    // On what entity/resource
    private String entityType; // Product, Order, User, etc.
    private String entityId;

    // Details of the action
    @Column(length = 2000)
    private String details;

    // Old and new values (for updates)
    @Column(length = 2000)
    private String oldValue;

    @Column(length = 2000)
    private String newValue;

    // Request metadata
    private String ipAddress;
    private String userAgent;
    private String requestUri;
    private String httpMethod;

    // When
    @Column(nullable = false)
    private LocalDateTime timestamp;

    // Status
    private String status; // SUCCESS, FAILURE

    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
    }
}
