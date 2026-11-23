package hu.unideb.inf.core.dto;

import hu.unideb.inf.core.enums.MovementType;

import java.time.Instant;

public record StockMovementResponse(
        Long id,
        MovementType movementType,
        Long productId,
        String productSku,
        Long inventoryId,
        Long fromShelfId,
        String fromShelfCode,
        Integer fromTierLevel,
        Long toShelfId,
        String toShelfCode,
        Integer toTierLevel,
        int quantity,
        String reason,
        Long taskId,
        Long performedById,
        String performedByUsername,
        Instant createdAt
) {
}