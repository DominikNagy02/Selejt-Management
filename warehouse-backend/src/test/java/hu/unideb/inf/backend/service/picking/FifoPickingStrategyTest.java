package hu.unideb.inf.backend.service.picking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FifoPickingStrategyTest {

    private FifoPickingStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new FifoPickingStrategy();
    }

    @Test
    void typeShouldReturnFifo() {
        assertEquals(PickingStrategyType.FIFO, strategy.type());
    }

    @Test
    void allocateShouldOrderLotsByReceivedAtAscending() {
        StockLot newest = new StockLot(
                3L, "S-01-03", "BATCH-3", 20,
                Instant.parse("2026-10-15T10:00:00Z"), LocalDate.of(2026, 12, 1)
        );
        StockLot oldest = new StockLot(
                1L, "S-01-01", "BATCH-1", 10,
                Instant.parse("2026-10-01T08:00:00Z"), LocalDate.of(2027, 6, 1)
        );
        StockLot middle = new StockLot(
                2L, "S-01-02", "BATCH-2", 15,
                Instant.parse("2026-10-10T12:00:00Z"), null
        );

        List<Allocation> allocations = strategy.allocate(List.of(newest, oldest, middle), 30);

        assertEquals(3, allocations.size());
        assertEquals(new Allocation(1L, "S-01-01", "BATCH-1", 10), allocations.get(0));
        assertEquals(new Allocation(2L, "S-01-02", "BATCH-2", 15), allocations.get(1));
        assertEquals(new Allocation(3L, "S-01-03", "BATCH-3", 5), allocations.get(2));
    }

    @Test
    void allocateShouldTieBreakByInventoryItemIdWhenReceivedAtIsEqual() {
        Instant sameTime = Instant.parse("2026-10-05T09:00:00Z");
        StockLot secondId = new StockLot(20L, "S-02-02", "BATCH-B", 10, sameTime, null);
        StockLot firstId = new StockLot(10L, "S-02-01", "BATCH-A", 10, sameTime, null);

        List<Allocation> allocations = strategy.allocate(List.of(secondId, firstId), 15);

        assertEquals(2, allocations.size());
        assertEquals(10L, allocations.get(0).inventoryItemId());
        assertEquals(20L, allocations.get(1).inventoryItemId());
    }
}

