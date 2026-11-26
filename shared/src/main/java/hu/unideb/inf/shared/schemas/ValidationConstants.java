package hu.unideb.inf.shared.schemas;

import java.math.BigDecimal;

/**
 * Közös validációs konstansok és adatbázis-mezőkorlátok a kliensek és a backend között.
 */
public final class ValidationConstants {

    private ValidationConstants() {
    }

    // --- Kód és azonosító hosszkorlátok ---
    public static final int WAREHOUSE_CODE_MAX_LENGTH = 20;
    public static final int ZONE_CODE_MAX_LENGTH = 20;
    public static final int SHELF_CODE_MAX_LENGTH = 30;
    public static final int SKU_MAX_LENGTH = 40;
    public static final int BARCODE_MAX_LENGTH = 64;
    public static final int LOT_NUMBER_MAX_LENGTH = 50;
    public static final int DO_NUMBER_MAX_LENGTH = 30;

    // --- Megnevezés, leírás és felhasználói mezők hosszkorlátai ---
    public static final int NAME_MAX_LENGTH = 100;
    public static final int PRODUCT_NAME_MAX_LENGTH = 150;
    public static final int CUSTOMER_NAME_MAX_LENGTH = 150;
    public static final int ADDRESS_MAX_LENGTH = 255;
    public static final int DESCRIPTION_MAX_LENGTH = 500;
    public static final int NOTE_MAX_LENGTH = 500;
    public static final int REASON_MAX_LENGTH = 500;
    public static final int USERNAME_MAX_LENGTH = 50;
    public static final int FULL_NAME_MAX_LENGTH = 100;
    public static final int PASSWORD_MIN_LENGTH = 8;
    public static final int PASSWORD_MAX_LENGTH = 100;

    // --- Fizikai méretek: DECIMAL(12,3) súly, DECIMAL(12,6) térfogat ---
    public static final int WEIGHT_PRECISION = 12;
    public static final int WEIGHT_SCALE = 3;
    public static final String MIN_POSITIVE_WEIGHT = "0.001";
    public static final String MAX_WEIGHT_KG = "999999999.999";

    public static final int VOLUME_PRECISION = 12;
    public static final int VOLUME_SCALE = 6;
    public static final String MIN_POSITIVE_VOLUME = "0.000001";
    public static final String MAX_VOLUME_M3 = "999999.999999";

    // --- Mennyiségi és hierarchia korlátok ---
    public static final int MIN_TIERS = 1;
    public static final int DEFAULT_TIER_LEVEL = 0;
    public static final int MIN_PRIORITY = 1;
    public static final int MAX_PRIORITY = 5;
    public static final int DEFAULT_PRIORITY = 3;

    public static final BigDecimal ZERO_WEIGHT = BigDecimal.ZERO.setScale(WEIGHT_SCALE);
    public static final BigDecimal ZERO_VOLUME = BigDecimal.ZERO.setScale(VOLUME_SCALE);
}