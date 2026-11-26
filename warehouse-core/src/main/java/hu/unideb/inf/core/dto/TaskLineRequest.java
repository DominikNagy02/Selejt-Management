package hu.unideb.inf.core.dto;

import hu.unideb.inf.shared.schemas.ValidationConstants;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

public record TaskLineRequest(
        Long inventoryId,
        Long lotId,
        Long fromShelfId,
        @Min(ValidationConstants.DEFAULT_TIER_LEVEL)
        Integer fromTierLevel,
        Long toShelfId,
        @Min(ValidationConstants.DEFAULT_TIER_LEVEL)
        Integer toTierLevel,
        @Positive
        int quantity
) {
}
