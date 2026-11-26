package hu.unideb.inf.backend.controller;

import hu.unideb.inf.backend.config.ClockConfig;
import hu.unideb.inf.backend.service.masterdata.ProductService;
import hu.unideb.inf.backend.service.masterdata.ShelfService;
import hu.unideb.inf.core.dto.PageResponse;
import hu.unideb.inf.core.dto.ProductRequest;
import hu.unideb.inf.core.dto.ProductResponse;
import hu.unideb.inf.core.dto.ShelfRequest;
import hu.unideb.inf.core.dto.ShelfResponse;
import hu.unideb.inf.core.enums.StorageType;
import hu.unideb.inf.core.enums.ZoneType;
import hu.unideb.inf.core.exception.CapacityViolation;
import hu.unideb.inf.core.exception.DuplicateResourceException;
import hu.unideb.inf.core.exception.ResourceNotFoundException;
import hu.unideb.inf.core.exception.ShelfCapacityExceededException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {ShelfController.class, ProductController.class})
@Import({GlobalExceptionHandler.class, ClockConfig.class})
class ShelfAndProductControllerTest {

    private static final Instant NOW = Instant.parse("2026-11-26T12:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ShelfService shelfService;

    @MockBean
    private ProductService productService;

    @Test
    void shelfEndpointsSupportCrudCodeLookupPaginationAndCapacityErrors() throws Exception {
        ShelfResponse shelf = new ShelfResponse(
                100L, 10L, "Z-01", "S-01-01", StorageType.RACK, 3,
                new BigDecimal("500.000"), new BigDecimal("2.000000"),
                new BigDecimal("100.000"), new BigDecimal("0.500000"),
                true, NOW, NOW
        );
        ShelfResponse inactive = new ShelfResponse(
                100L, 10L, "Z-01", "S-01-01", StorageType.RACK, 3,
                new BigDecimal("500.000"), new BigDecimal("2.000000"),
                new BigDecimal("100.000"), new BigDecimal("0.500000"),
                false, NOW, NOW
        );
        PageResponse<ShelfResponse> page = new PageResponse<>(List.of(shelf), 0, 10, 1, 1, true, true);

        when(shelfService.findAll(eq(1L), eq(10L), eq(true), any(Pageable.class))).thenReturn(page);
        when(shelfService.findById(100L)).thenReturn(shelf);
        when(shelfService.findByCode("S-01-01")).thenReturn(shelf);
        when(shelfService.create(any(ShelfRequest.class))).thenReturn(shelf);
        when(shelfService.update(eq(100L), any(ShelfRequest.class)))
                .thenReturn(shelf)
                .thenThrow(new ShelfCapacityExceededException(
                        "S-01-01",
                        List.of(new CapacityViolation("weight", new BigDecimal("50"), new BigDecimal("100"), "kg"))
                ));
        when(shelfService.deactivate(100L)).thenReturn(inactive);

        mockMvc.perform(get("/api/v1/shelves")
                        .param("warehouseId", "1")
                        .param("zoneId", "10")
                        .param("activeOnly", "true")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value("S-01-01"))
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/v1/shelves/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxTiers").value(3));

        mockMvc.perform(get("/api/v1/shelves/code/S-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100));

        String validBody = """
                {
                  "zoneId": 10,
                  "code": "S-01-01",
                  "storageType": "RACK",
                  "maxTiers": 3,
                  "maxWeightKg": 500.000,
                  "maxVolumeM3": 2.000000,
                  "active": true
                }
                """;
        mockMvc.perform(post("/api/v1/shelves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("S-01-01"));

        mockMvc.perform(put("/api/v1/shelves/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("S-01-01"));

        mockMvc.perform(put("/api/v1/shelves/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://smartwms.hu/errors/shelf-capacity-exceeded"));

        mockMvc.perform(patch("/api/v1/shelves/100/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void productEndpointsSupportSearchLookupsCrudAndErrors() throws Exception {
        ProductResponse product = new ProductResponse(
                1L, "SKU-MILK", "5991234567890", "Tej 2.8%", "Dobozos tej",
                new BigDecimal("1.030"), new BigDecimal("0.001050"),
                false, ZoneType.CHILLED, 20, true, NOW, NOW
        );
        ProductResponse inactive = new ProductResponse(
                1L, "SKU-MILK", "5991234567890", "Tej 2.8%", "Dobozos tej",
                new BigDecimal("1.030"), new BigDecimal("0.001050"),
                false, ZoneType.CHILLED, 20, false, NOW, NOW
        );
        PageResponse<ProductResponse> page = new PageResponse<>(List.of(product), 0, 10, 1, 1, true, true);

        when(productService.findAll(eq("tej"), eq(ZoneType.CHILLED), eq(true), any(Pageable.class))).thenReturn(page);
        when(productService.findById(1L)).thenReturn(product);
        when(productService.findBySku("SKU-MILK")).thenReturn(product);
        when(productService.findByBarcode("5991234567890")).thenReturn(product);
        when(productService.findById(99L)).thenThrow(ResourceNotFoundException.of("Product", 99L));
        when(productService.create(any(ProductRequest.class)))
                .thenReturn(product)
                .thenThrow(DuplicateResourceException.of("Product", "sku", "SKU-MILK"));
        when(productService.update(eq(1L), any(ProductRequest.class))).thenReturn(product);
        when(productService.deactivate(1L)).thenReturn(inactive);

        mockMvc.perform(get("/api/v1/products")
                        .param("search", "tej")
                        .param("zoneType", "CHILLED")
                        .param("activeOnly", "true")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].sku").value("SKU-MILK"))
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/v1/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.barcode").value("5991234567890"));

        mockMvc.perform(get("/api/v1/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://smartwms.hu/errors/resource-not-found"));

        mockMvc.perform(get("/api/v1/products/sku/SKU-MILK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        mockMvc.perform(get("/api/v1/products/barcode/5991234567890"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("SKU-MILK"));

        String validBody = """
                {
                  "sku": "SKU-MILK",
                  "barcode": "5991234567890",
                  "name": "Tej 2.8%",
                  "description": "Dobozos tej",
                  "unitWeightKg": 1.030,
                  "unitVolumeM3": 0.001050,
                  "requiresExpiry": false,
                  "requiredZoneType": "CHILLED",
                  "minStockLevel": 20,
                  "active": true
                }
                """;
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value("SKU-MILK"));

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://smartwms.hu/errors/duplicate-resource"));

        mockMvc.perform(put("/api/v1/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Tej 2.8%"));

        mockMvc.perform(patch("/api/v1/products/1/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }
}
