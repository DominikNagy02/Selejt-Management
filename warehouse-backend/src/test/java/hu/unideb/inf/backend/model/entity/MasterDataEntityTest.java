package hu.unideb.inf.backend.model.entity;

import hu.unideb.inf.backend.service.capacity.ItemSize;
import hu.unideb.inf.backend.service.capacity.ShelfLoad;
import hu.unideb.inf.core.enums.StorageType;
import hu.unideb.inf.core.enums.ZoneType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class MasterDataEntityTest {

    @Test
    @DisplayName("WarehouseEntity getters, setters, lifecycle, equals, hashCode, toString tesztek")
    void testWarehouseEntity() {
        WarehouseEntity w1 = new WarehouseEntity();
        w1.setId(1L);
        w1.setCode("WH-01");
        w1.setName("Fő Raktár");
        w1.setAddress("Debrecen, Kassai út 26.");
        w1.setActive(true);
        w1.setZones(new ArrayList<>());
        Instant now = Instant.now();
        w1.setCreatedAt(now);
        w1.setUpdatedAt(now);

        assertEquals(1L, w1.getId());
        assertEquals("WH-01", w1.getCode());
        assertEquals("Fő Raktár", w1.getName());
        assertEquals("Debrecen, Kassai út 26.", w1.getAddress());
        assertTrue(w1.isActive());
        assertEquals(now, w1.getCreatedAt());
        assertEquals(now, w1.getUpdatedAt());
        assertNotNull(w1.getZones());

        WarehouseEntity w2 = new WarehouseEntity("WH-01", "Másik név", "Másik cím", true);
        WarehouseEntity w3 = new WarehouseEntity("WH-02", "Másik raktár", "Cím", true);

        assertEquals(w1, w1);
        assertEquals(w1, w2);
        assertNotEquals(w1, w3);
        assertNotEquals(w1, null);
        assertNotEquals(w1, "not a warehouse");
        assertEquals(w1.hashCode(), w2.hashCode());
        assertTrue(w1.toString().contains("WH-01"));

        w1.onCreate();
        assertNotNull(w1.getCreatedAt());
        assertNotNull(w1.getUpdatedAt());
        w1.onUpdate();

        ZoneEntity zone = new ZoneEntity();
        zone.setCode("Z-01");
        w1.addZone(zone);
        assertEquals(1, w1.getZones().size());
        assertEquals(w1, zone.getWarehouse());

        w1.removeZone(zone);
        assertEquals(0, w1.getZones().size());
        assertNull(zone.getWarehouse());
    }

    @Test
    @DisplayName("ZoneEntity getters, setters, lifecycle, equals, hashCode, toString tesztek")
    void testZoneEntity() {
        WarehouseEntity w1 = new WarehouseEntity("WH-01", "Warehouse 1", "Address 1", true);
        w1.setId(10L);

        WarehouseEntity w2 = new WarehouseEntity("WH-02", "Warehouse 2", "Address 2", true);
        w2.setId(20L);

        ZoneEntity z1 = new ZoneEntity();
        z1.setId(2L);
        z1.setWarehouse(w1);
        z1.setCode("Z-DRY-01");
        z1.setName("Szárazáru zóna");
        z1.setZoneType(ZoneType.NORMAL);
        z1.setActive(true);
        z1.setShelves(new ArrayList<>());
        Instant now = Instant.now();
        z1.setCreatedAt(now);
        z1.setUpdatedAt(now);

        assertEquals(2L, z1.getId());
        assertEquals(w1, z1.getWarehouse());
        assertEquals("Z-DRY-01", z1.getCode());
        assertEquals("Szárazáru zóna", z1.getName());
        assertEquals(ZoneType.NORMAL, z1.getZoneType());
        assertTrue(z1.isActive());
        assertEquals(now, z1.getCreatedAt());
        assertEquals(now, z1.getUpdatedAt());
        assertNotNull(z1.getShelves());

        ZoneEntity z2 = new ZoneEntity(w1, "Z-DRY-01", "Név 2", ZoneType.NORMAL, true);
        ZoneEntity zDiffCode = new ZoneEntity(w1, "Z-DRY-02", "Név 2", ZoneType.NORMAL, true);
        ZoneEntity zDiffWh = new ZoneEntity(w2, "Z-DRY-01", "Név 2", ZoneType.NORMAL, true);

        assertEquals(z1, z1);
        assertEquals(z1, z2);
        assertNotEquals(z1, zDiffCode);
        assertNotEquals(z1, zDiffWh);
        assertNotEquals(z1, null);
        assertNotEquals(z1, "not a zone");
        assertEquals(z1.hashCode(), z2.hashCode());
        assertTrue(z1.toString().contains("Z-DRY-01"));

        z1.onCreate();
        assertNotNull(z1.getCreatedAt());
        assertNotNull(z1.getUpdatedAt());
        z1.onUpdate();

        ShelfEntity shelf = new ShelfEntity();
        shelf.setCode("S-01");
        z1.addShelf(shelf);
        assertEquals(1, z1.getShelves().size());
        assertEquals(z1, shelf.getZone());

        z1.removeShelf(shelf);
        assertEquals(0, z1.getShelves().size());
        assertNull(shelf.getZone());
    }

    @Test
    @DisplayName("ShelfEntity getters, setters, lifecycle, equals, hashCode, toString, toShelfLoad tesztek")
    void testShelfEntity() {
        ZoneEntity zone = new ZoneEntity();
        zone.setCode("Z-01");

        ShelfEntity s1 = new ShelfEntity();
        s1.setId(3L);
        s1.setZone(zone);
        s1.setCode("S-01-A");
        s1.setMaxTiers(3);
        s1.setStorageType(StorageType.RACK);
        s1.setMaxWeightKg(new BigDecimal("1000.000"));
        s1.setMaxVolumeM3(new BigDecimal("5.000000"));
        s1.setCurrentWeightKg(new BigDecimal("200.000"));
        s1.setCurrentVolumeM3(new BigDecimal("1.000000"));
        s1.setVersion(0L);
        s1.setActive(true);
        Instant now = Instant.now();
        s1.setCreatedAt(now);
        s1.setUpdatedAt(now);

        assertEquals(3L, s1.getId());
        assertEquals(zone, s1.getZone());
        assertEquals("S-01-A", s1.getCode());
        assertEquals(3, s1.getMaxTiers());
        assertEquals(StorageType.RACK, s1.getStorageType());
        assertEquals(new BigDecimal("1000.000"), s1.getMaxWeightKg());
        assertEquals(new BigDecimal("5.000000"), s1.getMaxVolumeM3());
        assertEquals(new BigDecimal("200.000"), s1.getCurrentWeightKg());
        assertEquals(new BigDecimal("1.000000"), s1.getCurrentVolumeM3());
        assertEquals(0L, s1.getVersion());
        assertTrue(s1.isActive());
        assertEquals(now, s1.getCreatedAt());
        assertEquals(now, s1.getUpdatedAt());

        ShelfLoad load = s1.toShelfLoad();
        assertEquals("S-01-A", load.code());
        assertEquals(new BigDecimal("200.000"), load.currentWeight());
        assertEquals(new BigDecimal("1.000000"), load.currentVolume());
        assertEquals(new BigDecimal("1000.000"), load.maxWeight());
        assertEquals(new BigDecimal("5.000000"), load.maxVolume());

        ShelfEntity s2 = new ShelfEntity(zone, "S-01-A", StorageType.RACK, 3,
                new BigDecimal("1000.000"), new BigDecimal("5.000000"), true);
        ShelfEntity s3 = new ShelfEntity(zone, "S-01-B", StorageType.RACK, 3,
                new BigDecimal("1000.000"), new BigDecimal("5.000000"), true);

        assertEquals(s1, s1);
        assertEquals(s1, s2);
        assertNotEquals(s1, s3);
        assertNotEquals(s1, null);
        assertNotEquals(s1, "not a shelf");
        assertEquals(s1.hashCode(), s2.hashCode());
        assertTrue(s1.toString().contains("S-01-A"));

        s2.setCurrentWeightKg(null);
        s2.setCurrentVolumeM3(null);
        s2.onCreate();
        assertEquals(BigDecimal.ZERO, s2.getCurrentWeightKg());
        assertEquals(BigDecimal.ZERO, s2.getCurrentVolumeM3());
        assertNotNull(s2.getCreatedAt());
        assertNotNull(s2.getUpdatedAt());
        s2.onUpdate();

        // when current weight/volume already present in onCreate
        s1.onCreate();
        assertEquals(new BigDecimal("200.000"), s1.getCurrentWeightKg());
        assertEquals(new BigDecimal("1.000000"), s1.getCurrentVolumeM3());
    }

    @Test
    @DisplayName("ProductEntity getters, setters, lifecycle, equals, hashCode, toString, toItemSize tesztek")
    void testProductEntity() {
        ProductEntity p1 = new ProductEntity();
        p1.setId(4L);
        p1.setSku("SKU-001");
        p1.setBarcode("BAR-001");
        p1.setName("Alma");
        p1.setDescription("Friss alma");
        p1.setUnitWeightKg(new BigDecimal("0.200"));
        p1.setUnitVolumeM3(new BigDecimal("0.000300"));
        p1.setRequiredZoneType(ZoneType.NORMAL);
        p1.setMinStockLevel(15);
        p1.setActive(true);
        p1.setVersion(0L);
        Instant now = Instant.now();
        p1.setCreatedAt(now);
        p1.setUpdatedAt(now);

        assertEquals(4L, p1.getId());
        assertEquals("SKU-001", p1.getSku());
        assertEquals("BAR-001", p1.getBarcode());
        assertEquals("Alma", p1.getName());
        assertEquals("Friss alma", p1.getDescription());
        assertEquals(new BigDecimal("0.200"), p1.getUnitWeightKg());
        assertEquals(new BigDecimal("0.000300"), p1.getUnitVolumeM3());
        assertEquals(ZoneType.NORMAL, p1.getRequiredZoneType());
        assertEquals(15, p1.getMinStockLevel());
        assertTrue(p1.isActive());
        assertEquals(0L, p1.getVersion());
        assertEquals(now, p1.getCreatedAt());
        assertEquals(now, p1.getUpdatedAt());

        ItemSize itemSize = p1.toItemSize();
        assertEquals(new BigDecimal("0.200"), itemSize.unitWeight());
        assertEquals(new BigDecimal("0.000300"), itemSize.unitVolume());

        ProductEntity p2 = new ProductEntity("SKU-001", "BAR-002", "Név", "Leírás",
                new BigDecimal("0.500"), new BigDecimal("0.001000"), null, 10, true);
        ProductEntity p3 = new ProductEntity("SKU-002", "BAR-003", "Név", "Leírás",
                new BigDecimal("0.500"), new BigDecimal("0.001000"), ZoneType.CHILLED, 10, true);

        assertEquals(p1, p1);
        assertEquals(p1, p2);
        assertNotEquals(p1, p3);
        assertNotEquals(p1, null);
        assertNotEquals(p1, "not a product");
        assertEquals(p1.hashCode(), p2.hashCode());
        assertTrue(p1.toString().contains("SKU-001"));

        p2.setRequiredZoneType(null);
        p2.onCreate();
        assertEquals(ZoneType.NORMAL, p2.getRequiredZoneType());
        assertNotNull(p2.getCreatedAt());
        assertNotNull(p2.getUpdatedAt());

        p1.onCreate();
        assertEquals(ZoneType.NORMAL, p1.getRequiredZoneType());
        p1.onUpdate();
    }
}
