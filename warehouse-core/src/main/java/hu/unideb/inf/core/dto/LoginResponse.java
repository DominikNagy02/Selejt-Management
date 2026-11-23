package hu.unideb.inf.core.dto;

import hu.unideb.inf.core.enums.Role;

import java.time.Instant;

public record LoginResponse(
        String token,
        String tokenType,
        Instant expiresAt,
        Long userId,
        String username,
        String fullName,
        Role role
) {
}