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
    }

    public ItemSize(double unitWeight, double unitVolume) {
        this(BigDecimal.valueOf(unitWeight), BigDecimal.valueOf(unitVolume));
    }
}
