package hu.unideb.inf.core.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DeliveryOrderItemRequest(
        @NotNull
        Long productId,

        @Positive
        int requestedQuantity
) {
}