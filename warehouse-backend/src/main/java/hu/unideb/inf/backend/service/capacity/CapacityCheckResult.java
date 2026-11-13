package hu.unideb.inf.backend.service.capacity;

import java.math.BigDecimal;
import java.util.Objects;

public record CapacityCheckResult(
        BigDecimal newWeight,
        BigDecimal newVolume,
        boolean weightOk,
        boolean volumeOk
) {
    public CapacityCheckResult {
        Objects.requireNonNull(newWeight, "newWeight must not be null");
        Objects.requireNonNull(newVolume, "newVolume must not be null");
    }

    public boolean isAllowed() {
        return weightOk && volumeOk;
    }
}
