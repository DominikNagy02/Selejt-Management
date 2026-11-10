package hu.unideb.inf.backend.service.picking;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public abstract class AbstractPickingStrategy implements PickingStrategy {

    @Override
    public List<Allocation> allocate(List<StockLot> lots, int quantity) {
        Objects.requireNonNull(lots, "lots must not be null");
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be greater than 0");
        }

        List<StockLot> sortedLots = lots.stream()
                .filter(lot -> lot.availableQuantity() > 0)
                .sorted(comparator())
                .toList();

        if (sortedLots.isEmpty()) {
            return List.of();
        }

        StockLot firstLot = sortedLots.get(0);
        return List.of(new Allocation(
                firstLot.inventoryItemId(),
                firstLot.shelfCode(),
                firstLot.batchNumber(),
                quantity
        ));
    }

    protected abstract Comparator<StockLot> comparator();
}

