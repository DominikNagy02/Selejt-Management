package hu.unideb.inf.core.dto;

import hu.unideb.inf.core.enums.DeliveryOrderStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record DeliveryOrderResponse(
        Long id,
        String doNumber,
        String customerName,
        DeliveryOrderStatus status,
        LocalDate dueDate,
        Long createdById,
        String createdByUsername,
        List<DeliveryOrderItemResponse> items,
        Instant createdAt,
        Instant updatedAt
) {
}
