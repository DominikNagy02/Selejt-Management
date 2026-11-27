package hu.unideb.inf.backend.controller;

import hu.unideb.inf.backend.config.ClockConfig;
import hu.unideb.inf.backend.service.masterdata.WarehouseService;
import hu.unideb.inf.backend.service.masterdata.ZoneService;
import hu.unideb.inf.core.dto.PageResponse;
import hu.unideb.inf.core.dto.WarehouseRequest;
import hu.unideb.inf.core.dto.WarehouseResponse;
import hu.unideb.inf.core.dto.ZoneRequest;
import hu.unideb.inf.core.dto.ZoneResponse;
import hu.unideb.inf.core.enums.ZoneType;
import hu.unideb.inf.core.exception.DuplicateResourceException;
import hu.unideb.inf.core.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

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

@WebMvcTest(controllers = {WarehouseController.class, ZoneController.class})
@Import({GlobalExceptionHandler.class, ClockConfig.class})
class WarehouseAndZoneControllerTest {

    private static final Instant NOW = Instant.parse("2026-11-26T12:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WarehouseService warehouseService;

    @MockBean
    private ZoneService zoneService;

    @Test
    void warehouseEndpointsSupportCrudDeactivateAndErrors() throws Exception {
        WarehouseResponse active = new WarehouseResponse(1L, "WH-01", "Debrecen", "Kassai út 26.", true, NOW, NOW);
        WarehouseResponse inactive = new WarehouseResponse(1L, "WH-01", "Debrecen", "Kassai út 26.", false, NOW, NOW);

        PageResponse<WarehouseResponse> page = new PageResponse<>(List.of(active), 0, 10, 1, 1, true, true);
        when(warehouseService.findAll(eq(true), any(Pageable.class))).thenReturn(page);
        when(warehouseService.findById(1L)).thenReturn(active);
        when(warehouseService.create(any(WarehouseRequest.class))).thenReturn(active);
        when(warehouseService.update(eq(1L), any(WarehouseRequest.class))).thenReturn(active);
        when(warehouseService.deactivate(1L)).thenReturn(inactive);
        when(warehouseService.findById(99L)).thenThrow(ResourceNotFoundException.of("Warehouse", 99L));

        mockMvc.perform(get("/api/v1/warehouses")
                        .param("activeOnly", "true")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value("WH-01"))
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/v1/warehouses/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Debrecen"));

        mockMvc.perform(get("/api/v1/warehouses/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://smartwms.hu/errors/resource-not-found"));

        mockMvc.perform(post("/api/v1/warehouses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"WH-01","name":"Debrecen","address":"Kassai út 26.","active":true}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));

        mockMvc.perform(post("/api/v1/warehouses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"","name":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.code").isNotEmpty());

        mockMvc.perform(put("/api/v1/warehouses/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"WH-01","name":"Debrecen","address":"Kassai út 26.","active":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("WH-01"));

        mockMvc.perform(patch("/api/v1/warehouses/1/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void zoneEndpointsSupportFilteringCrudDeactivateAndConflicts() throws Exception {
        ZoneResponse zone = new ZoneResponse(10L, 1L, "Z-CHILL", "Hűtött zóna", ZoneType.CHILLED, true, NOW, NOW);
        ZoneResponse inactiveZone = new ZoneResponse(10L, 1L, "Z-CHILL", "Hűtött zóna", ZoneType.CHILLED, false, NOW, NOW);

        PageResponse<ZoneResponse> page = new PageResponse<>(List.of(zone), 0, 10, 1, 1, true, true);
        when(zoneService.findAll(eq(1L), eq(ZoneType.CHILLED), eq(true), any(Pageable.class))).thenReturn(page);
        when(zoneService.findById(10L)).thenReturn(zone);
        when(zoneService.create(any(ZoneRequest.class)))
                .thenReturn(zone)
                .thenThrow(DuplicateResourceException.of("Zone", "code", "Z-CHILL"));
        when(zoneService.update(eq(10L), any(ZoneRequest.class))).thenReturn(zone);
        when(zoneService.deactivate(10L)).thenReturn(inactiveZone);

        mockMvc.perform(get("/api/v1/zones")
                        .param("warehouseId", "1")
                        .param("zoneType", "CHILLED")
                        .param("activeOnly", "true")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].zoneType").value("CHILLED"))
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/v1/zones/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("Z-CHILL"));

        String validBody = """
                {"warehouseId":1,"code":"Z-CHILL","name":"Hűtött zóna","zoneType":"CHILLED","active":true}
                """;
        mockMvc.perform(post("/api/v1/zones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10));

        mockMvc.perform(post("/api/v1/zones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://smartwms.hu/errors/duplicate-resource"));

        mockMvc.perform(put("/api/v1/zones/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Hűtött zóna"));

        mockMvc.perform(patch("/api/v1/zones/10/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }
}
