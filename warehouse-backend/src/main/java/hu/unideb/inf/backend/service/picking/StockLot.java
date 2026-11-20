package hu.unideb.inf.backend.service.picking;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public record StockLot(
        Long inventoryItemId,
        String shelfCode,
        int tierLevel,
        String batchNumber,
        int availableQuantity,
        Instant receivedAt,
        LocalDate expirationDate
) {
    public StockLot {
        Objects.requireNonNull(inventoryItemId, "inventoryItemId must not be null");
        Objects.requireNonNull(shelfCode, "shelfCode must not be null");
        Objects.requireNonNull(batchNumber, "batchNumber must not be null");
        Objects.requireNonNull(receivedAt, "receivedAt must not be null");
        if (tierLevel < 0) {
            throw new IllegalArgumentException("tierLevel must not be negative");
        }
        if (availableQuantity < 0) {
            throw new IllegalArgumentException("availableQuantity must not be negative");
        }
    }

    public StockLot(
            Long inventoryItemId,
            String shelfCode,
            String batchNumber,
            int availableQuantity,
            Instant receivedAt,
            LocalDate expirationDate
    ) {
        this(inventoryItemId, shelfCode, 0, batchNumber, availableQuantity, receivedAt, expirationDate);
    }
}

