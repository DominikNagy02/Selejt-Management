package hu.unideb.inf.backend.controller;

import hu.unideb.inf.backend.config.ClockConfig;
import hu.unideb.inf.backend.service.inventory.InventoryService;
import hu.unideb.inf.backend.service.suggestion.ShelfSuggestionService;
import hu.unideb.inf.core.dto.InboundRequest;
import hu.unideb.inf.core.dto.InboundResponse;
import hu.unideb.inf.core.dto.PageResponse;
import hu.unideb.inf.core.dto.ScrapRequest;
import hu.unideb.inf.core.dto.ShelfSuggestionResponse;
import hu.unideb.inf.core.dto.TransferRequest;
import hu.unideb.inf.core.enums.InventoryStatus;
import hu.unideb.inf.core.enums.ZoneType;
import hu.unideb.inf.core.exception.InsufficientStockException;
import hu.unideb.inf.core.exception.InvalidTransferException;
import hu.unideb.inf.core.exception.ReservationConflictException;
import hu.unideb.inf.core.exception.ResourceNotFoundException;
import hu.unideb.inf.core.exception.ShelfCapacityExceededException;
import hu.unideb.inf.core.exception.ZoneMismatchException;
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
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InventoryController.class)
@Import({GlobalExceptionHandler.class, ClockConfig.class})
class InventoryControllerTest {

    private static final Instant NOW = Instant.parse("2026-11-26T16:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InventoryService inventoryService;

    @MockBean
    private ShelfSuggestionService shelfSuggestionService;

    @Test
    void inboundInventoryAndSuggestionEndpointsReturnExpectedResponsesAndErrors() throws Exception {
        InboundResponse item = new InboundResponse(
                884L, 102L, "SKU-102", "Csavar", 45L, "S-04-12", 0,
                "BATCH-2026-A1", LocalDate.of(2027, 12, 31), 50,
                new BigDecimal("320.500"), new BigDecimal("2.150000"),
                InventoryStatus.STORED, NOW
        );
        PageResponse<InboundResponse> page = new PageResponse<>(List.of(item), 0, 20, 1, 1, true, true);
        ShelfSuggestionResponse suggestion = new ShelfSuggestionResponse(
                45L, "S-04-12", ZoneType.NORMAL, new BigDecimal("179.500"), new BigDecimal("0.850000")
        );

        when(inventoryService.findAll(eq(45L), eq(102L), eq(1L), eq(InventoryStatus.STORED), any(Pageable.class)))
                .thenReturn(page);
        when(inventoryService.receive(any(InboundRequest.class)))
                .thenReturn(item)
                .thenThrow(new ZoneMismatchException("SKU-102", ZoneType.CHILLED, "S-04-12", ZoneType.NORMAL))
                .thenThrow(new ShelfCapacityExceededException("S-04-12", new BigDecimal("500"), new BigDecimal("540"), "kg"))
                .thenThrow(ResourceNotFoundException.of("Product", 999L));
        when(shelfSuggestionService.suggest(102L, 50, 1L, 5)).thenReturn(List.of(suggestion));

        mockMvc.perform(get("/api/v1/inventory")
                        .param("shelfId", "45")
                        .param("productId", "102")
                        .param("lotId", "1")
                        .param("status", "STORED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].inventoryItemId").value(884))
                .andExpect(jsonPath("$.totalElements").value(1));

        String validBody = """
                {
                  "productId": 102,
                  "shelfId": 45,
                  "tierLevel": 0,
                  "batchNumber": "BATCH-2026-A1",
                  "expirationDate": "2027-12-31",
                  "quantity": 50
                }
                """;
        mockMvc.perform(post("/api/v1/inventory/inbound")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.inventoryItemId").value(884))
                .andExpect(jsonPath("$.currentShelfWeightKg").value(320.5));

        mockMvc.perform(post("/api/v1/inventory/inbound")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://smartwms.hu/errors/zone-mismatch"));

        mockMvc.perform(post("/api/v1/inventory/inbound")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://smartwms.hu/errors/shelf-capacity-exceeded"));

        mockMvc.perform(post("/api/v1/inventory/inbound")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://smartwms.hu/errors/resource-not-found"));

        mockMvc.perform(post("/api/v1/inventory/inbound")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":102,"shelfId":45,"batchNumber":"","quantity":0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.batchNumber").isNotEmpty());

        mockMvc.perform(get("/api/v1/inventory/suggest-shelf")
                        .param("productId", "102")
                        .param("quantity", "50")
                        .param("warehouseId", "1")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].shelfCode").value("S-04-12"))
                .andExpect(jsonPath("$[0].zoneType").value("NORMAL"));
    }

    @Test
    void transferAndScrapEndpointsReturnExpectedResponsesAndErrors() throws Exception {
        InboundResponse moved = new InboundResponse(
                885L, 102L, "SKU-102", "Csavar", 46L, "S-04-13", 1,
                "BATCH-2026-A1", LocalDate.of(2027, 12, 31), 20,
                new BigDecimal("120.000"), new BigDecimal("0.800000"),
                InventoryStatus.STORED, NOW
        );
        InboundResponse scrapped = new InboundResponse(
                884L, 102L, "SKU-102", "Csavar", 45L, "S-04-12", 0,
                "BATCH-2026-A1", LocalDate.of(2027, 12, 31), 30,
                new BigDecimal("200.000"), new BigDecimal("1.350000"),
                InventoryStatus.STORED, NOW
        );

        when(inventoryService.transfer(any(TransferRequest.class)))
                .thenReturn(moved)
                .thenThrow(InvalidTransferException.sameLocation("S-04-12", 0))
                .thenThrow(new InsufficientStockException("SKU-102", 80, 30))
                .thenThrow(new ShelfCapacityExceededException("S-04-13", new BigDecimal("500"), new BigDecimal("540"), "kg"));
        when(inventoryService.scrap(any(ScrapRequest.class)))
                .thenReturn(scrapped)
                .thenThrow(new ReservationConflictException(884L, 10, 4));

        String validTransferBody = """
                {
                  "inventoryId": 884,
                  "fromShelfId": 45,
                  "fromTierLevel": 0,
                  "toShelfId": 46,
                  "toTierLevel": 1,
                  "quantity": 20,
                  "reason": "Átrendezés"
                }
                """;
        mockMvc.perform(post("/api/v1/inventory/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validTransferBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inventoryItemId").value(885))
                .andExpect(jsonPath("$.shelfCode").value("S-04-13"));

        mockMvc.perform(post("/api/v1/inventory/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validTransferBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://smartwms.hu/errors/invalid-transfer"));

        mockMvc.perform(post("/api/v1/inventory/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validTransferBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://smartwms.hu/errors/insufficient-stock"));

        mockMvc.perform(post("/api/v1/inventory/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validTransferBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://smartwms.hu/errors/shelf-capacity-exceeded"));

        String validScrapBody = """
                {
                  "inventoryId": 884,
                  "quantity": 10,
                  "reason": "Sérült csomagolás"
                }
                """;
        mockMvc.perform(post("/api/v1/inventory/scrap")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validScrapBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(30));

        mockMvc.perform(post("/api/v1/inventory/scrap")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validScrapBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://smartwms.hu/errors/reservation-conflict"));

        mockMvc.perform(post("/api/v1/inventory/scrap")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"inventoryId":884,"quantity":5,"reason":"   "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.reason").isNotEmpty());
    }
}
