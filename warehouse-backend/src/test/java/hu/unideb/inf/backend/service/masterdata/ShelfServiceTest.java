package hu.unideb.inf.backend.service.masterdata;

import hu.unideb.inf.backend.model.entity.ShelfEntity;
import hu.unideb.inf.backend.model.entity.WarehouseEntity;
import hu.unideb.inf.backend.model.entity.ZoneEntity;
import hu.unideb.inf.backend.repository.ShelfRepository;
import hu.unideb.inf.backend.repository.ZoneRepository;
import hu.unideb.inf.core.dto.ShelfRequest;
import hu.unideb.inf.core.dto.ShelfResponse;
import hu.unideb.inf.core.enums.StorageType;
import hu.unideb.inf.core.enums.ZoneType;
import hu.unideb.inf.core.exception.DuplicateResourceException;
import hu.unideb.inf.core.exception.ResourceNotFoundException;
import hu.unideb.inf.core.exception.ShelfCapacityExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShelfServiceTest {

    @Mock
    private ShelfRepository shelfRepository;

    @Mock
    private ZoneRepository zoneRepository;

    private ShelfService shelfService;

    @BeforeEach
    void setUp() {
        shelfService = new ShelfService(shelfRepository, zoneRepository);
    }

    @Test
    void findAllSupportsZoneWarehouseActiveFiltersAndPagination() {
        WarehouseEntity wh1 = warehouse(1L, "WH-01");
        ZoneEntity z1 = zone(10L, wh1, "Z-01");
        ShelfEntity active = shelf(100L, z1, "S-01-01", true, "500.000", "2.000000", "0.000", "0.000000");
        ShelfEntity inactive = shelf(101L, z1, "S-01-02", false, "500.000", "2.000000", "0.000", "0.000000");
        Pageable pageable = PageRequest.of(0, 10);

        when(shelfRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(active, inactive), pageable, 2));
        when(shelfRepository.findAll()).thenReturn(List.of(active, inactive));
        when(shelfRepository.findByZoneId(10L)).thenReturn(List.of(active, inactive));
        when(shelfRepository.findByZoneIdAndActiveTrue(10L)).thenReturn(List.of(active));
        when(shelfRepository.findByZoneWarehouseId(1L)).thenReturn(List.of(active, inactive));

        assertEquals(2, shelfService.findAll(null, null, null, pageable).content().size());
        assertEquals(1, shelfService.findAll(null, null, true, pageable).content().size());
        assertEquals(2, shelfService.findAll(null, 10L, false, pageable).content().size());
        assertEquals(1, shelfService.findAll(1L, 10L, true, pageable).content().size());
        assertEquals(0, shelfService.findAll(99L, 10L, true, pageable).content().size());
        assertEquals(1, shelfService.findAll(1L, null, true, pageable).content().size());
        assertEquals(2, shelfService.findAll(null, null, false, Pageable.unpaged()).content().size());
    }

    @Test
    void findByIdAndFindByCodeReturnShelfOrThrowNotFound() {
        ZoneEntity z1 = zone(10L, warehouse(1L, "WH-01"), "Z-01");
        ShelfEntity existing = shelf(100L, z1, "S-01-01", true, "500.000", "2.000000", "10.000", "0.100000");
        when(shelfRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(shelfRepository.findById(999L)).thenReturn(Optional.empty());
        when(shelfRepository.findByCode("S-01-01")).thenReturn(Optional.of(existing));
        when(shelfRepository.findByCode("UNKNOWN")).thenReturn(Optional.empty());

        ShelfResponse byId = shelfService.findById(100L);
        assertEquals("S-01-01", byId.code());
        assertEquals("Z-01", byId.zoneCode());

        ShelfResponse byCode = shelfService.findByCode(" S-01-01 ");
        assertEquals(100L, byCode.id());

        assertThrows(ResourceNotFoundException.class, () -> shelfService.findById(999L));
        assertThrows(ResourceNotFoundException.class, () -> shelfService.findByCode("UNKNOWN"));
    }

    @Test
    void createValidatesDimensionsZoneAndUniqueness() {
        ZoneEntity z1 = zone(10L, warehouse(1L, "WH-01"), "Z-01");
        when(zoneRepository.findById(10L)).thenReturn(Optional.of(z1));
        when(zoneRepository.findById(99L)).thenReturn(Optional.empty());
        when(shelfRepository.existsByCode("S-01-01")).thenReturn(false);
        when(shelfRepository.save(any(ShelfEntity.class))).thenAnswer(inv -> {
            ShelfEntity saved = inv.getArgument(0);
            saved.setId(100L);
            return saved;
        });

        ShelfResponse created = shelfService.create(new ShelfRequest(
                10L, " S-01-01 ", StorageType.RACK, 3,
                new BigDecimal("500.000"), new BigDecimal("2.000000"), null
        ));
        assertEquals(100L, created.id());
        assertEquals("S-01-01", created.code());
        assertTrue(created.active());

        when(shelfRepository.existsByCode("S-01-02")).thenReturn(false);
        ShelfResponse inactive = shelfService.create(new ShelfRequest(
                10L, "S-01-02", StorageType.GROUND, 2,
                new BigDecimal("1000.000"), new BigDecimal("4.000000"), false
        ));
        assertFalse(inactive.active());

        when(shelfRepository.existsByCode("S-01-01")).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> shelfService.create(new ShelfRequest(
                10L, "S-01-01", StorageType.RACK, 1,
                new BigDecimal("500.000"), new BigDecimal("2.000000"), true
        )));
        assertThrows(ResourceNotFoundException.class, () -> shelfService.create(new ShelfRequest(
                99L, "S-01-03", StorageType.RACK, 1,
                new BigDecimal("500.000"), new BigDecimal("2.000000"), true
        )));

        assertThrows(IllegalArgumentException.class, () -> shelfService.create(new ShelfRequest(
                10L, "S-BAD-1", StorageType.RACK, 0,
                new BigDecimal("500.000"), new BigDecimal("2.000000"), true
        )));
        assertThrows(IllegalArgumentException.class, () -> shelfService.create(new ShelfRequest(
                10L, "S-BAD-2", StorageType.RACK, 1,
                BigDecimal.ZERO, new BigDecimal("2.000000"), true
        )));
        assertThrows(IllegalArgumentException.class, () -> shelfService.create(new ShelfRequest(
                10L, "S-BAD-3", StorageType.RACK, 1,
                new BigDecimal("500.000"), new BigDecimal("-1.000000"), true
        )));
    }

    @Test
    void updateEnforcesMaxNotBelowCurrentUniquenessAndDeactivate() {
        ZoneEntity z1 = zone(10L, warehouse(1L, "WH-01"), "Z-01");
        ShelfEntity existing = shelf(100L, z1, "S-01-01", true, "500.000", "2.000000", "300.000", "1.500000");

        when(shelfRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(zoneRepository.findById(10L)).thenReturn(Optional.of(z1));
        when(shelfRepository.save(any(ShelfEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        ShelfResponse sameCodeUpdated = shelfService.update(100L, new ShelfRequest(
                10L, "S-01-01", StorageType.GROUND, 2,
                new BigDecimal("300.000"), new BigDecimal("1.500000"), null
        ));
        assertEquals(StorageType.GROUND, sameCodeUpdated.storageType());
        assertEquals(2, sameCodeUpdated.maxTiers());
        assertTrue(sameCodeUpdated.active());

        when(shelfRepository.existsByCode("S-01-99")).thenReturn(false);
        ShelfResponse renamed = shelfService.update(100L, new ShelfRequest(
                10L, "S-01-99", StorageType.RACK, 4,
                new BigDecimal("600.000"), new BigDecimal("3.000000"), false
        ));
        assertEquals("S-01-99", renamed.code());
        assertFalse(renamed.active());

        when(shelfRepository.existsByCode("S-TAKEN")).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> shelfService.update(100L, new ShelfRequest(
                10L, "S-TAKEN", StorageType.RACK, 4,
                new BigDecimal("600.000"), new BigDecimal("3.000000"), true
        )));

        ShelfCapacityExceededException weightTooLow = assertThrows(
                ShelfCapacityExceededException.class,
                () -> shelfService.update(100L, new ShelfRequest(
                        10L, "S-01-99", StorageType.RACK, 4,
                        new BigDecimal("250.000"), new BigDecimal("2.000000"), true
                ))
        );
        assertEquals(1, weightTooLow.getViolations().size());

        ShelfCapacityExceededException bothTooLow = assertThrows(
                ShelfCapacityExceededException.class,
                () -> shelfService.update(100L, new ShelfRequest(
                        10L, "S-01-99", StorageType.RACK, 4,
                        new BigDecimal("250.000"), new BigDecimal("1.000000"), true
                ))
        );
        assertEquals(2, bothTooLow.getViolations().size());

        existing.setActive(true);
        ShelfResponse deactivated = shelfService.deactivate(100L);
        assertFalse(deactivated.active());
    }

    private WarehouseEntity warehouse(Long id, String code) {
        WarehouseEntity wh = new WarehouseEntity(code, "Raktár", null, true);
        wh.setId(id);
        return wh;
    }

    private ZoneEntity zone(Long id, WarehouseEntity wh, String code) {
        ZoneEntity zone = new ZoneEntity(wh, code, "Zóna", ZoneType.NORMAL, true);
        zone.setId(id);
        return zone;
    }

    private ShelfEntity shelf(
            Long id,
            ZoneEntity zone,
            String code,
            boolean active,
            String maxWeight,
            String maxVolume,
            String currentWeight,
            String currentVolume
    ) {
        ShelfEntity shelf = new ShelfEntity(
                zone,
                code,
                StorageType.RACK,
                3,
                new BigDecimal(maxWeight),
                new BigDecimal(maxVolume),
                active
        );
        shelf.setId(id);
        shelf.setCurrentWeightKg(new BigDecimal(currentWeight));
        shelf.setCurrentVolumeM3(new BigDecimal(currentVolume));
        shelf.setCreatedAt(Instant.parse("2026-11-26T10:00:00Z"));
        shelf.setUpdatedAt(Instant.parse("2026-11-26T10:00:00Z"));
        return shelf;
    }
}
