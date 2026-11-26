package hu.unideb.inf.core.dto;

import hu.unideb.inf.core.enums.ZoneType;

import java.time.Instant;

public record ZoneResponse(
        Long id,
        Long warehouseId,
        String code,
        String name,
        ZoneType zoneType,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}