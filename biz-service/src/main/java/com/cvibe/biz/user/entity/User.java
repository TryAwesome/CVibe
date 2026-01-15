package com.cvibe.biz.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * User Entity - Core user information
 */
@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "google_sub", unique = true)
    private String googleSub;

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(length = 20)
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private UserRole role = UserRole.ROLE_USER;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    /**
     * User Role Enum
     */
    public enum UserRole {
        ROLE_USER,
        ROLE_ADMIN
    }

    /**
     * Check if user is admin
     */
    public boolean isAdmin() {
        return UserRole.ROLE_ADMIN.equals(this.role);
    }

    /**
     * Check if user logged in via Google OAuth
     */
    public boolean isGoogleUser() {
        return this.googleSub != null && !this.googleSub.isEmpty();
    }

    /**
     * Check if user has password (email/password login)
     */
    public boolean hasPassword() {
        return this.passwordHash != null && !this.passwordHash.isEmpty();
    }
}
