package hu.unideb.inf.core.exception;

public class ShelfCapacityExceededException extends RuntimeException {

    private final String shelfCode;
    private final double maxLimit;
    private final double calculatedValue;
    private final String unit;

    public ShelfCapacityExceededException(String shelfCode, double maxLimit, double calculatedValue, String unit) {
        this(shelfCode, maxLimit, calculatedValue, unit, buildMessage(shelfCode, maxLimit, calculatedValue, unit));
    }

    public ShelfCapacityExceededException(
            String shelfCode,
            double maxLimit,
            double calculatedValue,
            String unit,
            String message
    ) {
        super(message);
        this.shelfCode = shelfCode;
        this.maxLimit = maxLimit;
        this.calculatedValue = calculatedValue;
        this.unit = unit;
    }

    private static String buildMessage(String shelfCode, double maxLimit, double calculatedValue, String unit) {
        return String.format(
                "A polc (%s) kapacitása meghaladná a maximális %.2f %s értéket. Számított új érték: %.2f %s.",
                shelfCode, maxLimit, unit, calculatedValue, unit
        );
    }

    public String getShelfCode() {
        return shelfCode;
    }

    public double getMaxLimit() {
        return maxLimit;
    }

    public double getCalculatedValue() {
        return calculatedValue;
    }

    public String getUnit() {
        return unit;
    }
}

