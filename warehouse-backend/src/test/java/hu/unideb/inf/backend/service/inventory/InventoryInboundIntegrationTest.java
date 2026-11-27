package hu.unideb.inf.backend.service.inventory;

import hu.unideb.inf.backend.model.entity.InventoryItemEntity;
import hu.unideb.inf.backend.model.entity.ProductEntity;
import hu.unideb.inf.backend.model.entity.ShelfEntity;
import hu.unideb.inf.backend.model.entity.WarehouseEntity;
import hu.unideb.inf.backend.model.entity.ZoneEntity;
import hu.unideb.inf.backend.repository.InventoryItemRepository;
import hu.unideb.inf.backend.repository.LotRepository;
import hu.unideb.inf.backend.repository.ProductRepository;
import hu.unideb.inf.backend.repository.ShelfRepository;
import hu.unideb.inf.backend.repository.WarehouseRepository;
import hu.unideb.inf.backend.repository.ZoneRepository;
import hu.unideb.inf.backend.service.suggestion.ShelfSuggestionService;
import hu.unideb.inf.core.dto.InboundRequest;
import hu.unideb.inf.core.dto.InboundResponse;
import hu.unideb.inf.core.dto.PageResponse;
import hu.unideb.inf.core.dto.ShelfSuggestionResponse;
import hu.unideb.inf.core.enums.InventoryStatus;
import hu.unideb.inf.core.enums.StorageType;
import hu.unideb.inf.core.enums.ZoneType;
import hu.unideb.inf.core.exception.ResourceNotFoundException;
import hu.unideb.inf.core.exception.ShelfCapacityExceededException;
import hu.unideb.inf.core.exception.ZoneMismatchException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class InventoryInboundIntegrationTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private ShelfSuggestionService shelfSuggestionService;

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

    private WarehouseEntity warehouse;
    private ZoneEntity normalZone;
    private ZoneEntity chilledZone;
    private ShelfEntity normalShelf;
    private ShelfEntity chilledShelf;
    private ProductEntity normalProduct;
    private ProductEntity chilledProduct;

    @BeforeEach
    void setUp() {
        cleanUp();
        warehouse = warehouseRepository.save(new WarehouseEntity("WH-INB-01", "Debrecen", "Kassai út 26.", true));
        normalZone = zoneRepository.save(new ZoneEntity(warehouse, "Z-NORM", "Normál", ZoneType.NORMAL, true));
        chilledZone = zoneRepository.save(new ZoneEntity(warehouse, "Z-CHILL", "Hűtött", ZoneType.CHILLED, true));
        normalShelf = shelfRepository.save(new ShelfEntity(
                normalZone, "S-INB-01", StorageType.RACK, 3,
                new BigDecimal("100.000"), new BigDecimal("2.000000"), true
        ));
        chilledShelf = shelfRepository.save(new ShelfEntity(
                chilledZone, "S-INB-02", StorageType.RACK, 2,
                new BigDecimal("100.000"), new BigDecimal("2.000000"), true
        ));
        normalProduct = productRepository.save(new ProductEntity(
                "SKU-INB-01", "5991000000001", "Csavar doboz", null,
                new BigDecimal("5.000"), new BigDecimal("0.100000"), ZoneType.NORMAL, 5, true
        ));
        chilledProduct = productRepository.save(new ProductEntity(
                "SKU-INB-02", "5991000000002", "Tej 1L", null,
                new BigDecimal("1.000"), new BigDecimal("0.010000"), ZoneType.CHILLED, 10, true
        ));
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    private void cleanUp() {
        inventoryItemRepository.deleteAll();
        lotRepository.deleteAll();
        shelfRepository.deleteAll();
        zoneRepository.deleteAll();
        warehouseRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    void receivePersistsMergesSameLotAndTierAndSupportsInventoryQueries() {
        InboundResponse first = inventoryService.receive(new InboundRequest(
                normalProduct.getId(), normalShelf.getId(), null, " BATCH-A1 ", null, 4
        ));
        assertNotNull(first.inventoryItemId());
        assertEquals(0, first.tierLevel());
        assertEquals("BATCH-A1", first.batchNumber());
        assertEquals(4, first.quantity());
        assertEquals(0, new BigDecimal("20.000").compareTo(first.currentShelfWeightKg()));
        assertEquals(0, new BigDecimal("0.400000").compareTo(first.currentShelfVolumeM3()));
        assertEquals(InventoryStatus.STORED, first.status());

        LocalDate expiry = LocalDate.of(2027, 12, 31);
        InboundResponse merged = inventoryService.receive(new InboundRequest(
                normalProduct.getId(), normalShelf.getId(), 0, "BATCH-A1", expiry, 6
        ));
        assertEquals(first.inventoryItemId(), merged.inventoryItemId());
        assertEquals(10, merged.quantity());
        assertEquals(expiry, merged.expirationDate());
        assertEquals(0, new BigDecimal("50.000").compareTo(merged.currentShelfWeightKg()));
        assertEquals(0, new BigDecimal("1.000000").compareTo(merged.currentShelfVolumeM3()));
        assertEquals(1, lotRepository.count());
        assertEquals(1, inventoryItemRepository.count());

        Long lotId = lotRepository.findByProductIdAndLotNumber(normalProduct.getId(), "BATCH-A1")
                .orElseThrow().getId();
        PageResponse<InboundResponse> allPaged = inventoryService.findAll(null, null, null, null, PageRequest.of(0, 10));
        assertEquals(1, allPaged.totalElements());
        assertEquals(1, inventoryService.findAll(normalShelf.getId(), null, null, null, Pageable.unpaged()).content().size());
        assertEquals(1, inventoryService.findAll(null, normalProduct.getId(), null, null, PageRequest.of(0, 10)).content().size());
        assertEquals(1, inventoryService.findAll(null, null, lotId, null, PageRequest.of(0, 10)).content().size());
        assertEquals(1, inventoryService.findAll(null, null, lotId, InventoryStatus.STORED, PageRequest.of(0, 10)).content().size());
        assertEquals(0, inventoryService.findAll(null, null, null, InventoryStatus.BLOCKED, PageRequest.of(0, 10)).content().size());
    }

    @Test
    void receiveReactivatesDepletedInventoryItem() {
        InboundResponse initial = inventoryService.receive(new InboundRequest(
                normalProduct.getId(), normalShelf.getId(), 1, "BATCH-DEP", LocalDate.of(2027, 6, 1), 2
        ));
        InventoryItemEntity stored = inventoryItemRepository.findById(initial.inventoryItemId()).orElseThrow();
        Instant oldReceivedAt = Instant.parse("2025-01-01T00:00:00Z");
        stored.setStatus(InventoryStatus.DEPLETED);
        stored.setQuantity(0);
        stored.setReservedQuantity(0);
        stored.setReceivedAt(oldReceivedAt);
        inventoryItemRepository.saveAndFlush(stored);

        ShelfEntity reloadedShelf = shelfRepository.findById(normalShelf.getId()).orElseThrow();
        reloadedShelf.setCurrentWeightKg(BigDecimal.ZERO);
        reloadedShelf.setCurrentVolumeM3(BigDecimal.ZERO);
        shelfRepository.saveAndFlush(reloadedShelf);

        InboundResponse reactivated = inventoryService.receive(new InboundRequest(
                normalProduct.getId(), normalShelf.getId(), 1, "BATCH-DEP", LocalDate.of(2027, 6, 1), 3
        ));
        assertEquals(initial.inventoryItemId(), reactivated.inventoryItemId());
        assertEquals(InventoryStatus.STORED, reactivated.status());
        assertEquals(3, reactivated.quantity());
        assertTrue(reactivated.timestamp().isAfter(oldReceivedAt));
    }

    @Test
    void receiveRejectsOverCapacityAndRollsBackTransaction() {
        inventoryService.receive(new InboundRequest(
                normalProduct.getId(), normalShelf.getId(), 0, "BATCH-OK", null, 16
        ));

        ShelfCapacityExceededException ex = assertThrows(
                ShelfCapacityExceededException.class,
                () -> inventoryService.receive(new InboundRequest(
                        normalProduct.getId(), normalShelf.getId(), 0, "BATCH-ROLLBACK", null, 5
                ))
        );
        assertEquals(409, ex.getHttpStatus());
        assertEquals("S-INB-01", ex.getShelfCode());

        assertTrue(lotRepository.findByProductIdAndLotNumber(normalProduct.getId(), "BATCH-ROLLBACK").isEmpty());
        assertEquals(1, inventoryItemRepository.findByShelfId(normalShelf.getId()).size());
        ShelfEntity reloadedShelf = shelfRepository.findById(normalShelf.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("80.000").compareTo(reloadedShelf.getCurrentWeightKg()));
        assertEquals(0, new BigDecimal("1.600000").compareTo(reloadedShelf.getCurrentVolumeM3()));
    }

    @Test
    void receiveValidatesZoneMismatchAndMissingResources() {
        ZoneMismatchException mismatch = assertThrows(
                ZoneMismatchException.class,
                () -> inventoryService.receive(new InboundRequest(
                        chilledProduct.getId(), normalShelf.getId(), 0, "BATCH-COLD", null, 2
                ))
        );
        assertEquals(409, mismatch.getHttpStatus());

        assertThrows(ResourceNotFoundException.class, () -> inventoryService.receive(new InboundRequest(
                99999L, normalShelf.getId(), 0, "BATCH-X", null, 1
        )));
        assertThrows(ResourceNotFoundException.class, () -> inventoryService.receive(new InboundRequest(
                normalProduct.getId(), 99999L, 0, "BATCH-X", null, 1
        )));
        assertThrows(ResourceNotFoundException.class, () -> inventoryService.receive(new InboundRequest(
                normalProduct.getId(), normalShelf.getId(), 3, "BATCH-X", null, 1
        )));
        assertThrows(ResourceNotFoundException.class, () -> inventoryService.receive(new InboundRequest(
                normalProduct.getId(), normalShelf.getId(), -1, "BATCH-X", null, 1
        )));
    }

    @Test
    void shelfSuggestionServiceFiltersByZoneCapacityAndPrefersSameProduct() {
        ShelfEntity secondNormalShelf = shelfRepository.save(new ShelfEntity(
                normalZone, "S-INB-03", StorageType.RACK, 2,
                new BigDecimal("100.000"), new BigDecimal("2.000000"), true
        ));
        ShelfEntity inactiveNormalShelf = shelfRepository.save(new ShelfEntity(
                normalZone, "S-INB-04", StorageType.RACK, 2,
                new BigDecimal("100.000"), new BigDecimal("2.000000"), false
        ));
        assertNotNull(inactiveNormalShelf.getId());

        inventoryService.receive(new InboundRequest(
                normalProduct.getId(), secondNormalShelf.getId(), 0, "BATCH-SUG", null, 2
        ));

        List<ShelfSuggestionResponse> suggestions = shelfSuggestionService.suggest(
                normalProduct.getId(), 2, warehouse.getId(), null
        );
        assertEquals(2, suggestions.size());
        assertEquals("S-INB-03", suggestions.get(0).shelfCode());
        assertEquals(ZoneType.NORMAL, suggestions.get(0).zoneType());
        assertEquals(0, new BigDecimal("90.000").compareTo(suggestions.get(0).freeWeightKg()));

        List<ShelfSuggestionResponse> limited = shelfSuggestionService.suggest(
                normalProduct.getId(), 2, null, 1
        );
        assertEquals(1, limited.size());

        assertThrows(
                ResourceNotFoundException.class,
                () -> shelfSuggestionService.suggest(99999L, 2, null, 5)
        );
    }
}
