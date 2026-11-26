package hu.unideb.inf.core.dto;

public record DeliveryOrderItemResponse(
        Long id,
        Long productId,
        String productSku,
        String productName,
        Long requestedLotId,
        int requestedQuantity,
        int pickedQuantity
) {
}
