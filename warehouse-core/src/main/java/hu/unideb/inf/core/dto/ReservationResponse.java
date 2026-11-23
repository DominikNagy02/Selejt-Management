package hu.unideb.inf.core.dto;

import hu.unideb.inf.core.enums.ReservationStatus;
import hu.unideb.inf.core.enums.ReservationType;

import java.time.Instant;

public record ReservationResponse(
        Long id,
        Long inventoryId,
        Long productId,
        String productSku,
        Long shelfId,
        String shelfCode,
        ReservationType reservationType,
        Long deliveryOrderId,
        Long taskId,
        int quantity,
        ReservationStatus status,
        Instant expiresAt,
        Instant createdAt,
        Instant updatedAt
) {
}