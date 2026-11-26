package hu.unideb.inf.core.dto;

import hu.unideb.inf.shared.schemas.ValidationConstants;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record InboundRequest(
        @NotNull
        Long productId,

        @NotNull
        Long shelfId,

        @Min(ValidationConstants.DEFAULT_TIER_LEVEL)
        Integer tierLevel,

        @NotBlank
        @Size(max = ValidationConstants.LOT_NUMBER_MAX_LENGTH)
        String batchNumber,

        LocalDate expirationDate,

        @Positive
        int quantity
) {
}
