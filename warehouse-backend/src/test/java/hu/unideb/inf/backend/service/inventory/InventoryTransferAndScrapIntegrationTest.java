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
import hu.unideb.inf.core.dto.InboundRequest;
import hu.unideb.inf.core.dto.InboundResponse;
import hu.unideb.inf.core.dto.ScrapRequest;
import hu.unideb.inf.core.dto.TransferRequest;
import hu.unideb.inf.core.enums.InventoryStatus;
import hu.unideb.inf.core.enums.StorageType;
import hu.unideb.inf.core.enums.ZoneType;
import hu.unideb.inf.core.exception.InsufficientStockException;
import hu.unideb.inf.core.exception.InvalidTransferException;
import hu.unideb.inf.core.exception.ReservationConflictException;
import hu.unideb.inf.core.exception.ResourceNotFoundException;
import hu.unideb.inf.core.exception.ShelfCapacityExceededException;
import hu.unideb.inf.core.exception.ZoneMismatchException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
class InventoryTransferAndScrapIntegrationTest {

    @Autowired
    private InventoryService inventoryService;

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

    private ZoneEntity normalZone;
    private ShelfEntity sourceShelf;
    private ShelfEntity targetShelf;
    private ShelfEntity chilledShelf;
    private ProductEntity normalProduct;
    private ProductEntity chilledProduct;

    @BeforeEach
    void setUp() {
        cleanUp();
        WarehouseEntity warehouse = warehouseRepository.save(
                new WarehouseEntity("WH-TR-01", "Debrecen", "Kassai út 26.", true)
        );
        normalZone = zoneRepository.save(
                new ZoneEntity(warehouse, "Z-TR-NORM", "Normál", ZoneType.NORMAL, true)
        );
        ZoneEntity chilledZone = zoneRepository.save(
                new ZoneEntity(warehouse, "Z-TR-CHILL", "Hűtött", ZoneType.CHILLED, true)
        );
        sourceShelf = shelfRepository.save(new ShelfEntity(
                normalZone, "S-TR-01", StorageType.RACK, 3,
                new BigDecimal("100.000"), new BigDecimal("2.000000"), true
        ));
        targetShelf = shelfRepository.save(new ShelfEntity(
                normalZone, "S-TR-02", StorageType.RACK, 2,
                new BigDecimal("100.000"), new BigDecimal("2.000000"), true
        ));
        chilledShelf = shelfRepository.save(new ShelfEntity(
                chilledZone, "S-TR-03", StorageType.RACK, 2,
                new BigDecimal("100.000"), new BigDecimal("2.000000"), true
        ));
        normalProduct = productRepository.save(new ProductEntity(
                "SKU-TR-01", "5992000000001", "Csapágy", null,
                new BigDecimal("5.000"), new BigDecimal("0.100000"), ZoneType.NORMAL, 5, true
        ));
        chilledProduct = productRepository.save(new ProductEntity(
                "SKU-TR-02", "5992000000002", "Joghurt", null,
                new BigDecimal("2.000"), new BigDecimal("0.020000"), ZoneType.CHILLED, 5, true
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
    void transferMovesStockPreservesReceivedAtUpdatesLoadsAndDepletesEmptySource() {
        InboundResponse inbound = inventoryService.receive(new InboundRequest(
                normalProduct.getId(), sourceShelf.getId(), 0, "BATCH-TR-1", LocalDate.of(2027, 10, 1), 10
        ));
        Instant originalReceivedAt = Instant.parse("2025-05-01T08:00:00Z");
        InventoryItemEntity sourceEntity = inventoryItemRepository.findById(inbound.inventoryItemId()).orElseThrow();
        sourceEntity.setReceivedAt(originalReceivedAt);
        inventoryItemRepository.saveAndFlush(sourceEntity);

        InboundResponse partial = inventoryService.transfer(new TransferRequest(
                inbound.inventoryItemId(), sourceShelf.getId(), 0, targetShelf.getId(), 1, 4, "Átrendezés"
        ));
        assertEquals(targetShelf.getId(), partial.shelfId());
        assertEquals(1, partial.tierLevel());
        assertEquals(4, partial.quantity());
        assertEquals(originalReceivedAt, partial.timestamp());
        assertEquals(0, new BigDecimal("20.000").compareTo(partial.currentShelfWeightKg()));
        assertEquals(0, new BigDecimal("0.400000").compareTo(partial.currentShelfVolumeM3()));

        ShelfEntity reloadedSource = shelfRepository.findById(sourceShelf.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("30.000").compareTo(reloadedSource.getCurrentWeightKg()));
        assertEquals(0, new BigDecimal("0.600000").compareTo(reloadedSource.getCurrentVolumeM3()));

        InboundResponse merged = inventoryService.transfer(new TransferRequest(
                inbound.inventoryItemId(), sourceShelf.getId(), 0, targetShelf.getId(), 1, 6, "Teljes áthelyezés"
        ));
        assertEquals(partial.inventoryItemId(), merged.inventoryItemId());
        assertEquals(10, merged.quantity());
        assertEquals(originalReceivedAt, merged.timestamp());
        assertEquals(0, new BigDecimal("50.000").compareTo(merged.currentShelfWeightKg()));

        InventoryItemEntity depletedSource = inventoryItemRepository.findById(inbound.inventoryItemId()).orElseThrow();
        assertEquals(0, depletedSource.getQuantity());
        assertEquals(InventoryStatus.DEPLETED, depletedSource.getStatus());
        ShelfEntity emptiedSourceShelf = shelfRepository.findById(sourceShelf.getId()).orElseThrow();
        assertEquals(0, BigDecimal.ZERO.compareTo(emptiedSourceShelf.getCurrentWeightKg()));

        InboundResponse reactivated = inventoryService.transfer(new TransferRequest(
                merged.inventoryItemId(), targetShelf.getId(), 1, sourceShelf.getId(), 0, 3, "Visszamozgatás"
        ));
        assertEquals(inbound.inventoryItemId(), reactivated.inventoryItemId());
        assertEquals(InventoryStatus.STORED, reactivated.status());
        assertEquals(3, reactivated.quantity());
        assertEquals(originalReceivedAt, reactivated.timestamp());
    }

    @Test
    void transferAcrossTiersOnSameShelfDoesNotDoubleCountShelfLoad() {
        InboundResponse inbound = inventoryService.receive(new InboundRequest(
                normalProduct.getId(), sourceShelf.getId(), 0, "BATCH-TIER", null, 18
        ));

        InboundResponse moved = inventoryService.transfer(new TransferRequest(
                inbound.inventoryItemId(), sourceShelf.getId(), 0, sourceShelf.getId(), 2, 5, "Szintcsere"
        ));
        assertEquals(sourceShelf.getId(), moved.shelfId());
        assertEquals(2, moved.tierLevel());
        assertEquals(5, moved.quantity());
        assertEquals(0, new BigDecimal("90.000").compareTo(moved.currentShelfWeightKg()));
        assertEquals(0, new BigDecimal("1.800000").compareTo(moved.currentShelfVolumeM3()));

        ShelfEntity reloaded = shelfRepository.findById(sourceShelf.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("90.000").compareTo(reloaded.getCurrentWeightKg()));
    }

    @Test
    void transferRejectsInsufficientStockReservedStockAndFullTargetWithRollback() {
        InboundResponse source = inventoryService.receive(new InboundRequest(
                normalProduct.getId(), sourceShelf.getId(), 0, "BATCH-SRC", null, 10
        ));
        inventoryService.receive(new InboundRequest(
                normalProduct.getId(), targetShelf.getId(), 0, "BATCH-FILL", null, 18
        ));

        InventoryItemEntity sourceItem = inventoryItemRepository.findById(source.inventoryItemId()).orElseThrow();
        sourceItem.setReservedQuantity(4);
        inventoryItemRepository.saveAndFlush(sourceItem);

        InsufficientStockException insufficient = assertThrows(
                InsufficientStockException.class,
                () -> inventoryService.transfer(new TransferRequest(
                        source.inventoryItemId(), sourceShelf.getId(), 0, targetShelf.getId(), 1, 7, "Túl sok"
                ))
        );
        assertEquals(409, insufficient.getHttpStatus());
        assertEquals(6, insufficient.getAvailableQuantity());

        ShelfCapacityExceededException full = assertThrows(
                ShelfCapacityExceededException.class,
                () -> inventoryService.transfer(new TransferRequest(
                        source.inventoryItemId(), sourceShelf.getId(), 0, targetShelf.getId(), 1, 5, "Teli cél"
                ))
        );
        assertEquals(409, full.getHttpStatus());
        assertEquals("S-TR-02", full.getShelfCode());

        InventoryItemEntity unchangedSource = inventoryItemRepository.findById(source.inventoryItemId()).orElseThrow();
        assertEquals(10, unchangedSource.getQuantity());
        ShelfEntity unchangedSourceShelf = shelfRepository.findById(sourceShelf.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("50.000").compareTo(unchangedSourceShelf.getCurrentWeightKg()));
        ShelfEntity unchangedTargetShelf = shelfRepository.findById(targetShelf.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("90.000").compareTo(unchangedTargetShelf.getCurrentWeightKg()));
    }

    @Test
    void transferValidatesLocationMismatchSameLocationZoneMismatchAndMissingResources() {
        InboundResponse chilledItem = inventoryService.receive(new InboundRequest(
                chilledProduct.getId(), chilledShelf.getId(), 0, "BATCH-COLD-TR", null, 5
        ));
        ShelfEntity inactiveShelf = shelfRepository.save(new ShelfEntity(
                normalZone, "S-TR-INACT", StorageType.RACK, 2,
                new BigDecimal("100.000"), new BigDecimal("2.000000"), false
        ));

        InvalidTransferException sameLocation = assertThrows(
                InvalidTransferException.class,
                () -> inventoryService.transfer(new TransferRequest(
                        chilledItem.inventoryItemId(), chilledShelf.getId(), 0, chilledShelf.getId(), 0, 1, null
                ))
        );
        assertEquals(400, sameLocation.getHttpStatus());

        InvalidTransferException wrongSource = assertThrows(
                InvalidTransferException.class,
                () -> inventoryService.transfer(new TransferRequest(
                        chilledItem.inventoryItemId(), sourceShelf.getId(), 0, targetShelf.getId(), 0, 1, null
                ))
        );
        assertEquals(409, wrongSource.getHttpStatus());

        ZoneMismatchException zoneMismatch = assertThrows(
                ZoneMismatchException.class,
                () -> inventoryService.transfer(new TransferRequest(
                        chilledItem.inventoryItemId(), chilledShelf.getId(), 0, targetShelf.getId(), 0, 2, null
                ))
        );
        assertEquals(409, zoneMismatch.getHttpStatus());

        assertThrows(ResourceNotFoundException.class, () -> inventoryService.transfer(new TransferRequest(
                99999L, chilledShelf.getId(), 0, targetShelf.getId(), 0, 1, null
        )));
        assertThrows(ResourceNotFoundException.class, () -> inventoryService.transfer(new TransferRequest(
                chilledItem.inventoryItemId(), chilledShelf.getId(), 0, 99999L, 0, 1, null
        )));
        assertThrows(ResourceNotFoundException.class, () -> inventoryService.transfer(new TransferRequest(
                chilledItem.inventoryItemId(), chilledShelf.getId(), 0, inactiveShelf.getId(), 0, 1, null
        )));
        assertThrows(ResourceNotFoundException.class, () -> inventoryService.transfer(new TransferRequest(
                chilledItem.inventoryItemId(), chilledShelf.getId(), 0, chilledShelf.getId(), 5, 1, null
        )));
    }

    @Test
    void scrapDeductsStockUpdatesShelfLoadDepletesWhenEmptyAndBlocksReservedStock() {
        InboundResponse inbound = inventoryService.receive(new InboundRequest(
                normalProduct.getId(), sourceShelf.getId(), 0, "BATCH-SCRAP", null, 10
        ));

        InboundResponse partialScrap = inventoryService.scrap(new ScrapRequest(
                inbound.inventoryItemId(), 4, "Sérült csomagolás"
        ));
        assertEquals(6, partialScrap.quantity());
        assertEquals(InventoryStatus.STORED, partialScrap.status());
        assertEquals(0, new BigDecimal("30.000").compareTo(partialScrap.currentShelfWeightKg()));
        assertEquals(0, new BigDecimal("0.600000").compareTo(partialScrap.currentShelfVolumeM3()));

        InventoryItemEntity item = inventoryItemRepository.findById(inbound.inventoryItemId()).orElseThrow();
        item.setReservedQuantity(4);
        inventoryItemRepository.saveAndFlush(item);

        ReservationConflictException reservedEx = assertThrows(
                ReservationConflictException.class,
                () -> inventoryService.scrap(new ScrapRequest(inbound.inventoryItemId(), 3, "Törött"))
        );
        assertEquals(409, reservedEx.getHttpStatus());
        assertEquals(2, reservedEx.getAvailableQuantity());

        InsufficientStockException tooManyEx = assertThrows(
                InsufficientStockException.class,
                () -> inventoryService.scrap(new ScrapRequest(inbound.inventoryItemId(), 10, "Túl sok"))
        );
        assertEquals(409, tooManyEx.getHttpStatus());

        InboundResponse scrapUnreserved = inventoryService.scrap(new ScrapRequest(
                inbound.inventoryItemId(), 2, "Sérült"
        ));
        assertEquals(4, scrapUnreserved.quantity());
        assertEquals(0, new BigDecimal("20.000").compareTo(scrapUnreserved.currentShelfWeightKg()));

        InventoryItemEntity afterUnreserved = inventoryItemRepository.findById(inbound.inventoryItemId()).orElseThrow();
        afterUnreserved.setReservedQuantity(0);
        inventoryItemRepository.saveAndFlush(afterUnreserved);

        InboundResponse depletedScrap = inventoryService.scrap(new ScrapRequest(
                inbound.inventoryItemId(), 4, "Lejárt tétel"
        ));
        assertEquals(0, depletedScrap.quantity());
        assertEquals(InventoryStatus.DEPLETED, depletedScrap.status());
        assertEquals(0, BigDecimal.ZERO.compareTo(depletedScrap.currentShelfWeightKg()));
        assertEquals(0, BigDecimal.ZERO.compareTo(depletedScrap.currentShelfVolumeM3()));

        assertThrows(
                InsufficientStockException.class,
                () -> inventoryService.scrap(new ScrapRequest(inbound.inventoryItemId(), 1, "Üres tétel"))
        );
        assertThrows(
                ResourceNotFoundException.class,
                () -> inventoryService.scrap(new ScrapRequest(99999L, 1, "Hiányzó tétel"))
        );
    }
}
