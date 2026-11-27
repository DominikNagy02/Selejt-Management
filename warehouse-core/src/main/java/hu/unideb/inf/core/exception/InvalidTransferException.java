package hu.unideb.inf.core.exception;

import java.util.Objects;

public class InvalidTransferException extends WmsBusinessException {

    public static final String ERROR_CODE = "invalid-transfer";
    public static final String TITLE = "Invalid Stock Transfer";

    public InvalidTransferException(String message) {
        this(400, message);
    }

    public InvalidTransferException(int httpStatus, String message) {
        super(ERROR_CODE, TITLE, httpStatus, message);
    }

    public static InvalidTransferException sameLocation(String shelfCode, int tierLevel) {
        Objects.requireNonNull(shelfCode, "shelfCode must not be null");
        return new InvalidTransferException(
                400,
                String.format(
                        "A forrás- és célhely nem egyezhet meg (%s, %d. szint).",
                        shelfCode, tierLevel
                )
        );
    }

    public static InvalidTransferException sourceLocationMismatch(
            Long inventoryId,
            String actualShelfCode,
            int actualTierLevel
    ) {
        Objects.requireNonNull(inventoryId, "inventoryId must not be null");
        Objects.requireNonNull(actualShelfCode, "actualShelfCode must not be null");
        return new InvalidTransferException(
                409,
                String.format(
                        "A(z) %d azonosítójú készlettétel nem a megadott forráshelyen található (aktuális hely: %s, %d. szint).",
                        inventoryId, actualShelfCode, actualTierLevel
                )
        );
    }
}
