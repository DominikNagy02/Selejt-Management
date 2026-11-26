package hu.unideb.inf.backend.controller;

import hu.unideb.inf.backend.service.masterdata.WarehouseService;
import hu.unideb.inf.core.dto.WarehouseRequest;
import hu.unideb.inf.core.dto.WarehouseResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/warehouses")
public class WarehouseController {

    private final WarehouseService warehouseService;

    public WarehouseController(WarehouseService warehouseService) {
        this.warehouseService = Objects.requireNonNull(
                warehouseService,
                "warehouseService must not be null"
        );
    }

    @GetMapping
    public List<WarehouseResponse> findAll(
            @RequestParam(name = "activeOnly", required = false) Boolean activeOnly
    ) {
        return warehouseService.findAll(activeOnly);
    }

    @GetMapping("/{id}")
    public WarehouseResponse findById(@PathVariable("id") Long id) {
        return warehouseService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WarehouseResponse create(@Valid @RequestBody WarehouseRequest request) {
        return warehouseService.create(request);
    }

    @PutMapping("/{id}")
    public WarehouseResponse update(
            @PathVariable("id") Long id,
            @Valid @RequestBody WarehouseRequest request
    ) {
        return warehouseService.update(id, request);
    }

    @PatchMapping("/{id}/deactivate")
    public WarehouseResponse deactivate(@PathVariable("id") Long id) {
        return warehouseService.deactivate(id);
    }
}
