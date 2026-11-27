package hu.unideb.inf.backend.service.masterdata;

import hu.unideb.inf.backend.model.entity.WarehouseEntity;
import hu.unideb.inf.backend.model.entity.ZoneEntity;
import hu.unideb.inf.backend.repository.WarehouseRepository;
import hu.unideb.inf.backend.repository.ZoneRepository;
import hu.unideb.inf.core.dto.ZoneRequest;
import hu.unideb.inf.core.dto.ZoneResponse;
import hu.unideb.inf.core.enums.ZoneType;
import hu.unideb.inf.core.exception.DuplicateResourceException;
import hu.unideb.inf.core.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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
class ZoneServiceTest {

    @Mock
    private ZoneRepository zoneRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    private ZoneService zoneService;

    @BeforeEach
    void setUp() {
        zoneService = new ZoneService(zoneRepository, warehouseRepository);
    }

    @Test
    void findAllSupportsWarehouseZoneTypeActiveFiltersAndPagination() {
        WarehouseEntity wh = warehouse(1L, "WH-01");
        ZoneEntity normalActive = zone(10L, wh, "Z-A", "Normál", ZoneType.NORMAL, true);
        ZoneEntity chilledInactive = zone(11L, wh, "Z-B", "Hűtött", ZoneType.CHILLED, false);
        Pageable pageable = PageRequest.of(0, 10);

        when(zoneRepository.findByWarehouseId(1L)).thenReturn(List.of(normalActive, chilledInactive));
        when(zoneRepository.findByWarehouseIdAndActiveTrue(1L)).thenReturn(List.of(normalActive));
        when(zoneRepository.findByZoneTypeAndActiveTrue(ZoneType.NORMAL)).thenReturn(List.of(normalActive));
        when(zoneRepository.findAll()).thenReturn(List.of(normalActive, chilledInactive));
        when(zoneRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(normalActive, chilledInactive), pageable, 2));

        assertEquals(2, zoneService.findAll(1L, null, false, pageable).content().size());
        assertEquals(1, zoneService.findAll(1L, ZoneType.CHILLED, false, pageable).content().size());
        assertEquals(1, zoneService.findAll(1L, null, true, pageable).content().size());
        assertEquals(1, zoneService.findAll(null, ZoneType.NORMAL, true, pageable).content().size());
        assertEquals(1, zoneService.findAll(null, null, true, pageable).content().size());
        assertEquals(2, zoneService.findAll(null, null, null, pageable).content().size());
        assertEquals(2, zoneService.findAll(null, null, null, Pageable.unpaged()).content().size());
    }

    @Test
    void findByIdReturnsZoneOrThrowsNotFound() {
        WarehouseEntity wh = warehouse(1L, "WH-01");
        ZoneEntity existing = zone(10L, wh, "Z-A", "Normál", ZoneType.NORMAL, true);
        when(zoneRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(zoneRepository.findById(99L)).thenReturn(Optional.empty());

        ZoneResponse found = zoneService.findById(10L);
        assertEquals(10L, found.id());
        assertEquals(1L, found.warehouseId());
        assertEquals(ZoneType.NORMAL, found.zoneType());

        assertThrows(ResourceNotFoundException.class, () -> zoneService.findById(99L));
    }

    @Test
    void createValidatesWarehouseUniquenessAndActiveDefault() {
        WarehouseEntity wh = warehouse(1L, "WH-01");
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(wh));
        when(warehouseRepository.findById(99L)).thenReturn(Optional.empty());
        when(zoneRepository.existsByWarehouseIdAndCode(1L, "Z-A")).thenReturn(false);
        when(zoneRepository.save(any(ZoneEntity.class))).thenAnswer(inv -> {
            ZoneEntity saved = inv.getArgument(0);
            saved.setId(20L);
            return saved;
        });

        ZoneResponse created = zoneService.create(new ZoneRequest(1L, " Z-A ", " Normál ", ZoneType.NORMAL, null));
        assertEquals(20L, created.id());
        assertEquals("Z-A", created.code());
        assertEquals("Normál", created.name());
        assertTrue(created.active());

        when(zoneRepository.existsByWarehouseIdAndCode(1L, "Z-B")).thenReturn(false);
        ZoneResponse createdInactive = zoneService.create(
                new ZoneRequest(1L, "Z-B", "Hűtött", ZoneType.CHILLED, false)
        );
        assertFalse(createdInactive.active());

        when(zoneRepository.existsByWarehouseIdAndCode(1L, "Z-A")).thenReturn(true);
        assertThrows(
                DuplicateResourceException.class,
                () -> zoneService.create(new ZoneRequest(1L, "Z-A", "Duplikált", ZoneType.NORMAL, true))
        );
        assertThrows(
                ResourceNotFoundException.class,
                () -> zoneService.create(new ZoneRequest(99L, "Z-C", "Hiányzó raktár", ZoneType.HAZARDOUS, true))
        );
    }

    @Test
    void updateHandlesSameKeyChangedKeyDuplicateAndDeactivate() {
        WarehouseEntity wh1 = warehouse(1L, "WH-01");
        WarehouseEntity wh2 = warehouse(2L, "WH-02");
        ZoneEntity existing = zone(10L, wh1, "Z-A", "Normál", ZoneType.NORMAL, true);

        when(zoneRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(wh1));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(wh2));
        when(zoneRepository.save(any(ZoneEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        ZoneResponse sameKey = zoneService.update(10L, new ZoneRequest(1L, "Z-A", "Frissített", ZoneType.CHILLED, null));
        assertEquals("Frissített", sameKey.name());
        assertEquals(ZoneType.CHILLED, sameKey.zoneType());
        assertTrue(sameKey.active());

        when(zoneRepository.existsByWarehouseIdAndCode(1L, "Z-NEW")).thenReturn(false);
        ZoneResponse changedCode = zoneService.update(10L, new ZoneRequest(1L, "Z-NEW", "Új kód", ZoneType.NORMAL, false));
        assertEquals("Z-NEW", changedCode.code());
        assertFalse(changedCode.active());

        when(zoneRepository.existsByWarehouseIdAndCode(2L, "Z-NEW")).thenReturn(false);
        ZoneResponse movedWarehouse = zoneService.update(10L, new ZoneRequest(2L, "Z-NEW", "Áthelyezett", ZoneType.HAZARDOUS, true));
        assertEquals(2L, movedWarehouse.warehouseId());

        when(zoneRepository.existsByWarehouseIdAndCode(2L, "Z-TAKEN")).thenReturn(true);
        assertThrows(
                DuplicateResourceException.class,
                () -> zoneService.update(10L, new ZoneRequest(2L, "Z-TAKEN", "Ütköző", ZoneType.NORMAL, true))
        );

        ZoneResponse deactivated = zoneService.deactivate(10L);
        assertFalse(deactivated.active());
    }

    private WarehouseEntity warehouse(Long id, String code) {
        WarehouseEntity wh = new WarehouseEntity(code, "Raktár " + code, null, true);
        wh.setId(id);
        return wh;
    }

    private ZoneEntity zone(Long id, WarehouseEntity wh, String code, String name, ZoneType type, boolean active) {
        ZoneEntity zone = new ZoneEntity(wh, code, name, type, active);
        zone.setId(id);
        zone.setCreatedAt(Instant.parse("2026-11-26T10:00:00Z"));
        zone.setUpdatedAt(Instant.parse("2026-11-26T10:00:00Z"));
        return zone;
    }
}
