package hu.unideb.inf.backend.service.picking;

import java.util.List;

public interface PickingStrategy {

    List<Allocation> allocate(List<StockLot> lots, int quantity);

    PickingStrategyType type();
}

