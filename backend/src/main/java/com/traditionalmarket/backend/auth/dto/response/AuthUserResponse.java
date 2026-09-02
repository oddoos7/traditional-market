package com.traditionalmarket.backend.auth.dto.response;

import java.time.LocalDateTime;

import com.traditionalmarket.backend.user.domain.UserAccount;
import com.traditionalmarket.backend.user.domain.UserRole;

public record AuthUserResponse(
        Long id,
        String email,
        String name,
        UserRole role,
        LocalDateTime createdAt
) {

    public static AuthUserResponse from(UserAccount user) {
        return new AuthUserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}
