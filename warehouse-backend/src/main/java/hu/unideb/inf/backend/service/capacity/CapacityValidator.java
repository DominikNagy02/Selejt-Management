package hu.unideb.inf.backend.service.capacity;

import hu.unideb.inf.core.exception.CapacityViolation;
import hu.unideb.inf.core.exception.ShelfCapacityExceededException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import static hu.unideb.inf.core.exception.ShelfCapacityExceededException.formatDecimal;

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

        if (!result.weightOk() && !result.volumeOk()) {
            CapacityViolation weightViolation = new CapacityViolation(
                    "weight", shelf.maxWeight(), result.newWeight(), "kg"
            );
            CapacityViolation volumeViolation = new CapacityViolation(
                    "volume", shelf.maxVolume(), result.newVolume(), "m³"
            );
            String message = String.format(
                    "A polc (%s) súly- és térfogatkorlátja is túllépésre kerülne: súly %s / %s kg, térfogat %s / %s m³.",
                    shelf.code(),
                    formatDecimal(result.newWeight()),
                    formatDecimal(shelf.maxWeight()),
                    formatDecimal(result.newVolume()),
                    formatDecimal(shelf.maxVolume())
            );
            throw new ShelfCapacityExceededException(
                    shelf.code(),
                    List.of(weightViolation, volumeViolation),
                    message
            );
        }

        if (!result.weightOk()) {
            CapacityViolation weightViolation = new CapacityViolation(
                    "weight", shelf.maxWeight(), result.newWeight(), "kg"
            );
            String message = String.format(
                    "A polc (%s) teherbírása meghaladná a maximális %s kg-ot. Számított új súly: %s kg.",
                    shelf.code(),
                    formatDecimal(shelf.maxWeight()),
                    formatDecimal(result.newWeight())
            );
            throw new ShelfCapacityExceededException(
                    shelf.code(),
                    List.of(weightViolation),
                    message
            );
        }

        if (!result.volumeOk()) {
            CapacityViolation volumeViolation = new CapacityViolation(
                    "volume", shelf.maxVolume(), result.newVolume(), "m³"
            );
            String message = String.format(
                    "A polc (%s) térfogata meghaladná a maximális %s m³-t. Számított új térfogat: %s m³.",
                    shelf.code(),
                    formatDecimal(shelf.maxVolume()),
                    formatDecimal(result.newVolume())
            );
            throw new ShelfCapacityExceededException(
                    shelf.code(),
                    List.of(volumeViolation),
                    message
            );
        }

        return result;
    }
}
