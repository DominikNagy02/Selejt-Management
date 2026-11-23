package hu.unideb.inf.core.dto;

import hu.unideb.inf.core.enums.TaskType;
import hu.unideb.inf.shared.schemas.ValidationConstants;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record TaskRequest(
        @NotNull
        TaskType taskType,

        @Min(ValidationConstants.MIN_PRIORITY)
        @Max(ValidationConstants.MAX_PRIORITY)
        Integer priority,

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

        Long deliveryOrderId,

        Long assignedToId,

        @Size(max = ValidationConstants.NOTE_MAX_LENGTH)
        String note
) {
}