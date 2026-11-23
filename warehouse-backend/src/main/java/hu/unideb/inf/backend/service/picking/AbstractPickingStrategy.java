package hu.unideb.inf.backend.service.picking;

import hu.unideb.inf.core.exception.InsufficientStockException;

import java.util.ArrayList;
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
        for (StockLot lot : lots) {
            Objects.requireNonNull(lot, "lot must not be null");
        }

        List<StockLot> sortedLots = lots.stream()
                .filter(lot -> lot.availableQuantity() > 0)
                .filter(this::isEligible)
                .sorted(comparator())
                .toList();

        List<Allocation> allocations = new ArrayList<>();
        int remaining = quantity;

        for (StockLot lot : sortedLots) {
            if (remaining <= 0) {
                break;
            }
            int allocatedQty = Math.min(remaining, lot.availableQuantity());
            allocations.add(new Allocation(
                    lot.inventoryItemId(),
                    lot.shelfCode(),
                    lot.batchNumber(),
                    allocatedQty
            ));
            remaining -= allocatedQty;
        }

        if (remaining > 0) {
            int availableQuantity = quantity - remaining;
            throw new InsufficientStockException(quantity, availableQuantity);
        }

        return List.copyOf(allocations);
    }

    protected boolean isEligible(StockLot lot) {
        return true;
    }

    protected abstract Comparator<StockLot> comparator();
}
