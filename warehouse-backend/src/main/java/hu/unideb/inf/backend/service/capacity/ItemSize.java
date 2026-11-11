package hu.unideb.inf.backend.service.capacity;

import java.math.BigDecimal;
import java.util.Objects;

public record ItemSize(
        BigDecimal unitWeight,
        BigDecimal unitVolume
) {
    public ItemSize {
        Objects.requireNonNull(unitWeight, "unitWeight must not be null");
        Objects.requireNonNull(unitVolume, "unitVolume must not be null");
        if (unitWeight.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("unitWeight must be greater than 0");
        }
        if (unitVolume.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("unitVolume must be greater than 0");
        }
    }
}
