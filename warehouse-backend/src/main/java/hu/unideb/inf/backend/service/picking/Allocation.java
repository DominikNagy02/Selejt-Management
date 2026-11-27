package hu.unideb.inf.backend.service.picking;

import java.util.Objects;

public record Allocation(
        Long inventoryItemId,
        String shelfCode,
        int tierLevel,
        String batchNumber,
        int quantity
) {
    public Allocation {
        Objects.requireNonNull(inventoryItemId, "inventoryItemId must not be null");
        Objects.requireNonNull(shelfCode, "shelfCode must not be null");
        Objects.requireNonNull(batchNumber, "batchNumber must not be null");
        if (tierLevel < 0) {
            throw new IllegalArgumentException("tierLevel must not be negative");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be greater than 0");
        }
    }

    public Allocation(
            Long inventoryItemId,
            String shelfCode,
            String batchNumber,
            int quantity
    ) {
        this(inventoryItemId, shelfCode, 0, batchNumber, quantity);
    }
}

