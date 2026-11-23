package hu.unideb.inf.core.dto;

import hu.unideb.inf.shared.schemas.ValidationConstants;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record TaskCompleteRequest(
        @Positive
        int completedQuantity,

        @Size(max = ValidationConstants.NOTE_MAX_LENGTH)
        String note
) {
}