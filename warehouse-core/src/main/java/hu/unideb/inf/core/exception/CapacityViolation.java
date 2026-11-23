package hu.unideb.inf.core.exception;

import java.math.BigDecimal;
import java.util.Objects;

public record CapacityViolation(
        String dimension,
        BigDecimal maxLimit,
        BigDecimal calculatedValue,
        String unit
) {
    public CapacityViolation {
        Objects.requireNonNull(dimension, "dimension must not be null");
        Objects.requireNonNull(maxLimit, "maxLimit must not be null");
        Objects.requireNonNull(calculatedValue, "calculatedValue must not be null");
        Objects.requireNonNull(unit, "unit must not be null");
    }
}

