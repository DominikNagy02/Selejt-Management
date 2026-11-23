package hu.unideb.inf.core.exception;

import java.util.Objects;

public class ReservationConflictException extends WmsBusinessException {

    public static final String ERROR_CODE = "reservation-conflict";
    public static final String TITLE = "Reservation Conflict";

    private final Long inventoryItemId;
    private final int requestedQuantity;
    private final int availableQuantity;

    public ReservationConflictException(Long inventoryItemId, int requestedQuantity, int availableQuantity) {
        super(
                ERROR_CODE,
                TITLE,
                409,
                buildMessage(inventoryItemId, requestedQuantity, availableQuantity)
        );
        this.inventoryItemId = inventoryItemId;
        this.requestedQuantity = requestedQuantity;
        this.availableQuantity = availableQuantity;
    }

    private static String buildMessage(Long inventoryItemId, int requestedQuantity, int availableQuantity) {
        Objects.requireNonNull(inventoryItemId, "inventoryItemId must not be null");
        return String.format(
                "A készlettétel (#%d) nem foglalható le: kért mennyiség %d, még elérhető %d.",
                inventoryItemId, requestedQuantity, availableQuantity
        );
    }

    public Long getInventoryItemId() {
        return inventoryItemId;
    }

    public int getRequestedQuantity() {
        return requestedQuantity;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }
}

