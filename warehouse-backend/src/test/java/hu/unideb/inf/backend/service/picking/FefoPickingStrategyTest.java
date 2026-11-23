package hu.unideb.inf.backend.service.picking;

import hu.unideb.inf.core.exception.InsufficientStockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FefoPickingStrategyTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-11-10T12:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 10);

    private FefoPickingStrategy strategy;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
        strategy = new FefoPickingStrategy(fixedClock);
    }

    @Test
    void defaultConstructorShouldInitializeSystemClockAndReturnFefoType() {
        FefoPickingStrategy defaultStrategy = new FefoPickingStrategy();
        assertEquals(PickingStrategyType.FEFO, defaultStrategy.type());
    }

    @Test
    void allocateShouldOrderByExpirationDateWithNullsLastAndTieBreakByReceivedAt() {
        StockLot noExpiry = new StockLot(
                4L, "S-01-04", "BATCH-NO-EXP", 20,
                Instant.parse("2026-09-01T08:00:00Z"), null
        );
        StockLot laterExpiry = new StockLot(
                3L, "S-01-03", "BATCH-LATER", 10,
                Instant.parse("2026-10-01T08:00:00Z"), TODAY.plusDays(30)
        );
        StockLot sameSoonExpiryNewerReceived = new StockLot(
                2L, "S-01-02", "BATCH-SOON-2", 10,
                Instant.parse("2026-10-05T08:00:00Z"), TODAY.plusDays(5)
        );
        StockLot sameSoonExpiryOlderReceived = new StockLot(
                1L, "S-01-01", "BATCH-SOON-1", 10,
                Instant.parse("2026-10-01T08:00:00Z"), TODAY.plusDays(5)
        );

        List<Allocation> allocations = strategy.allocate(
                List.of(noExpiry, laterExpiry, sameSoonExpiryNewerReceived, sameSoonExpiryOlderReceived),
                35
        );

        assertEquals(4, allocations.size());
        assertEquals(1L, allocations.get(0).inventoryItemId());
        assertEquals(2L, allocations.get(1).inventoryItemId());
        assertEquals(3L, allocations.get(2).inventoryItemId());
        assertEquals(4L, allocations.get(3).inventoryItemId());
        assertEquals(5, allocations.get(3).quantity());
    }

    @Test
    void allocateShouldSkipExpiredLotsAndAcceptLotsExpiringToday() {
        StockLot expiredYesterday = new StockLot(
                1L, "S-01-01", "BATCH-EXPIRED", 50,
                Instant.parse("2026-09-01T08:00:00Z"), TODAY.minusDays(1)
        );
        StockLot expiringToday = new StockLot(
                2L, "S-01-02", "BATCH-TODAY", 15,
                Instant.parse("2026-10-01T08:00:00Z"), TODAY
        );

        List<Allocation> allocations = strategy.allocate(List.of(expiredYesterday, expiringToday), 10);

        assertEquals(1, allocations.size());
        assertEquals(new Allocation(2L, "S-01-02", "BATCH-TODAY", 10), allocations.get(0));
    }

    @Test
    void allocateShouldThrowInsufficientStockWhenOnlyExpiredLotsExist() {
        StockLot expiredLot = new StockLot(
                1L, "S-01-01", "BATCH-EXPIRED", 100,
                Instant.parse("2026-09-01T08:00:00Z"), TODAY.minusDays(1)
        );

        InsufficientStockException ex = assertThrows(
                InsufficientStockException.class,
                () -> strategy.allocate(List.of(expiredLot), 10)
        );

        assertEquals(10, ex.getRequestedQuantity());
        assertEquals(0, ex.getAvailableQuantity());
    }
}

