package hu.unideb.inf.core.dto;

import hu.unideb.inf.core.enums.InventoryStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record InboundResponse(
        Long inventoryItemId,
        Long productId,
        String productSku,
        String productName,
        Long shelfId,
        String shelfCode,
        Integer tierLevel,
        String batchNumber,
        LocalDate expirationDate,
        int quantity,
        BigDecimal currentShelfWeightKg,
        BigDecimal currentShelfVolumeM3,
        InventoryStatus status,
        Instant timestamp
) {
}
