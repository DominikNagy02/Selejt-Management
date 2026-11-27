package hu.unideb.inf.backend.service.suggestion;

import hu.unideb.inf.core.enums.ZoneType;

import java.math.BigDecimal;
import java.util.Objects;

public record ShelfSuggestion(
        Long shelfId,
        String shelfCode,
        ZoneType zoneType,
        BigDecimal freeWeightKg,
        BigDecimal freeVolumeM3,
        BigDecimal remainingWeightAfterPlacementKg,
        BigDecimal remainingVolumeAfterPlacementM3,
        BigDecimal remainingCapacityRatio,
        boolean sameProductPresent
) {
    public ShelfSuggestion {
        Objects.requireNonNull(shelfId, "shelfId must not be null");
        Objects.requireNonNull(shelfCode, "shelfCode must not be null");
        Objects.requireNonNull(zoneType, "zoneType must not be null");
        Objects.requireNonNull(freeWeightKg, "freeWeightKg must not be null");
        Objects.requireNonNull(freeVolumeM3, "freeVolumeM3 must not be null");
        Objects.requireNonNull(remainingWeightAfterPlacementKg, "remainingWeightAfterPlacementKg must not be null");
        Objects.requireNonNull(remainingVolumeAfterPlacementM3, "remainingVolumeAfterPlacementM3 must not be null");
        Objects.requireNonNull(remainingCapacityRatio, "remainingCapacityRatio must not be null");
        if (shelfCode.isBlank()) {
            throw new IllegalArgumentException("shelfCode must not be blank");
        }
    }
}

