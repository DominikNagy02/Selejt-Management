package hu.unideb.inf.backend.service.picking;

import java.util.Comparator;

public class FifoPickingStrategy extends AbstractPickingStrategy {

    private static final Comparator<StockLot> FIFO_COMPARATOR =
            Comparator.comparing(StockLot::receivedAt)
                    .thenComparing(StockLot::inventoryItemId);

    @Override
    protected Comparator<StockLot> comparator() {
        return FIFO_COMPARATOR;
    }

    @Override
    public PickingStrategyType type() {
        return PickingStrategyType.FIFO;
    }
}

