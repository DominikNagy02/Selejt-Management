package hu.unideb.inf.backend.service.picking;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Objects;

public class FefoPickingStrategy extends AbstractPickingStrategy {

    private static final Comparator<StockLot> FEFO_COMPARATOR =
            Comparator.comparing(StockLot::expirationDate, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(StockLot::receivedAt)
                    .thenComparing(StockLot::inventoryItemId);

    private final Clock clock;

    public FefoPickingStrategy() {
        this(Clock.systemDefaultZone());
    }

    public FefoPickingStrategy(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    protected boolean isEligible(StockLot lot) {
        LocalDate expirationDate = lot.expirationDate();
        return expirationDate == null || !expirationDate.isBefore(LocalDate.now(clock));
    }

    @Override
    protected Comparator<StockLot> comparator() {
        return FEFO_COMPARATOR;
    }

    @Override
    public PickingStrategyType type() {
        return PickingStrategyType.FEFO;
    }
}
