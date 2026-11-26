package hu.unideb.inf.core.dto;

import hu.unideb.inf.core.enums.ZoneType;

import java.math.BigDecimal;

public record ShelfSuggestionResponse(
        Long shelfId,
        String shelfCode,
        ZoneType zoneType,
        BigDecimal freeWeightKg,
        BigDecimal freeVolumeM3
) {
}