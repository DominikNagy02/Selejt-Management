package hu.unideb.inf.backend.service.masterdata;

import hu.unideb.inf.backend.model.entity.WarehouseEntity;
import hu.unideb.inf.backend.repository.WarehouseRepository;
import hu.unideb.inf.core.dto.WarehouseRequest;
import hu.unideb.inf.core.dto.WarehouseResponse;
import hu.unideb.inf.core.exception.DuplicateResourceException;
import hu.unideb.inf.core.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WarehouseServiceTest {

    @Mock
    private WarehouseRepository warehouseRepository;

    private WarehouseService warehouseService;

    @BeforeEach
    void setUp() {
        warehouseService = new WarehouseService(warehouseRepository);
    }

    @Test
    void findAllReturnsAllOrOnlyActiveWarehouses() {
        WarehouseEntity active = entity(1L, "WH-01", "Debrecen", "Kassai út 26.", true);
        WarehouseEntity inactive = entity(2L, "WH-02", "Régi", null, false);
        when(warehouseRepository.findAll()).thenReturn(List.of(active, inactive));
        when(warehouseRepository.findAllByActiveTrue()).thenReturn(List.of(active));

        assertEquals(2, warehouseService.findAll(null).size());
        assertEquals(2, warehouseService.findAll(false).size());

        List<WarehouseResponse> onlyActive = warehouseService.findAll(true);
        assertEquals(1, onlyActive.size());
        assertEquals("WH-01", onlyActive.get(0).code());
        assertTrue(onlyActive.get(0).active());
    }

    @Test
    void findByIdReturnsWarehouseOrThrowsNotFound() {
        WarehouseEntity existing = entity(1L, "WH-01", "Debrecen", "Kassai út 26.", true);
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(warehouseRepository.findById(99L)).thenReturn(Optional.empty());

        WarehouseResponse response = warehouseService.findById(1L);
        assertEquals(1L, response.id());
        assertEquals("WH-01", response.code());

        assertThrows(ResourceNotFoundException.class, () -> warehouseService.findById(99L));
    }

    @Test
    void createTrimsFieldsAppliesDefaultsAndRejectsDuplicateCode() {
        when(warehouseRepository.existsByCode("WH-01")).thenReturn(false);
        when(warehouseRepository.save(any(WarehouseEntity.class))).thenAnswer(inv -> {
            WarehouseEntity saved = inv.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        WarehouseResponse created = warehouseService.create(
                new WarehouseRequest(" WH-01 ", " Központi ", "   ", null)
        );
        assertEquals(10L, created.id());
        assertEquals("WH-01", created.code());
        assertEquals("Központi", created.name());
        assertNull(created.address());
        assertTrue(created.active());

        when(warehouseRepository.existsByCode("WH-02")).thenReturn(false);
        WarehouseResponse createdInactive = warehouseService.create(
                new WarehouseRequest("WH-02", "Tartalék", " Debrecen ", false)
        );
        assertEquals("Debrecen", createdInactive.address());
        assertFalse(createdInactive.active());

        when(warehouseRepository.existsByCode("WH-01")).thenReturn(true);
        assertThrows(
                DuplicateResourceException.class,
                () -> warehouseService.create(new WarehouseRequest("WH-01", "Másik", null, true))
        );
    }

    @Test
    void updateHandlesSameCodeChangedCodeDuplicateAndDeactivate() {
        WarehouseEntity existing = entity(1L, "WH-01", "Régi név", "Régi cím", true);
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(warehouseRepository.save(any(WarehouseEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        WarehouseResponse sameCode = warehouseService.update(
                1L,
                new WarehouseRequest("WH-01", "Új név", null, null)
        );
        assertEquals("Új név", sameCode.name());
        assertNull(sameCode.address());
        assertTrue(sameCode.active());

        when(warehouseRepository.existsByCode("WH-02")).thenReturn(false);
        WarehouseResponse renamed = warehouseService.update(
                1L,
                new WarehouseRequest("WH-02", "Átnevezett", "Új cím", false)
        );
        assertEquals("WH-02", renamed.code());
        assertEquals("Új cím", renamed.address());
        assertFalse(renamed.active());

        when(warehouseRepository.existsByCode("WH-TAKEN")).thenReturn(true);
        assertThrows(
                DuplicateResourceException.class,
                () -> warehouseService.update(1L, new WarehouseRequest("WH-TAKEN", "Ütköző", null, true))
        );

        existing.setActive(true);
        WarehouseResponse deactivated = warehouseService.deactivate(1L);
        assertFalse(deactivated.active());
    }

    private WarehouseEntity entity(Long id, String code, String name, String address, boolean active) {
        WarehouseEntity entity = new WarehouseEntity(code, name, address, active);
        entity.setId(id);
        entity.setCreatedAt(Instant.parse("2026-11-26T10:00:00Z"));
        entity.setUpdatedAt(Instant.parse("2026-11-26T10:00:00Z"));
        return entity;
    }
}
