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
                List.of(new CapacityViolation(resolveDimension(unit), maxLimit, calculatedValue, unit))
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
            List<CapacityViolation> violations
    ) {
        this(shelfCode, violations, buildMessage(shelfCode, violations));
    }

    public ShelfCapacityExceededException(
            String shelfCode,
            List<CapacityViolation> violations,
            String message
    ) {
        super(ERROR_CODE, TITLE, 409, message);
        this.shelfCode = Objects.requireNonNull(shelfCode, "shelfCode must not be null");
        validateViolations(violations);
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

    private static void validateViolations(List<CapacityViolation> violations) {
        Objects.requireNonNull(violations, "violations must not be null");
        if (violations.isEmpty()) {
            throw new IllegalArgumentException("violations must not be empty");
        }
    }

    private static String buildMessage(String shelfCode, List<CapacityViolation> violations) {
        Objects.requireNonNull(shelfCode, "shelfCode must not be null");
        validateViolations(violations);

        if (violations.size() > 1) {
            CapacityViolation weightViolation = violations.get(0);
            CapacityViolation volumeViolation = violations.get(1);
            return String.format(
                    "A polc (%s) súly- és térfogatkorlátja is túllépésre kerülne: súly %s / %s %s, térfogat %s / %s %s.",
                    shelfCode,
                    formatDecimal(weightViolation.calculatedValue()),
                    formatDecimal(weightViolation.maxLimit()),
                    weightViolation.unit(),
                    formatDecimal(volumeViolation.calculatedValue()),
                    formatDecimal(volumeViolation.maxLimit()),
                    volumeViolation.unit()
            );
        }

        CapacityViolation violation = violations.get(0);
        if ("volume".equals(violation.dimension())) {
            return String.format(
                    "A polc (%s) térfogata meghaladná a maximális %s %s-t. Számított új térfogat: %s %s.",
                    shelfCode,
                    formatDecimal(violation.maxLimit()),
                    violation.unit(),
                    formatDecimal(violation.calculatedValue()),
                    violation.unit()
            );
        }

        return String.format(
                "A polc (%s) teherbírása meghaladná a maximális %s %s-ot. Számított új súly: %s %s.",
                shelfCode,
                formatDecimal(violation.maxLimit()),
                violation.unit(),
                formatDecimal(violation.calculatedValue()),
                violation.unit()
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
