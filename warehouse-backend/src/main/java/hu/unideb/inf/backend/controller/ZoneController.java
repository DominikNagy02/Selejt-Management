package hu.unideb.inf.backend.controller;

import hu.unideb.inf.backend.service.masterdata.ZoneService;
import hu.unideb.inf.core.dto.PageResponse;
import hu.unideb.inf.core.dto.ZoneRequest;
import hu.unideb.inf.core.dto.ZoneResponse;
import hu.unideb.inf.core.enums.ZoneType;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/zones")
public class ZoneController {

    private final ZoneService zoneService;

    public ZoneController(ZoneService zoneService) {
        this.zoneService = Objects.requireNonNull(
                zoneService,
                "zoneService must not be null"
        );
    }

    @GetMapping
    public PageResponse<ZoneResponse> findAll(
            @RequestParam(name = "warehouseId", required = false) Long warehouseId,
            @RequestParam(name = "zoneType", required = false) ZoneType zoneType,
            @RequestParam(name = "activeOnly", required = false) Boolean activeOnly,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return zoneService.findAll(warehouseId, zoneType, activeOnly, pageable);
    }

    @GetMapping("/{id}")
    public ZoneResponse findById(@PathVariable("id") Long id) {
        return zoneService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ZoneResponse create(@Valid @RequestBody ZoneRequest request) {
        return zoneService.create(request);
    }

    @PutMapping("/{id}")
    public ZoneResponse update(
            @PathVariable("id") Long id,
            @Valid @RequestBody ZoneRequest request
    ) {
        return zoneService.update(id, request);
    }

    @PatchMapping("/{id}/deactivate")
    public ZoneResponse deactivate(@PathVariable("id") Long id) {
        return zoneService.deactivate(id);
    }
}
