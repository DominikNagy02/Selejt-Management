package hu.unideb.inf.backend.service.picking;

import hu.unideb.inf.core.exception.InsufficientStockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbstractPickingStrategyTest {

    private PickingStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new FifoPickingStrategy();
    }

    // F3.5 + F3.11: 30 kért darab, tételek: 10 + 15 + 50 (+ 20) -> allokációk: 10, 15, 5
    @Test
    void allocateShouldSplitQuantityAcrossMultipleBatchesAndStopWhenFulfilled() {
        StockLot zeroStockLot = new StockLot(
                99L, "S-00-00", "BATCH-EMPTY", 0,
                Instant.parse("2026-09-01T08:00:00Z"), null
        );
        StockLot lot1 = new StockLot(
                1L, "S-01-01", "BATCH-1", 10,
                Instant.parse("2026-10-01T08:00:00Z"), null
        );
        StockLot lot2 = new StockLot(
                2L, "S-01-02", "BATCH-2", 15,
                Instant.parse("2026-10-02T08:00:00Z"), null
        );
        StockLot lot3 = new StockLot(
                3L, "S-01-03", "BATCH-3", 50,
                Instant.parse("2026-10-03T08:00:00Z"), null
        );
        StockLot lot4 = new StockLot(
                4L, "S-01-04", "BATCH-4", 20,
                Instant.parse("2026-10-04T08:00:00Z"), null
        );

        List<Allocation> allocations = strategy.allocate(
                List.of(zeroStockLot, lot1, lot2, lot3, lot4),
                30
        );

        assertEquals(3, allocations.size());
        assertEquals(new Allocation(1L, "S-01-01", "BATCH-1", 10), allocations.get(0));
        assertEquals(new Allocation(2L, "S-01-02", "BATCH-2", 15), allocations.get(1));
        assertEquals(new Allocation(3L, "S-01-03", "BATCH-3", 5), allocations.get(2));

        int totalAllocated = allocations.stream().mapToInt(Allocation::quantity).sum();
        assertEquals(30, totalAllocated);
    }

    // F3.7 + F3.11: 100 kért darab, 60 elérhető -> InsufficientStockException
    @Test
    void allocateShouldThrowInsufficientStockExceptionWhenTotalStockIsNotEnough() {
        StockLot lot1 = new StockLot(
                1L, "S-01-01", "BATCH-1", 25,
                Instant.parse("2026-10-01T08:00:00Z"), null
        );
        StockLot lot2 = new StockLot(
                2L, "S-01-02", "BATCH-2", 35,
                Instant.parse("2026-10-02T08:00:00Z"), null
        );

        InsufficientStockException ex = assertThrows(
                InsufficientStockException.class,
                () -> strategy.allocate(List.of(lot1, lot2), 100)
        );

        assertNull(ex.getProductSku());
        assertEquals(100, ex.getRequestedQuantity());
        assertEquals(60, ex.getAvailableQuantity());
        assertTrue(ex.getMessage().contains("100"));
        assertTrue(ex.getMessage().contains("60"));

        InsufficientStockException withSku = new InsufficientStockException("SKU-1092", 10, 3);
        assertEquals("SKU-1092", withSku.getProductSku());
        assertTrue(withSku.getMessage().contains("SKU-1092"));

        InsufficientStockException blankSku = new InsufficientStockException("   ", 10, 3);
        assertEquals("   ", blankSku.getProductSku());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -50})
    void allocateShouldRejectNonPositiveQuantity(int invalidQuantity) {
        assertThrows(
                IllegalArgumentException.class,
                () -> strategy.allocate(List.of(), invalidQuantity)
        );
    }

    @Test
    void allocateAndModelsShouldValidateNullAndInvalidArguments() {
        assertThrows(NullPointerException.class, () -> strategy.allocate(null, 5));
        assertThrows(NullPointerException.class, () -> strategy.allocate(Arrays.asList((StockLot) null), 5));

        assertThrows(
                IllegalArgumentException.class,
                () -> new StockLot(1L, "S-01", "B-1", -1, Instant.now(), null)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new Allocation(1L, "S-01", "B-1", 0)
        );
    }
}

