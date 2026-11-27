package hu.unideb.inf.backend.service.reservation;

import hu.unideb.inf.backend.model.entity.InventoryItemEntity;
import hu.unideb.inf.backend.model.entity.ProductEntity;
import hu.unideb.inf.backend.model.entity.ShelfEntity;
import hu.unideb.inf.backend.model.entity.StockReservationEntity;
import hu.unideb.inf.backend.model.entity.WarehouseEntity;
import hu.unideb.inf.backend.model.entity.ZoneEntity;
import hu.unideb.inf.backend.repository.InventoryItemRepository;
import hu.unideb.inf.backend.repository.LotRepository;
import hu.unideb.inf.backend.repository.ProductRepository;
import hu.unideb.inf.backend.repository.ShelfRepository;
import hu.unideb.inf.backend.repository.StockReservationRepository;
import hu.unideb.inf.backend.repository.WarehouseRepository;
import hu.unideb.inf.backend.repository.ZoneRepository;
import hu.unideb.inf.backend.service.inventory.InventoryService;
import hu.unideb.inf.core.dto.InboundRequest;
import hu.unideb.inf.core.dto.InboundResponse;
import hu.unideb.inf.core.enums.InventoryStatus;
import hu.unideb.inf.core.enums.ReservationStatus;
import hu.unideb.inf.core.enums.ReservationType;
import hu.unideb.inf.core.enums.StorageType;
import hu.unideb.inf.core.enums.ZoneType;
import hu.unideb.inf.core.exception.InsufficientStockException;
import hu.unideb.inf.core.exception.ReservationConflictException;
import hu.unideb.inf.core.exception.ResourceNotFoundException;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "wms.scheduling.enabled=false")
@ActiveProfiles("test")
@Import(StockReservationIntegrationTest.MutableClockConfig.class)
class StockReservationIntegrationTest {

    private static final Instant BASE_TIME = Instant.parse("2026-11-27T10:00:00Z");

    @TestConfiguration
    static class MutableClockConfig {

        @Bean
        @Primary
        MutableClock mutableClock() {
            return new MutableClock(BASE_TIME, ZoneOffset.UTC);
        }
    }

    static final class MutableClock extends Clock {
        private final AtomicReference<Instant> currentInstant;
        private final ZoneId zone;

        MutableClock(Instant initial, ZoneId zone) {
            this.currentInstant = new AtomicReference<>(initial);
            this.zone = zone;
        }

        void setInstant(Instant instant) {
            currentInstant.set(instant);
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(currentInstant.get(), zone);
        }

        @Override
        public Instant instant() {
            return currentInstant.get();
        }
    }

    @Autowired
    private MutableClock mutableClock;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private StockReservationService stockReservationService;

    @Autowired
    private ReservationTimeoutJob reservationTimeoutJob;

    @Autowired
    private StockReservationRepository stockReservationRepository;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private LotRepository lotRepository;

    @Autowired
    private ShelfRepository shelfRepository;

    @Autowired
    private ZoneRepository zoneRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long testUserId;
    private WarehouseEntity warehouse;
    private ShelfEntity shelf;
    private ProductEntity product;

    @BeforeEach
    void setUp() {
        mutableClock.setInstant(BASE_TIME);
        cleanUp();

        jdbcTemplate.update(
                "INSERT INTO users (username, password_hash, full_name, role, active, version, created_at, updated_at) "
                        + "VALUES ('res_dispatcher', 'hash', 'Dispatcher', 'DISPATCHER', true, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
        );
        testUserId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE username = 'res_dispatcher'",
                Long.class
        );

        warehouse = warehouseRepository.save(
                new WarehouseEntity("WH-RES-01", "Debrecen", "Kassai út 26.", true)
        );
        ZoneEntity zone = zoneRepository.save(
                new ZoneEntity(warehouse, "Z-RES-NORM", "Normál", ZoneType.NORMAL, true)
        );
        shelf = shelfRepository.save(new ShelfEntity(
                zone, "S-RES-01", StorageType.RACK, 3,
                new BigDecimal("200.000"), new BigDecimal("4.000000"), true
        ));
        product = productRepository.save(new ProductEntity(
                "SKU-RES-01", "5993000000001", "Érzékelő", null,
                new BigDecimal("2.500"), new BigDecimal("0.050000"), ZoneType.NORMAL, 5, true
        ));
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    private void cleanUp() {
        stockReservationRepository.deleteAll();
        inventoryItemRepository.deleteAll();
        lotRepository.deleteAll();
        shelfRepository.deleteAll();
        zoneRepository.deleteAll();
        warehouseRepository.deleteAll();
        productRepository.deleteAll();
        jdbcTemplate.update("DELETE FROM users WHERE username = 'res_dispatcher'");
    }

    @Test
    void reserveReleaseReReserveAndConsumeUpdatesStockAndShelfInOneTransaction() {
        InboundResponse earlierLot = inventoryService.receive(new InboundRequest(
                product.getId(), shelf.getId(), 0, "BATCH-EARLY", LocalDate.of(2026, 12, 10), 4
        ));
        InboundResponse laterLot = inventoryService.receive(new InboundRequest(
                product.getId(), shelf.getId(), 1, "BATCH-LATE", LocalDate.of(2027, 6, 10), 6
        ));
        inventoryService.receive(new InboundRequest(
                product.getId(), shelf.getId(), 2, "BATCH-EXPIRED", LocalDate.of(2026, 11, 1), 5
        ));

        assertEquals(0, stockReservationService.availableQuantity(null));
        assertEquals(15, stockReservationService.availableQuantityForProduct(product.getId(), warehouse.getId()));
        assertEquals(15, stockReservationService.availableQuantityForProduct(product.getId(), null));

        // Reserve 6 units via FEFO -> splits 4 from BATCH-EARLY + 2 from BATCH-LATE (skips expired lot)
        List<StockReservationEntity> reservations = stockReservationService.reserve(
                ReservationCommand.forPicking(product.getId(), 6, testUserId, warehouse.getId(), null, null)
        );
        assertEquals(2, reservations.size());
        assertEquals(earlierLot.inventoryItemId(), reservations.get(0).getInventoryItem().getId());
        assertEquals(4, reservations.get(0).getQuantity());
        assertEquals(ReservationStatus.ACTIVE, reservations.get(0).getStatus());
        assertEquals(BASE_TIME.plus(30, ChronoUnit.MINUTES), reservations.get(0).getExpiresAt());
        assertEquals(laterLot.inventoryItemId(), reservations.get(1).getInventoryItem().getId());
        assertEquals(2, reservations.get(1).getQuantity());

        // Release first reservation -> makes 4 units available again
        StockReservationEntity released = stockReservationService.release(reservations.get(0).getId());
        assertEquals(ReservationStatus.RELEASED, released.getStatus());
        assertNotNull(released.getClosedAt());
        assertEquals(0, inventoryItemRepository.findById(earlierLot.inventoryItemId()).orElseThrow().getReservedQuantity());

        // Double release rejected with 409
        ReservationConflictException doubleRelease = assertThrows(
                ReservationConflictException.class,
                () -> stockReservationService.release(released.getId())
        );
        assertEquals(409, doubleRelease.getHttpStatus());

        // Re-reserve the 4 released units (transfer type) and consume them in one transaction
        List<StockReservationEntity> reReserved = stockReservationService.reserve(
                ReservationCommand.forTransfer(product.getId(), 4, testUserId, warehouse.getId(), null)
        );
        assertEquals(1, reReserved.size());
        assertEquals(ReservationType.TRANSFER, reReserved.get(0).getReservationType());
        assertEquals(earlierLot.inventoryItemId(), reReserved.get(0).getInventoryItem().getId());

        StockReservationEntity consumed = stockReservationService.consume(reReserved.get(0).getId());
        assertEquals(ReservationStatus.CONSUMED, consumed.getStatus());
        assertEquals(BASE_TIME, consumed.getClosedAt());

        InventoryItemEntity depletedItem = inventoryItemRepository.findById(earlierLot.inventoryItemId()).orElseThrow();
        assertEquals(0, depletedItem.getQuantity());
        assertEquals(0, depletedItem.getReservedQuantity());
        assertEquals(InventoryStatus.DEPLETED, depletedItem.getStatus());
        assertEquals(0, stockReservationService.availableQuantity(depletedItem));

        // Initial shelf load was 15 * 2.5 kg = 37.5 kg, after consuming 4 units -> 11 * 2.5 kg = 27.5 kg
        ShelfEntity updatedShelf = shelfRepository.findById(shelf.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("27.500").compareTo(updatedShelf.getCurrentWeightKg()));
        assertEquals(0, new BigDecimal("0.550000").compareTo(updatedShelf.getCurrentVolumeM3()));

        // Consuming already consumed reservation or missing reservation
        assertThrows(ReservationConflictException.class, () -> stockReservationService.consume(consumed.getId()));
        assertThrows(ResourceNotFoundException.class, () -> stockReservationService.release(99999L));
        assertThrows(ResourceNotFoundException.class, () -> stockReservationService.reserve(
                ReservationCommand.forPicking(99999L, 1, testUserId, null, null, null)
        ));
        assertThrows(InsufficientStockException.class, () -> stockReservationService.reserve(
                ReservationCommand.forPicking(product.getId(), 50, testUserId, warehouse.getId(), null, null)
        ));
        assertThrows(IllegalArgumentException.class, () ->
                ReservationCommand.forPicking(product.getId(), 0, testUserId, null, null, null)
        );
    }

    @Test
    void concurrentReservationsAndStaleVersionsTriggerConflict() throws Exception {
        InboundResponse inbound = inventoryService.receive(new InboundRequest(
                product.getId(), shelf.getId(), 0, "BATCH-CONC", LocalDate.of(2027, 12, 31), 10
        ));

        // 1. Direct optimistic locking check on InventoryItemEntity (@Version)
        InventoryItemEntity staleItem = inventoryItemRepository.findById(inbound.inventoryItemId()).orElseThrow();
        InventoryItemEntity freshItem = inventoryItemRepository.findById(inbound.inventoryItemId()).orElseThrow();
        freshItem.setReservedQuantity(2);
        inventoryItemRepository.saveAndFlush(freshItem);

        staleItem.setReservedQuantity(3);
        assertThrows(OptimisticLockingFailureException.class, () -> inventoryItemRepository.saveAndFlush(staleItem));

        // Reset reserved quantity for concurrent thread race test
        InventoryItemEntity resetItem = inventoryItemRepository.findById(inbound.inventoryItemId()).orElseThrow();
        resetItem.setReservedQuantity(0);
        inventoryItemRepository.saveAndFlush(resetItem);

        // 2. Direct optimistic locking check on ShelfEntity (@Version)
        ShelfEntity staleShelf = shelfRepository.findById(shelf.getId()).orElseThrow();
        ShelfEntity freshShelf = shelfRepository.findById(shelf.getId()).orElseThrow();
        freshShelf.setMaxWeightKg(new BigDecimal("210.000"));
        shelfRepository.saveAndFlush(freshShelf);

        staleShelf.setMaxWeightKg(new BigDecimal("220.000"));
        assertThrows(OptimisticLockingFailureException.class, () -> shelfRepository.saveAndFlush(staleShelf));

        // 3. Real concurrent reservation race: 2 threads both request 7 out of 10 units simultaneously
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger conflictFailures = new AtomicInteger();

        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    try {
                        start.await();
                        stockReservationService.reserve(
                                ReservationCommand.forPicking(product.getId(), 7, testUserId, warehouse.getId(), null, null)
                        );
                        successes.incrementAndGet();
                    } catch (OptimisticLockingFailureException
                             | OptimisticLockException
                             | InsufficientStockException
                             | ReservationConflictException expectedConflict) {
                        conflictFailures.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }));
            }

            ready.await();
            start.countDown();
            for (Future<?> future : futures) {
                future.get();
            }
        } finally {
            executor.shutdownNow();
        }

        assertEquals(1, successes.get());
        assertEquals(1, conflictFailures.get());

        InventoryItemEntity afterRace = inventoryItemRepository.findById(inbound.inventoryItemId()).orElseThrow();
        assertEquals(7, afterRace.getReservedQuantity());
        assertEquals(3, stockReservationService.availableQuantity(afterRace));
    }

    @Test
    void timeoutJobExpiresOverdueReservationsUsingFixedClock() {
        InboundResponse inbound = inventoryService.receive(new InboundRequest(
                product.getId(), shelf.getId(), 0, "BATCH-TTL", LocalDate.of(2027, 12, 31), 10
        ));

        List<StockReservationEntity> created = stockReservationService.reserve(
                ReservationCommand.forPicking(product.getId(), 6, testUserId, warehouse.getId(), null, null)
        );
        Long reservationId = created.get(0).getId();
        assertEquals(BASE_TIME.plus(30, ChronoUnit.MINUTES), created.get(0).getExpiresAt());
        assertEquals(6, inventoryItemRepository.findById(inbound.inventoryItemId()).orElseThrow().getReservedQuantity());

        // 29 minutes later -> still active, job releases 0
        mutableClock.setInstant(BASE_TIME.plus(29, ChronoUnit.MINUTES));
        assertEquals(0, reservationTimeoutJob.releaseExpiredReservations());
        assertEquals(ReservationStatus.ACTIVE, stockReservationRepository.findById(reservationId).orElseThrow().getStatus());
        assertEquals(6, inventoryItemRepository.findById(inbound.inventoryItemId()).orElseThrow().getReservedQuantity());

        // 31 minutes later -> overdue: consume is blocked with 409, timeout job marks EXPIRED and frees stock
        Instant expiredRunTime = BASE_TIME.plus(31, ChronoUnit.MINUTES);
        mutableClock.setInstant(expiredRunTime);

        ReservationConflictException expiredConsume = assertThrows(
                ReservationConflictException.class,
                () -> stockReservationService.consume(reservationId)
        );
        assertEquals(409, expiredConsume.getHttpStatus());

        assertEquals(1, reservationTimeoutJob.releaseExpiredReservations());
        StockReservationEntity expiredEntity = stockReservationRepository.findById(reservationId).orElseThrow();
        assertEquals(ReservationStatus.EXPIRED, expiredEntity.getStatus());
        assertEquals(expiredRunTime, expiredEntity.getClosedAt());

        InventoryItemEntity freedItem = inventoryItemRepository.findById(inbound.inventoryItemId()).orElseThrow();
        assertEquals(0, freedItem.getReservedQuantity());
        assertEquals(10, stockReservationService.availableQuantity(freedItem));

        // Freed stock can be fully reserved again
        List<StockReservationEntity> reReserved = stockReservationService.reserve(
                ReservationCommand.forPicking(product.getId(), 10, testUserId, warehouse.getId(), null, null)
        );
        assertEquals(1, reReserved.size());
        assertEquals(10, reReserved.get(0).getQuantity());
        assertTrue(reReserved.get(0).getExpiresAt().equals(expiredRunTime.plus(30, ChronoUnit.MINUTES)));
    }
}
