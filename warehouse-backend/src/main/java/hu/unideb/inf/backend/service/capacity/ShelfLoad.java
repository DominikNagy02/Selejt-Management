package hu.unideb.inf.backend.service.capacity;

import java.math.BigDecimal;
import java.util.Objects;

public record ShelfLoad(
        String code,
        BigDecimal currentWeight,
        BigDecimal currentVolume,
        BigDecimal maxWeight,
        BigDecimal maxVolume
) {
    public ShelfLoad {
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(currentWeight, "currentWeight must not be null");
        Objects.requireNonNull(currentVolume, "currentVolume must not be null");
        Objects.requireNonNull(maxWeight, "maxWeight must not be null");
        Objects.requireNonNull(maxVolume, "maxVolume must not be null");
        if (code.isBlank()) {
            throw new IllegalArgumentException("code must not be blank");
        }
        if (currentWeight.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("currentWeight must not be negative");
        }
        if (currentVolume.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("currentVolume must not be negative");
        }
        if (maxWeight.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("maxWeight must be greater than 0");
        }
        if (maxVolume.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("maxVolume must be greater than 0");
        }
    }
}
