package hu.unideb.inf.core.dto;

import java.time.Instant;

public record WarehouseResponse(
        Long id,
        String code,
        String name,
        String address,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}