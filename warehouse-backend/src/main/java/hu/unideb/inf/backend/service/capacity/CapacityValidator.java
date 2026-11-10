package hu.unideb.inf.backend.service.capacity;

import hu.unideb.inf.core.exception.ShelfCapacityExceededException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Objects;

@Component
public class CapacityValidator {

    private final CapacityCalculator calculator;

    public CapacityValidator() {
        this(new CapacityCalculator());
    }

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
            String message = String.format(
                    "A polc (%s) súly- és térfogatkorlátja is túllépésre kerülne: súly %s / %s kg, térfogat %s / %s m³.",
                    shelf.code(),
                    result.newWeight().toPlainString(),
                    shelf.maxWeight().toPlainString(),
                    result.newVolume().toPlainString(),
                    shelf.maxVolume().toPlainString()
            );
            throw new ShelfCapacityExceededException(
                    shelf.code(), shelf.maxWeight(), result.newWeight(), "kg, m³", message
            );
        }

        if (!result.weightOk()) {
            String message = String.format(
                    "A polc (%s) teherbírása meghaladná a maximális %s kg-ot. Számított új súly: %s kg.",
                    shelf.code(),
                    shelf.maxWeight().toPlainString(),
                    result.newWeight().toPlainString()
            );
            throw new ShelfCapacityExceededException(
                    shelf.code(), shelf.maxWeight(), result.newWeight(), "kg", message
            );
        }

        if (!result.volumeOk()) {
            String message = String.format(
                    "A polc (%s) térfogata meghaladná a maximális %s m³-t. Számított új térfogat: %s m³.",
                    shelf.code(),
                    shelf.maxVolume().toPlainString(),
                    result.newVolume().toPlainString()
            );
            throw new ShelfCapacityExceededException(
                    shelf.code(), shelf.maxVolume(), result.newVolume(), "m³", message
            );
        }

        return result;
    }
}
