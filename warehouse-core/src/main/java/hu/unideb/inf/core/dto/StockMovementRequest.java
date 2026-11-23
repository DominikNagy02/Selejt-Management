package hu.unideb.inf.core.dto;

import hu.unideb.inf.core.enums.MovementType;
import hu.unideb.inf.shared.schemas.ValidationConstants;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record StockMovementRequest(
        @NotNull
        MovementType movementType,

        @NotNull
        Long productId,

        Long inventoryId,

        Long fromShelfId,

        @Min(ValidationConstants.DEFAULT_TIER_LEVEL)
        Integer fromTierLevel,

        Long toShelfId,

        @Min(ValidationConstants.DEFAULT_TIER_LEVEL)
        Integer toTierLevel,

        @Positive
        int quantity,

        @Size(max = ValidationConstants.REASON_MAX_LENGTH)
        String reason,

        Long taskId
) {
}