package hu.unideb.inf.core.dto;

import hu.unideb.inf.core.enums.StorageType;

import java.math.BigDecimal;
import java.time.Instant;

public record ShelfResponse(
        Long id,
        Long zoneId,
        String zoneCode,
        String code,
        StorageType storageType,
        int maxTiers,
        BigDecimal maxWeightKg,
        BigDecimal maxVolumeM3,
        BigDecimal currentWeightKg,
        BigDecimal currentVolumeM3,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}