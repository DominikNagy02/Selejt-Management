package hu.unideb.inf.core.dto;

public record InventorySummaryItemDto(
        Long productId,
        String productSku,
        String productName,
        int totalQuantity,
        int reservedQuantity,
        int availableQuantity,
        int minStockLevel,
        boolean belowMinimum
) {
}