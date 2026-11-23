package hu.unideb.inf.core.dto;

import hu.unideb.inf.core.enums.ZoneType;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(
        Long id,
        String sku,
        String barcode,
        String name,
        String description,
        BigDecimal unitWeightKg,
        BigDecimal unitVolumeM3,
        boolean requiresExpiry,
        ZoneType requiredZoneType,
        int minStockLevel,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}