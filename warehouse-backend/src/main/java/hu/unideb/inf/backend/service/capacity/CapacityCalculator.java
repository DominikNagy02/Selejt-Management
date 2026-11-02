package hu.unideb.inf.backend.service.capacity;

import java.util.Objects;

public class CapacityCalculator {

    public double calculateNewWeight(ShelfLoad shelf, ItemSize item, int quantity) {
        Objects.requireNonNull(shelf, "shelf must not be null");
        Objects.requireNonNull(item, "item must not be null");
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be greater than 0");
        }
        return shelf.currentWeight() + (item.unitWeight() * quantity);
    }
}

