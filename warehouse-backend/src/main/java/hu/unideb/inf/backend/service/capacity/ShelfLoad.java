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
    }

    public ShelfLoad(
            String code,
            double currentWeight,
            double currentVolume,
            double maxWeight,
            double maxVolume
    ) {
        this(
                code,
                BigDecimal.valueOf(currentWeight),
                BigDecimal.valueOf(currentVolume),
                BigDecimal.valueOf(maxWeight),
                BigDecimal.valueOf(maxVolume)
        );
    }
}
