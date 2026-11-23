package hu.unideb.inf.core.dto;

import hu.unideb.inf.core.enums.ReservationType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;

public record ReservationRequest(
        @NotNull
        Long inventoryId,

        @NotNull
        ReservationType reservationType,

        Long deliveryOrderId,

        Long taskId,

        @Positive
        int quantity,

        Instant expiresAt
) {
}