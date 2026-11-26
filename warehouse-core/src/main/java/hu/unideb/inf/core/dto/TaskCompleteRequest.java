package hu.unideb.inf.core.dto;

import hu.unideb.inf.shared.schemas.ValidationConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record TaskCompleteRequest(
        @NotBlank
        @Size(max = ValidationConstants.SHELF_CODE_MAX_LENGTH)
        String scannedShelfCode,

        @Positive
        int completedQuantity,

        @Size(max = ValidationConstants.NOTE_MAX_LENGTH)
        String note
) {
}
