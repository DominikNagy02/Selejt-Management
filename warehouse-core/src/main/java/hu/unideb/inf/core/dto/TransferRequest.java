package hu.unideb.inf.core.dto;

import hu.unideb.inf.shared.schemas.ValidationConstants;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record TransferRequest(
        @NotNull
        Long inventoryId,

        @NotNull
        Long fromShelfId,

        @Min(ValidationConstants.DEFAULT_TIER_LEVEL)
        int fromTierLevel,

        @NotNull
        Long toShelfId,

        @Min(ValidationConstants.DEFAULT_TIER_LEVEL)
        int toTierLevel,

        @Positive
        int quantity,

        @Size(max = ValidationConstants.REASON_MAX_LENGTH)
        String reason
) {
}