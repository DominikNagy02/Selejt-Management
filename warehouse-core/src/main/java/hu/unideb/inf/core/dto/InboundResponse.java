package hu.unideb.inf.core.dto;

import hu.unideb.inf.core.enums.InventoryStatus;

import java.time.Instant;
import java.time.LocalDate;

public record InboundResponse(
        Long id,
        Long productId,
        String productSku,
        String productName,
        Long shelfId,
        String shelfCode,
        int tierLevel,
        String lotNumber,
        LocalDate expiryDate,
        int quantity,
        InventoryStatus status,
        Instant receivedAt
) {
}