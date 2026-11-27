package hu.unideb.inf.backend.service.reservation;

import hu.unideb.inf.backend.model.entity.DeliveryOrderLineEntity;
import hu.unideb.inf.backend.model.entity.TaskLineEntity;
import hu.unideb.inf.core.enums.ReservationType;

import java.util.Objects;

public record ReservationCommand(
        Long productId,
        int quantity,
        ReservationType reservationType,
        Long createdBy,
        Long warehouseId,
        TaskLineEntity taskLine,
        DeliveryOrderLineEntity deliveryOrderLine
) {
    public ReservationCommand {
        Objects.requireNonNull(productId, "productId must not be null");
        Objects.requireNonNull(reservationType, "reservationType must not be null");
        Objects.requireNonNull(createdBy, "createdBy must not be null");
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be greater than 0");
        }
    }

    public static ReservationCommand forPicking(
            Long productId,
            int quantity,
            Long createdBy,
            Long warehouseId,
            TaskLineEntity taskLine,
            DeliveryOrderLineEntity deliveryOrderLine
    ) {
        return new ReservationCommand(
                productId,
                quantity,
                ReservationType.PICKING,
                createdBy,
                warehouseId,
                taskLine,
                deliveryOrderLine
        );
    }

    public static ReservationCommand forTransfer(
            Long productId,
            int quantity,
            Long createdBy,
            Long warehouseId,
            TaskLineEntity taskLine
    ) {
        return new ReservationCommand(
                productId,
                quantity,
                ReservationType.TRANSFER,
                createdBy,
                warehouseId,
                taskLine,
                null
        );
    }
}
