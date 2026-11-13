package hu.unideb.inf.core.exception;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ShelfCapacityExceededException extends WmsBusinessException {

    public static final String ERROR_CODE = "shelf-capacity-exceeded";
    public static final String TITLE = "Shelf Capacity Exceeded";

    private final String shelfCode;
    private final BigDecimal maxLimit;
    private final BigDecimal calculatedValue;
    private final String unit;
    private final List<CapacityViolation> violations;

    public ShelfCapacityExceededException(
            String shelfCode,
            BigDecimal maxLimit,
            BigDecimal calculatedValue,
            String unit
    ) {
        this(
                shelfCode,
                maxLimit,
                calculatedValue,
                unit,
                buildMessage(shelfCode, maxLimit, calculatedValue, unit)
        );
    }

    public ShelfCapacityExceededException(
            String shelfCode,
            BigDecimal maxLimit,
            BigDecimal calculatedValue,
            String unit,
            String message
    ) {
        this(
                shelfCode,
                List.of(new CapacityViolation(resolveDimension(unit), maxLimit, calculatedValue, unit)),
                message
        );
    }

    public ShelfCapacityExceededException(
            String shelfCode,
            List<CapacityViolation> violations,
            String message
    ) {
        super(ERROR_CODE, TITLE, 409, message);
        this.shelfCode = Objects.requireNonNull(shelfCode, "shelfCode must not be null");
        Objects.requireNonNull(violations, "violations must not be null");
        if (violations.isEmpty()) {
            throw new IllegalArgumentException("violations must not be empty");
        }
        this.violations = List.copyOf(violations);
        CapacityViolation primary = this.violations.get(0);
        this.maxLimit = primary.maxLimit();
        this.calculatedValue = primary.calculatedValue();
        this.unit = this.violations.stream()
                .map(CapacityViolation::unit)
                .collect(Collectors.joining(", "));
    }

    public static String formatDecimal(BigDecimal value) {
        Objects.requireNonNull(value, "value must not be null");
        return value.stripTrailingZeros().toPlainString();
    }

    private static String resolveDimension(String unit) {
        return "m³".equals(unit) ? "volume" : "weight";
    }

    private static String buildMessage(
            String shelfCode,
            BigDecimal maxLimit,
            BigDecimal calculatedValue,
            String unit
    ) {
        return String.format(
                "A polc (%s) kapacitása meghaladná a maximális %s %s értéket. Számított új érték: %s %s.",
                shelfCode, formatDecimal(maxLimit), unit, formatDecimal(calculatedValue), unit
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

    public List<CapacityViolation> getViolations() {
        return violations;
    }
}
