package hu.unideb.inf.core.dto;

import hu.unideb.inf.shared.schemas.ValidationConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ScrapRequest(
        @NotNull
        Long inventoryId,

        @Positive
        int quantity,

        @NotBlank
        @Size(max = ValidationConstants.REASON_MAX_LENGTH)
        String reason
) {
}