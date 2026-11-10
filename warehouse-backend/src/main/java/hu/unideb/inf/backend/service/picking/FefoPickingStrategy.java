package hu.unideb.inf.backend.service.picking;

import java.util.Comparator;

public class FefoPickingStrategy extends AbstractPickingStrategy {

    private static final Comparator<StockLot> FEFO_COMPARATOR =
            Comparator.comparing(StockLot::expirationDate, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(StockLot::receivedAt)
                    .thenComparing(StockLot::inventoryItemId);

    @Override
    protected Comparator<StockLot> comparator() {
        return FEFO_COMPARATOR;
    }

    @Override
    public PickingStrategyType type() {
        return PickingStrategyType.FEFO;
    }
}

