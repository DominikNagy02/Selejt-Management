package hu.unideb.inf.backend.service.capacity;

import hu.unideb.inf.core.exception.ShelfCapacityExceededException;
import org.springframework.stereotype.Component;

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
        double newWeight = calculator.calculateNewWeight(shelf, item, quantity);
        double newVolume = calculator.calculateNewVolume(shelf, item, quantity);

        boolean weightOk = newWeight <= shelf.maxWeight();
        boolean volumeOk = newVolume <= shelf.maxVolume();

        return new CapacityCheckResult(newWeight, newVolume, weightOk, volumeOk);
    }

    public CapacityCheckResult validate(ShelfLoad shelf, ItemSize item, int quantity) {
        CapacityCheckResult result = check(shelf, item, quantity);

        if (!result.weightOk() && !result.volumeOk()) {
            String message = String.format(
                    "A polc (%s) súly- és térfogatkorlátja is túllépésre kerülne: súly %.2f / %.2f kg, térfogat %.2f / %.2f m³.",
                    shelf.code(), result.newWeight(), shelf.maxWeight(), result.newVolume(), shelf.maxVolume()
            );
            throw new ShelfCapacityExceededException(
                    shelf.code(), shelf.maxWeight(), result.newWeight(), "kg, m³", message
            );
        }

        if (!result.weightOk()) {
            String message = String.format(
                    "A polc (%s) teherbírása meghaladná a maximális %.2f kg-ot. Számított új súly: %.2f kg.",
                    shelf.code(), shelf.maxWeight(), result.newWeight()
            );
            throw new ShelfCapacityExceededException(
                    shelf.code(), shelf.maxWeight(), result.newWeight(), "kg", message
            );
        }

        if (!result.volumeOk()) {
            String message = String.format(
                    "A polc (%s) térfogata meghaladná a maximális %.2f m³-t. Számított új térfogat: %.2f m³.",
                    shelf.code(), shelf.maxVolume(), result.newVolume()
            );
            throw new ShelfCapacityExceededException(
                    shelf.code(), shelf.maxVolume(), result.newVolume(), "m³", message
            );
        }

        return result;
    }
}

