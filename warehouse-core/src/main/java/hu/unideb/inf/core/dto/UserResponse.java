package hu.unideb.inf.core.dto;

import hu.unideb.inf.core.enums.Role;

import java.time.Instant;

public record UserResponse(
        Long id,
        String username,
        String fullName,
        Role role,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}