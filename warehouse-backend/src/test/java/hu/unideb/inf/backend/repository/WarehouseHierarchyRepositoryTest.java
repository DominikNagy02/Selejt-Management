package hu.unideb.inf.backend.repository;

import hu.unideb.inf.backend.model.entity.ShelfEntity;
import hu.unideb.inf.backend.model.entity.WarehouseEntity;
import hu.unideb.inf.backend.model.entity.ZoneEntity;
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
class WarehouseHierarchyRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private ZoneRepository zoneRepository;

    @Autowired
    private ShelfRepository shelfRepository;

    @Test
    @DisplayName("Raktár -> Zóna -> Polc hierarchia mentése és visszakeresése cascading működéssel")
    void persistAndQueryWarehouseZoneShelfHierarchy() {
        WarehouseEntity warehouse = new WarehouseEntity("WH-DEB-01", "Debrecen Központi", "Kassai út 26.", true);
        ZoneEntity zoneNormal = new ZoneEntity(warehouse, "Z-NORM-01", "Normál tároló", ZoneType.NORMAL, true);
        ZoneEntity zoneChilled = new ZoneEntity(warehouse, "Z-CHILL-01", "Hűtött tároló", ZoneType.CHILLED, true);

        ShelfEntity shelf1 = new ShelfEntity(
                zoneNormal,
                "S-01-01",
                StorageType.RACK,
                4,
                new BigDecimal("1000.000"),
                new BigDecimal("5.000000"),
                true
        );
        ShelfEntity shelf2 = new ShelfEntity(
                zoneNormal,
                "S-01-02",
                StorageType.GROUND,
                2,
                new BigDecimal("2500.000"),
                new BigDecimal("10.000000"),
                true
        );

        zoneNormal.addShelf(shelf1);
        zoneNormal.addShelf(shelf2);
        warehouse.addZone(zoneNormal);
        warehouse.addZone(zoneChilled);

        WarehouseEntity savedWarehouse = warehouseRepository.saveAndFlush(warehouse);
        assertNotNull(savedWarehouse.getId());
        assertNotNull(savedWarehouse.getCreatedAt());
        assertNotNull(savedWarehouse.getUpdatedAt());

        Optional<WarehouseEntity> foundWh = warehouseRepository.findByCode("WH-DEB-01");
        assertTrue(foundWh.isPresent());
        assertEquals("Debrecen Központi", foundWh.get().getName());
        assertTrue(warehouseRepository.existsByCode("WH-DEB-01"));
        assertFalse(warehouseRepository.existsByCode("NON_EXISTING"));

        Optional<ZoneEntity> foundZone = zoneRepository.findByWarehouseIdAndCode(savedWarehouse.getId(), "Z-NORM-01");
        assertTrue(foundZone.isPresent());
        assertEquals(ZoneType.NORMAL, foundZone.get().getZoneType());
        assertTrue(zoneRepository.existsByWarehouseIdAndCode(savedWarehouse.getId(), "Z-NORM-01"));
        assertFalse(zoneRepository.existsByWarehouseIdAndCode(savedWarehouse.getId(), "NON_EXISTING"));

        List<ZoneEntity> zonesInWh = zoneRepository.findByWarehouseId(savedWarehouse.getId());
        assertEquals(2, zonesInWh.size());
        assertEquals(2, zoneRepository.findByWarehouseIdAndActiveTrue(savedWarehouse.getId()).size());
        assertFalse(zoneRepository.findByZoneTypeAndActiveTrue(ZoneType.CHILLED).isEmpty());

        Optional<ShelfEntity> foundShelf = shelfRepository.findByCode("S-01-01");
        assertTrue(foundShelf.isPresent());
        assertEquals(StorageType.RACK, foundShelf.get().getStorageType());
        assertEquals(4, foundShelf.get().getMaxTiers());
        assertTrue(shelfRepository.existsByCode("S-01-01"));
        assertFalse(shelfRepository.existsByCode("UNKNOWN"));

        List<ShelfEntity> shelvesInZone = shelfRepository.findByZoneId(foundZone.get().getId());
        assertEquals(2, shelvesInZone.size());
        assertEquals(2, shelfRepository.findByZoneIdAndActiveTrue(foundZone.get().getId()).size());

        List<ShelfEntity> shelvesInWh = shelfRepository.findByZoneWarehouseId(savedWarehouse.getId());
        assertEquals(2, shelvesInWh.size());

        ShelfLoad shelfLoad = foundShelf.get().toShelfLoad();
        assertEquals("S-01-01", shelfLoad.code());
        assertEquals(new BigDecimal("1000.000"), shelfLoad.maxWeight());
        assertEquals(new BigDecimal("5.000000"), shelfLoad.maxVolume());
        assertEquals(BigDecimal.ZERO, shelfLoad.currentWeight());
        assertEquals(BigDecimal.ZERO, shelfLoad.currentVolume());
    }

    @Test
    @DisplayName("Egyedi kód kényszerek ellenőrzése raktár, zóna és polc szinten")
    void uniqueConstraintsAreEnforced() {
        WarehouseEntity wh1 = new WarehouseEntity("WH-UNIQ", "Raktár 1", "Cím 1", true);
        warehouseRepository.saveAndFlush(wh1);

        WarehouseEntity whDuplicate = new WarehouseEntity("WH-UNIQ", "Másik raktár", "Cím 2", true);
        assertThrows(DataIntegrityViolationException.class, () -> {
            warehouseRepository.saveAndFlush(whDuplicate);
        });

        entityManager.clear();

        WarehouseEntity reloadedWh = warehouseRepository.findByCode("WH-UNIQ").orElseThrow();
        ZoneEntity z1 = new ZoneEntity(reloadedWh, "Z-01", "Zóna 1", ZoneType.NORMAL, true);
        zoneRepository.saveAndFlush(z1);

        ZoneEntity zDuplicate = new ZoneEntity(reloadedWh, "Z-01", "Zóna 1 duplikátum", ZoneType.CHILLED, true);
        assertThrows(DataIntegrityViolationException.class, () -> {
            zoneRepository.saveAndFlush(zDuplicate);
        });

        entityManager.clear();

        ZoneEntity reloadedZone = zoneRepository.findByWarehouseIdAndCode(reloadedWh.getId(), "Z-01").orElseThrow();
        ShelfEntity shelf1 = new ShelfEntity(
                reloadedZone,
                "SH-UNIQUE-01",
                StorageType.RACK,
                1,
                new BigDecimal("500.000"),
                new BigDecimal("2.000000"),
                true
        );
        shelfRepository.saveAndFlush(shelf1);

        ShelfEntity shelfDuplicate = new ShelfEntity(
                reloadedZone,
                "SH-UNIQUE-01",
                StorageType.GROUND,
                1,
                new BigDecimal("600.000"),
                new BigDecimal("3.000000"),
                true
        );
        assertThrows(DataIntegrityViolationException.class, () -> {
            shelfRepository.saveAndFlush(shelfDuplicate);
        });
    }

    @Test
    @DisplayName("Entitás metódusok és logikai törlés ellenőrzése")
    void helperMethodsAndActiveFilter() {
        WarehouseEntity wh = new WarehouseEntity("WH-ACTIVE", "Raktár Aktív", null, true);
        WarehouseEntity whInactive = new WarehouseEntity("WH-INACTIVE", "Inaktív Raktár", null, false);
        warehouseRepository.saveAllAndFlush(List.of(wh, whInactive));

        List<WarehouseEntity> activeWarehouses = warehouseRepository.findAllByActiveTrue();
        assertTrue(activeWarehouses.stream().anyMatch(w -> "WH-ACTIVE".equals(w.getCode())));
        assertFalse(activeWarehouses.stream().anyMatch(w -> "WH-INACTIVE".equals(w.getCode())));

        ZoneEntity zone = new ZoneEntity(wh, "Z-ACT", "Aktív Zóna", ZoneType.HAZARDOUS, true);
        wh.addZone(zone);
        warehouseRepository.saveAndFlush(wh);

        assertEquals(1, wh.getZones().size());
        wh.removeZone(zone);
        warehouseRepository.saveAndFlush(wh);
        assertEquals(0, wh.getZones().size());

        assertNotNull(wh.toString());
        assertNotNull(zone.toString());
        assertEquals(wh, wh);
        assertFalse(wh.equals(null));
        assertFalse(wh.equals("different-type"));
    }
}
