package hu.unideb.inf.core.exception;

import java.math.BigDecimal;

public class ShelfCapacityExceededException extends RuntimeException {

    private final String shelfCode;
    private final BigDecimal maxLimit;
    private final BigDecimal calculatedValue;
    private final String unit;

    public ShelfCapacityExceededException(
            String shelfCode,
            BigDecimal maxLimit,
            BigDecimal calculatedValue,
            String unit
    ) {
        this(shelfCode, maxLimit, calculatedValue, unit, buildMessage(shelfCode, maxLimit, calculatedValue, unit));
    }

    public ShelfCapacityExceededException(
            String shelfCode,
            BigDecimal maxLimit,
            BigDecimal calculatedValue,
            String unit,
            String message
    ) {
        super(message);
        this.shelfCode = shelfCode;
        this.maxLimit = maxLimit;
        this.calculatedValue = calculatedValue;
        this.unit = unit;
    }

    private static String buildMessage(
            String shelfCode,
            BigDecimal maxLimit,
            BigDecimal calculatedValue,
            String unit
    ) {
        return String.format(
                "A polc (%s) kapacitása meghaladná a maximális %s %s értéket. Számított új érték: %s %s.",
                shelfCode, maxLimit.toPlainString(), unit, calculatedValue.toPlainString(), unit
        );
    }

    public String getShelfCode() {
        return shelfCode;
    }

    public BigDecimal getMaxLimit() {
        return maxLimit;
    }

    public BigDecimal getCalculatedValue() {
        return calculatedValue;
    }

    public String getUnit() {
        return unit;
    }
}
