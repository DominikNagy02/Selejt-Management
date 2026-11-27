package hu.unideb.inf.backend.service.masterdata;

import hu.unideb.inf.backend.model.entity.ProductEntity;
import hu.unideb.inf.backend.repository.ProductRepository;
import hu.unideb.inf.core.dto.ProductRequest;
import hu.unideb.inf.core.dto.ProductResponse;
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

import java.math.BigDecimal;
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
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository);
    }

    @Test
    void findAllSupportsSearchBySkuBarcodeNameZoneTypeActiveAndPagination() {
        ProductEntity milk = product(1L, "SKU-MILK", "5991111111111", "Tej 2.8%", ZoneType.CHILLED, true);
        ProductEntity oil = product(2L, "SKU-OIL", "5992222222222", "Motorolaj 5W30", ZoneType.HAZARDOUS, false);
        Pageable pageable = PageRequest.of(0, 10);

        when(productRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(milk, oil), pageable, 2));
        when(productRepository.findAll()).thenReturn(List.of(milk, oil));
        when(productRepository.findAllByActiveTrue()).thenReturn(List.of(milk));
        when(productRepository.findByRequiredZoneType(ZoneType.CHILLED)).thenReturn(List.of(milk));

        assertEquals(2, productService.findAll("  ", null, null, pageable).content().size());
        assertEquals(2, productService.findAll(null, null, false, Pageable.unpaged()).content().size());
        assertEquals(1, productService.findAll(null, null, true, pageable).content().size());
        assertEquals(1, productService.findAll(null, ZoneType.CHILLED, true, pageable).content().size());
        assertEquals(1, productService.findAll("milk", null, false, pageable).content().size());
        assertEquals(1, productService.findAll("599222", null, false, pageable).content().size());
        assertEquals(1, productService.findAll("motorolaj", null, false, pageable).content().size());
        assertEquals(0, productService.findAll("nem-letezik", null, false, pageable).content().size());
    }

    @Test
    void findByIdSkuAndBarcodeReturnProductOrThrowNotFound() {
        ProductEntity milk = product(1L, "SKU-MILK", "5991111111111", "Tej 2.8%", ZoneType.CHILLED, true);
        when(productRepository.findById(1L)).thenReturn(Optional.of(milk));
        when(productRepository.findById(99L)).thenReturn(Optional.empty());
        when(productRepository.findBySku("SKU-MILK")).thenReturn(Optional.of(milk));
        when(productRepository.findBySku("MISSING")).thenReturn(Optional.empty());
        when(productRepository.findByBarcode("5991111111111")).thenReturn(Optional.of(milk));
        when(productRepository.findByBarcode("0000")).thenReturn(Optional.empty());

        assertEquals("SKU-MILK", productService.findById(1L).sku());
        assertEquals(1L, productService.findBySku(" SKU-MILK ").id());
        assertEquals(1L, productService.findByBarcode(" 5991111111111 ").id());

        assertThrows(ResourceNotFoundException.class, () -> productService.findById(99L));
        assertThrows(ResourceNotFoundException.class, () -> productService.findBySku("MISSING"));
        assertThrows(ResourceNotFoundException.class, () -> productService.findByBarcode("0000"));
    }

    @Test
    void createValidatesUniqueSkuAndBarcodeAndAppliesDefaults() {
        when(productRepository.existsBySku("SKU-01")).thenReturn(false);
        when(productRepository.existsByBarcode("5990001")).thenReturn(false);
        when(productRepository.save(any(ProductEntity.class))).thenAnswer(inv -> {
            ProductEntity saved = inv.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        ProductResponse created = productService.create(new ProductRequest(
                " SKU-01 ", " 5990001 ", " Termék ", "   ",
                new BigDecimal("1.500"), new BigDecimal("0.010000"),
                false, null, 5, null
        ));
        assertEquals(10L, created.id());
        assertEquals("SKU-01", created.sku());
        assertEquals("5990001", created.barcode());
        assertNull(created.description());
        assertEquals(ZoneType.NORMAL, created.requiredZoneType());
        assertTrue(created.active());

        when(productRepository.existsBySku("SKU-02")).thenReturn(false);
        when(productRepository.existsByBarcode("5990002")).thenReturn(false);
        ProductResponse chilledInactive = productService.create(new ProductRequest(
                "SKU-02", "5990002", "Hűtött", " Leírás ",
                new BigDecimal("1.000"), new BigDecimal("0.005000"),
                true, ZoneType.CHILLED, 10, false
        ));
        assertEquals("Leírás", chilledInactive.description());
        assertEquals(ZoneType.CHILLED, chilledInactive.requiredZoneType());
        assertFalse(chilledInactive.active());

        when(productRepository.existsBySku("SKU-01")).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> productService.create(new ProductRequest(
                "SKU-01", "5990099", "Dup SKU", null,
                new BigDecimal("1.000"), new BigDecimal("0.005000"),
                false, ZoneType.NORMAL, 0, true
        )));

        when(productRepository.existsBySku("SKU-99")).thenReturn(false);
        when(productRepository.existsByBarcode("5990001")).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> productService.create(new ProductRequest(
                "SKU-99", "5990001", "Dup Barcode", null,
                new BigDecimal("1.000"), new BigDecimal("0.005000"),
                false, ZoneType.NORMAL, 0, true
        )));
    }

    @Test
    void updateChecksDuplicateSkuAndBarcodeAndSupportsDeactivate() {
        ProductEntity existing = product(1L, "SKU-01", "5990001", "Régi", ZoneType.NORMAL, true);
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.save(any(ProductEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponse sameKeys = productService.update(1L, new ProductRequest(
                "SKU-01", "5990001", "Frissített", null,
                new BigDecimal("2.000"), new BigDecimal("0.020000"),
                false, null, 15, null
        ));
        assertEquals("Frissített", sameKeys.name());
        assertEquals(ZoneType.NORMAL, sameKeys.requiredZoneType());
        assertTrue(sameKeys.active());

        when(productRepository.existsBySku("SKU-02")).thenReturn(false);
        when(productRepository.existsByBarcode("5990002")).thenReturn(false);
        ProductResponse changedKeys = productService.update(1L, new ProductRequest(
                "SKU-02", "5990002", "Új", "Desc",
                new BigDecimal("2.500"), new BigDecimal("0.025000"),
                false, ZoneType.HAZARDOUS, 20, false
        ));
        assertEquals("SKU-02", changedKeys.sku());
        assertEquals("5990002", changedKeys.barcode());
        assertEquals(ZoneType.HAZARDOUS, changedKeys.requiredZoneType());
        assertFalse(changedKeys.active());

        when(productRepository.existsBySku("SKU-TAKEN")).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> productService.update(1L, new ProductRequest(
                "SKU-TAKEN", "5990002", "Ütköző", null,
                new BigDecimal("2.500"), new BigDecimal("0.025000"),
                false, ZoneType.NORMAL, 0, true
        )));

        when(productRepository.existsByBarcode("599-TAKEN")).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> productService.update(1L, new ProductRequest(
                "SKU-02", "599-TAKEN", "Ütköző", null,
                new BigDecimal("2.500"), new BigDecimal("0.025000"),
                false, ZoneType.NORMAL, 0, true
        )));

        existing.setActive(true);
        ProductResponse deactivated = productService.deactivate(1L);
        assertFalse(deactivated.active());
    }

    private ProductEntity product(Long id, String sku, String barcode, String name, ZoneType zoneType, boolean active) {
        ProductEntity entity = new ProductEntity(
                sku,
                barcode,
                name,
                null,
                new BigDecimal("1.000"),
                new BigDecimal("0.010000"),
                zoneType,
                5,
                active
        );
        entity.setId(id);
        entity.setCreatedAt(Instant.parse("2026-11-26T10:00:00Z"));
        entity.setUpdatedAt(Instant.parse("2026-11-26T10:00:00Z"));
        return entity;
    }
}
