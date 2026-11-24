package hu.unideb.inf.backend.repository;

import hu.unideb.inf.backend.model.entity.ProductEntity;
import hu.unideb.inf.backend.model.entity.ShelfEntity;
import hu.unideb.inf.backend.model.entity.WarehouseEntity;
import hu.unideb.inf.backend.model.entity.ZoneEntity;
import hu.unideb.inf.backend.service.capacity.ItemSize;
import hu.unideb.inf.backend.service.capacity.ShelfLoad;
import hu.unideb.inf.core.enums.StorageType;
import hu.unideb.inf.core.enums.ZoneType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
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
class ProductRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ShelfRepository shelfRepository;

    @Autowired
    private ZoneRepository zoneRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Test
    @DisplayName("Termék mentése, lekérdezése és toItemSize konverzió ellenőrzése")
    void persistAndQueryProductAndVerifyItemSize() {
        ProductEntity product = new ProductEntity(
                "SKU-MILK-01",
                "5991234567890",
                "Tej 2.8% 1L",
                "Pasztőrözött dobozos tej",
                new BigDecimal("1.030"),
                new BigDecimal("0.001050"),
                ZoneType.CHILLED,
                20,
                true
        );

        ProductEntity saved = productRepository.saveAndFlush(product);
        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
        assertEquals(0L, saved.getVersion());

        Optional<ProductEntity> bySku = productRepository.findBySku("SKU-MILK-01");
        assertTrue(bySku.isPresent());
        assertEquals("Tej 2.8% 1L", bySku.get().getName());
        assertTrue(productRepository.existsBySku("SKU-MILK-01"));
        assertFalse(productRepository.existsBySku("NON_EXISTING"));

        Optional<ProductEntity> byBarcode = productRepository.findByBarcode("5991234567890");
        assertTrue(byBarcode.isPresent());
        assertEquals("SKU-MILK-01", byBarcode.get().getSku());
        assertTrue(productRepository.existsByBarcode("5991234567890"));
        assertFalse(productRepository.existsByBarcode("0000000000000"));

        List<ProductEntity> chilledProducts = productRepository.findByRequiredZoneType(ZoneType.CHILLED);
        assertEquals(1, chilledProducts.size());

        ItemSize itemSize = saved.toItemSize();
        assertEquals(new BigDecimal("1.030"), itemSize.unitWeight());
        assertEquals(new BigDecimal("0.001050"), itemSize.unitVolume());
    }

    @Test
    @DisplayName("SKU és vonalkód egyediség ellenőrzése")
    void uniqueSkuAndBarcodeConstraints() {
        ProductEntity p1 = new ProductEntity(
                "SKU-UNIQUE-01",
                "BARCODE-001",
                "Termék 1",
                null,
                new BigDecimal("2.500"),
                new BigDecimal("0.005000"),
                ZoneType.NORMAL,
                10,
                true
        );
        productRepository.saveAndFlush(p1);

        ProductEntity duplicateSku = new ProductEntity(
                "SKU-UNIQUE-01",
                "BARCODE-002",
                "Termék duplikált SKU-val",
                null,
                new BigDecimal("1.000"),
                new BigDecimal("0.002000"),
                ZoneType.NORMAL,
                5,
                true
        );
        assertThrows(DataIntegrityViolationException.class, () -> {
            productRepository.saveAndFlush(duplicateSku);
        });

        entityManager.clear();

        ProductEntity duplicateBarcode = new ProductEntity(
                "SKU-OTHER-02",
                "BARCODE-001",
                "Termék duplikált vonalkóddal",
                null,
                new BigDecimal("1.000"),
                new BigDecimal("0.002000"),
                ZoneType.NORMAL,
                5,
                true
        );
        assertThrows(DataIntegrityViolationException.class, () -> {
            productRepository.saveAndFlush(duplicateBarcode);
        });
    }

    @Test
    @DisplayName("Termék és Polc @Version mezőjének frissülése módosításkor")
    void optimisticLockingVersionIncrementsOnUpdate() {
        ProductEntity product = new ProductEntity(
                "SKU-VER-01",
                "BAR-VER-01",
                "Verziózott termék",
                "Első leírás",
                new BigDecimal("1.000"),
                new BigDecimal("0.001000"),
                ZoneType.NORMAL,
                5,
                true
        );
        ProductEntity savedProduct = productRepository.saveAndFlush(product);
        assertEquals(0L, savedProduct.getVersion());

        savedProduct.setName("Verziózott termék frissítve");
        ProductEntity updatedProduct = productRepository.saveAndFlush(savedProduct);
        assertEquals(1L, updatedProduct.getVersion());

        WarehouseEntity wh = new WarehouseEntity("WH-V", "Raktár V", null, true);
        ZoneEntity zone = new ZoneEntity(wh, "Z-V", "Zóna V", ZoneType.NORMAL, true);
        wh.addZone(zone);
        warehouseRepository.saveAndFlush(wh);

        ShelfEntity shelf = new ShelfEntity(
                zone,
                "SH-VER-01",
                StorageType.RACK,
                3,
                new BigDecimal("500.000"),
                new BigDecimal("2.000000"),
                true
        );
        ShelfEntity savedShelf = shelfRepository.saveAndFlush(shelf);
        assertEquals(0L, savedShelf.getVersion());

        ShelfLoad initialLoad = savedShelf.toShelfLoad();
        assertEquals(BigDecimal.ZERO, initialLoad.currentWeight());

        savedShelf.setCurrentWeightKg(new BigDecimal("150.000"));
        savedShelf.setCurrentVolumeM3(new BigDecimal("0.500000"));
        ShelfEntity updatedShelf = shelfRepository.saveAndFlush(savedShelf);
        assertEquals(1L, updatedShelf.getVersion());

        ShelfLoad updatedLoad = updatedShelf.toShelfLoad();
        assertEquals(new BigDecimal("150.000"), updatedLoad.currentWeight());
        assertEquals(new BigDecimal("0.500000"), updatedLoad.currentVolume());
    }

    @Test
    @DisplayName("Termék entitás segédmetódusok és aktív szűrés")
    void helperMethodsAndActiveFiltering() {
        ProductEntity pActive = new ProductEntity(
                "SKU-ACT",
                "BAR-ACT",
                "Aktív termék",
                null,
                new BigDecimal("1.000"),
                new BigDecimal("0.001000"),
                ZoneType.NORMAL,
                0,
                true
        );
        ProductEntity pInactive = new ProductEntity(
                "SKU-INACT",
                "BAR-INACT",
                "Inaktív termék",
                null,
                new BigDecimal("1.000"),
                new BigDecimal("0.001000"),
                ZoneType.NORMAL,
                0,
                false
        );
        productRepository.saveAllAndFlush(List.of(pActive, pInactive));

        List<ProductEntity> activeProducts = productRepository.findAllByActiveTrue();
        assertTrue(activeProducts.stream().anyMatch(p -> "SKU-ACT".equals(p.getSku())));
        assertFalse(activeProducts.stream().anyMatch(p -> "SKU-INACT".equals(p.getSku())));

        assertNotNull(pActive.toString());
        assertEquals(pActive, pActive);
        assertFalse(pActive.equals(null));
        assertFalse(pActive.equals("string"));
    }
}
