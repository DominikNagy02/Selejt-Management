package hu.unideb.inf.backend.service.capacity;

import hu.unideb.inf.core.exception.CapacityViolation;
import hu.unideb.inf.core.exception.ShelfCapacityExceededException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class CapacityValidator {

    private final CapacityCalculator calculator;

    public CapacityValidator(CapacityCalculator calculator) {
        this.calculator = Objects.requireNonNull(calculator, "calculator must not be null");
    }

    public CapacityCheckResult check(ShelfLoad shelf, ItemSize item, int quantity) {
        BigDecimal newWeight = calculator.calculateNewWeight(shelf, item, quantity);
        BigDecimal newVolume = calculator.calculateNewVolume(shelf, item, quantity);

        boolean weightOk = newWeight.compareTo(shelf.maxWeight()) <= 0;
        boolean volumeOk = newVolume.compareTo(shelf.maxVolume()) <= 0;

        return new CapacityCheckResult(newWeight, newVolume, weightOk, volumeOk);
    }

    public CapacityCheckResult validate(ShelfLoad shelf, ItemSize item, int quantity) {
        CapacityCheckResult result = check(shelf, item, quantity);

        List<CapacityViolation> violations = new ArrayList<>(2);
        if (!result.weightOk()) {
            violations.add(new CapacityViolation(
                    "weight", shelf.maxWeight(), result.newWeight(), "kg"
            ));
        }
        if (!result.volumeOk()) {
            violations.add(new CapacityViolation(
                    "volume", shelf.maxVolume(), result.newVolume(), "m³"
            ));
        }

        if (!violations.isEmpty()) {
            throw new ShelfCapacityExceededException(shelf.code(), violations);
        }

        return result;
    }
}
