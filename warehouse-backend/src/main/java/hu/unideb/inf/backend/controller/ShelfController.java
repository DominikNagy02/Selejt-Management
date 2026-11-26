package hu.unideb.inf.backend.controller;

import hu.unideb.inf.backend.service.masterdata.ShelfService;
import hu.unideb.inf.core.dto.PageResponse;
import hu.unideb.inf.core.dto.ShelfRequest;
import hu.unideb.inf.core.dto.ShelfResponse;
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
@RequestMapping("/api/v1/shelves")
public class ShelfController {

    private final ShelfService shelfService;

    public ShelfController(ShelfService shelfService) {
        this.shelfService = Objects.requireNonNull(
                shelfService,
                "shelfService must not be null"
        );
    }

    @GetMapping
    public PageResponse<ShelfResponse> findAll(
            @RequestParam(name = "warehouseId", required = false) Long warehouseId,
            @RequestParam(name = "zoneId", required = false) Long zoneId,
            @RequestParam(name = "activeOnly", required = false) Boolean activeOnly,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return shelfService.findAll(warehouseId, zoneId, activeOnly, pageable);
    }

    @GetMapping("/{id}")
    public ShelfResponse findById(@PathVariable("id") Long id) {
        return shelfService.findById(id);
    }

    @GetMapping("/code/{code}")
    public ShelfResponse findByCode(@PathVariable("code") String code) {
        return shelfService.findByCode(code);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShelfResponse create(@Valid @RequestBody ShelfRequest request) {
        return shelfService.create(request);
    }

    @PutMapping("/{id}")
    public ShelfResponse update(
            @PathVariable("id") Long id,
            @Valid @RequestBody ShelfRequest request
    ) {
        return shelfService.update(id, request);
    }

    @PatchMapping("/{id}/deactivate")
    public ShelfResponse deactivate(@PathVariable("id") Long id) {
        return shelfService.deactivate(id);
    }
}
