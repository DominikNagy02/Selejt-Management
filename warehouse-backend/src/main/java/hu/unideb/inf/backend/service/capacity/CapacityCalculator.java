package hu.unideb.inf.backend.service.capacity;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Objects;

@Component
public class CapacityCalculator {

    public BigDecimal calculateNewWeight(ShelfLoad shelf, ItemSize item, int quantity) {
        validateInputs(shelf, item, quantity);
        return shelf.currentWeight().add(item.unitWeight().multiply(BigDecimal.valueOf(quantity)));
    }

    public BigDecimal calculateNewVolume(ShelfLoad shelf, ItemSize item, int quantity) {
        validateInputs(shelf, item, quantity);
        return shelf.currentVolume().add(item.unitVolume().multiply(BigDecimal.valueOf(quantity)));
    }

    private void validateInputs(ShelfLoad shelf, ItemSize item, int quantity) {
        Objects.requireNonNull(shelf, "shelf must not be null");
        Objects.requireNonNull(item, "item must not be null");
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be greater than 0");
        }
    }
}
