package com.traditionalmarket.backend.user.domain;

import java.util.Locale;

import com.traditionalmarket.backend.common.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "users",
        indexes = @Index(name = "idx_users_email", columnList = "email"),
        uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserAccount extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    private UserAccount(String email, String passwordHash, String name, UserRole role) {
        this.email = normalizeEmail(email);
        this.passwordHash = requireText(passwordHash, "비밀번호 해시");
        this.name = requireText(name, "이름");
        this.role = role == null ? UserRole.USER : role;
    }

    public static UserAccount create(String email, String passwordHash, String name) {
        return new UserAccount(email, passwordHash, name, UserRole.USER);
    }

    public static UserAccount createAdmin(String email, String passwordHash, String name) {
        return new UserAccount(email, passwordHash, name, UserRole.ADMIN);
    }

    private static String normalizeEmail(String email) {
        return requireText(email, "이메일").toLowerCase(Locale.ROOT);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "은(는) 필수입니다.");
        }
        return value.trim();
    }
}
