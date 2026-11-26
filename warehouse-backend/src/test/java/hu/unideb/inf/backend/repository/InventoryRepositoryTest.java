package hu.unideb.inf.backend.repository;

import hu.unideb.inf.backend.model.entity.InventoryItemEntity;
import hu.unideb.inf.backend.model.entity.LotEntity;
import hu.unideb.inf.backend.model.entity.ProductEntity;
import hu.unideb.inf.backend.model.entity.ShelfEntity;
import hu.unideb.inf.backend.model.entity.StockReservationEntity;
import hu.unideb.inf.backend.model.entity.WarehouseEntity;
import hu.unideb.inf.backend.model.entity.ZoneEntity;
import hu.unideb.inf.backend.service.picking.StockLot;
import hu.unideb.inf.core.enums.InventoryStatus;
import hu.unideb.inf.core.enums.ReservationStatus;
import hu.unideb.inf.core.enums.ReservationType;
import hu.unideb.inf.core.enums.StorageType;
import hu.unideb.inf.core.enums.ZoneType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class InventoryRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private ZoneRepository zoneRepository;

    @Autowired
    private ShelfRepository shelfRepository;

    @Autowired
    private LotRepository lotRepository;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private StockReservationRepository stockReservationRepository;

    private ProductEntity product;
    private ShelfEntity shelf;
    private Long testUserId;

    @BeforeEach
    void setUp() {
        entityManager.getEntityManager().createNativeQuery(
                "INSERT INTO users (username, password_hash, full_name, role, active, version, created_at, updated_at) " +
                        "VALUES ('worker_c4', 'hash123', 'Worker User', 'WORKER', true, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
        ).executeUpdate();

        Number id = (Number) entityManager.getEntityManager().createNativeQuery(
                "SELECT id FROM users WHERE username = 'worker_c4'"
        ).getSingleResult();
        testUserId = id.longValue();

        product = new ProductEntity(
                "SKU-INV-01",
                "BAR-INV-01",
                "Készlet Termék",
                "Leírás",
                new BigDecimal("1.500"),
                new BigDecimal("0.002000"),
                ZoneType.NORMAL,
                10,
                true
        );
        product = productRepository.saveAndFlush(product);

        WarehouseEntity wh = new WarehouseEntity("WH-INV", "Raktár Inv", "Cím", true);
        ZoneEntity zone = new ZoneEntity(wh, "Z-INV", "Zóna Inv", ZoneType.NORMAL, true);
        shelf = new ShelfEntity(zone, "SH-INV-01", StorageType.RACK, 3, new BigDecimal("1000.000"), new BigDecimal("5.000000"), true);
        zone.addShelf(shelf);
        wh.addZone(zone);
        warehouseRepository.saveAndFlush(wh);
    }

    @Test
    @DisplayName("C4.6 Sarzs és készlettétel mentése, lekérdezése és toStockLot konverzió")
    void persistAndQueryLotAndInventoryItem() {
        LotEntity lot = new LotEntity(product, "LOT-2026-A1", LocalDate.of(2026, 12, 31));
        LotEntity savedLot = lotRepository.saveAndFlush(lot);

        assertNotNull(savedLot.getId());
        assertNotNull(savedLot.getCreatedAt());
        assertEquals("LOT-2026-A1", savedLot.getLotNumber());
        assertTrue(lotRepository.existsByProductIdAndLotNumber(product.getId(), "LOT-2026-A1"));

        Instant receivedAt = Instant.now().minus(2, ChronoUnit.DAYS);
        InventoryItemEntity item = new InventoryItemEntity(
                shelf,
                savedLot,
                1,
                100,
                20,
                InventoryStatus.STORED,
                receivedAt
        );
        InventoryItemEntity savedItem = inventoryItemRepository.saveAndFlush(item);

        assertNotNull(savedItem.getId());
        assertEquals(0L, savedItem.getVersion());
        assertEquals(80, savedItem.getAvailableQuantity());

        StockLot stockLot = savedItem.toStockLot();
        assertEquals(savedItem.getId(), stockLot.inventoryItemId());
        assertEquals("SH-INV-01", stockLot.shelfCode());
        assertEquals("LOT-2026-A1", stockLot.batchNumber());
        assertEquals(80, stockLot.availableQuantity());
        assertEquals(LocalDate.of(2026, 12, 31), stockLot.expirationDate());

        // Készlet elérhetőség egyéb státuszoknál
        savedItem.setStatus(InventoryStatus.BLOCKED);
        assertEquals(0, savedItem.getAvailableQuantity());
        savedItem.setStatus(InventoryStatus.DEPLETED);
        assertEquals(0, savedItem.getAvailableQuantity());
        savedItem.setStatus(InventoryStatus.STORED);

        // Keresőmetódusok ellenőrzése
        Optional<InventoryItemEntity> byShelfLotTier = inventoryItemRepository.findByShelfIdAndLotIdAndTierLevel(
                shelf.getId(), savedLot.getId(), 1
        );
        assertTrue(byShelfLotTier.isPresent());

        assertFalse(inventoryItemRepository.findByShelfId(shelf.getId()).isEmpty());
        assertFalse(inventoryItemRepository.findByLotId(savedLot.getId()).isEmpty());
        assertFalse(inventoryItemRepository.findByStatusAndLotId(InventoryStatus.STORED, savedLot.getId()).isEmpty());
        assertFalse(inventoryItemRepository.findByLotProductId(product.getId()).isEmpty());
        assertFalse(inventoryItemRepository.findByShelfZoneWarehouseId(shelf.getZone().getWarehouse().getId()).isEmpty());
    }

    @Test
    @DisplayName("C4.6 Készlettétel @Version növekedése optimista zároláskor")
    void inventoryItemVersionIncrementsOnUpdate() {
        LotEntity lot = lotRepository.saveAndFlush(new LotEntity(product, "LOT-VER-01", null));
        InventoryItemEntity item = new InventoryItemEntity(
                shelf, lot, 0, 50, 0, InventoryStatus.STORED, Instant.now()
        );
        InventoryItemEntity saved = inventoryItemRepository.saveAndFlush(item);
        assertEquals(0L, saved.getVersion());

        saved.setQuantity(60);
        InventoryItemEntity updated = inventoryItemRepository.saveAndFlush(saved);
        assertEquals(1L, updated.getVersion());
    }

    @Test
    @DisplayName("C4.6 Készletfoglalás és lejárt aktív foglalások lekérdezése")
    void stockReservationAndExpiredActiveQuery() {
        LotEntity lot = lotRepository.saveAndFlush(new LotEntity(product, "LOT-RES-01", null));
        InventoryItemEntity item = inventoryItemRepository.saveAndFlush(new InventoryItemEntity(
                shelf, lot, 0, 100, 30, InventoryStatus.STORED, Instant.now()
        ));

        Instant past = Instant.now().minus(10, ChronoUnit.MINUTES);
        Instant future = Instant.now().plus(20, ChronoUnit.MINUTES);

        StockReservationEntity expiredRes = new StockReservationEntity(
                item, null, null, ReservationType.PICKING, 10, ReservationStatus.ACTIVE, past, testUserId
        );
        StockReservationEntity activeRes = new StockReservationEntity(
                item, null, null, ReservationType.PICKING, 20, ReservationStatus.ACTIVE, future, testUserId
        );
        StockReservationEntity consumedRes = new StockReservationEntity(
                item, null, null, ReservationType.PICKING, 5, ReservationStatus.CONSUMED, past, testUserId
        );

        stockReservationRepository.saveAllAndFlush(List.of(expiredRes, activeRes, consumedRes));

        List<StockReservationEntity> expiredActive = stockReservationRepository.findByStatusAndExpiresAtBefore(
                ReservationStatus.ACTIVE, Instant.now()
        );
        assertEquals(1, expiredActive.size());
        assertEquals(expiredRes.getId(), expiredActive.get(0).getId());

        List<StockReservationEntity> byItem = stockReservationRepository.findByInventoryItemId(item.getId());
        assertEquals(3, byItem.size());

        List<StockReservationEntity> byItemActive = stockReservationRepository.findByInventoryItemIdAndStatus(
                item.getId(), ReservationStatus.ACTIVE
        );
        assertEquals(2, byItemActive.size());
    }

    @Test
    @DisplayName("C4.6 Egyediségi kényszerek sarzs és készlettétel szinten")
    void uniqueConstraintsOnLotAndInventoryItem() {
        LotEntity lot1 = new LotEntity(product, "LOT-DUP-01", null);
        lotRepository.saveAndFlush(lot1);

        LotEntity lotDuplicate = new LotEntity(product, "LOT-DUP-01", null);
        assertThrows(DataIntegrityViolationException.class, () -> {
            lotRepository.saveAndFlush(lotDuplicate);
        });

        entityManager.clear();

        LotEntity reloadedLot = lotRepository.findByProductIdAndLotNumber(product.getId(), "LOT-DUP-01").orElseThrow();
        InventoryItemEntity item1 = new InventoryItemEntity(shelf, reloadedLot, 2, 20, 0, InventoryStatus.STORED, Instant.now());
        inventoryItemRepository.saveAndFlush(item1);

        InventoryItemEntity itemDuplicate = new InventoryItemEntity(shelf, reloadedLot, 2, 10, 0, InventoryStatus.STORED, Instant.now());
        assertThrows(DataIntegrityViolationException.class, () -> {
            inventoryItemRepository.saveAndFlush(itemDuplicate);
        });
    }
}
