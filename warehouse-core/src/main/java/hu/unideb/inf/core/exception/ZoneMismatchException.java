package hu.unideb.inf.core.exception;

import hu.unideb.inf.core.enums.ZoneType;

import java.util.Objects;

public class ZoneMismatchException extends WmsBusinessException {

    public static final String ERROR_CODE = "zone-mismatch";
    public static final String TITLE = "Zone Type Mismatch";

    private final String productSku;
    private final ZoneType requiredZoneType;
    private final String shelfCode;
    private final ZoneType actualZoneType;

    public ZoneMismatchException(
            String productSku,
            ZoneType requiredZoneType,
            String shelfCode,
            ZoneType actualZoneType
    ) {
        super(
                ERROR_CODE,
                TITLE,
                409,
                buildMessage(productSku, requiredZoneType, shelfCode, actualZoneType)
        );
        this.productSku = productSku;
        this.requiredZoneType = requiredZoneType;
        this.shelfCode = shelfCode;
        this.actualZoneType = actualZoneType;
    }

    private static String buildMessage(
            String productSku,
            ZoneType requiredZoneType,
            String shelfCode,
            ZoneType actualZoneType
    ) {
        Objects.requireNonNull(productSku, "productSku must not be null");
        Objects.requireNonNull(requiredZoneType, "requiredZoneType must not be null");
        Objects.requireNonNull(shelfCode, "shelfCode must not be null");
        Objects.requireNonNull(actualZoneType, "actualZoneType must not be null");
        return String.format(
                "A termék (%s) %s zónát igényel, ezért nem helyezhető a(z) %s (%s) polcra.",
                productSku, requiredZoneType, shelfCode, actualZoneType
        );
    }

    public String getProductSku() {
        return productSku;
    }

    public ZoneType getRequiredZoneType() {
        return requiredZoneType;
    }

    public String getShelfCode() {
        return shelfCode;
    }

    public ZoneType getActualZoneType() {
        return actualZoneType;
    }
}
