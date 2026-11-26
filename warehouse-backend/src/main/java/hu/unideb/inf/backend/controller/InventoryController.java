package hu.unideb.inf.backend.controller;

import hu.unideb.inf.backend.service.inventory.InventoryService;
import hu.unideb.inf.backend.service.suggestion.ShelfSuggestionService;
import hu.unideb.inf.core.dto.InboundRequest;
import hu.unideb.inf.core.dto.InboundResponse;
import hu.unideb.inf.core.dto.PageResponse;
import hu.unideb.inf.core.dto.ShelfSuggestionResponse;
import hu.unideb.inf.core.dto.TransferRequest;
import hu.unideb.inf.core.enums.InventoryStatus;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryService inventoryService;
    private final ShelfSuggestionService shelfSuggestionService;

    public InventoryController(
            InventoryService inventoryService,
            ShelfSuggestionService shelfSuggestionService
    ) {
        this.inventoryService = Objects.requireNonNull(
                inventoryService,
                "inventoryService must not be null"
        );
        this.shelfSuggestionService = Objects.requireNonNull(
                shelfSuggestionService,
                "shelfSuggestionService must not be null"
        );
    }

    @GetMapping
    public PageResponse<InboundResponse> findAll(
            @RequestParam(name = "shelfId", required = false) Long shelfId,
            @RequestParam(name = "productId", required = false) Long productId,
            @RequestParam(name = "lotId", required = false) Long lotId,
            @RequestParam(name = "status", required = false) InventoryStatus status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return inventoryService.findAll(shelfId, productId, lotId, status, pageable);
    }

    @PostMapping("/inbound")
    @ResponseStatus(HttpStatus.CREATED)
    public InboundResponse receive(@Valid @RequestBody InboundRequest request) {
        return inventoryService.receive(request);
    }

    @PostMapping("/transfer")
    public InboundResponse transfer(@Valid @RequestBody TransferRequest request) {
        return inventoryService.transfer(request);
    }

    @GetMapping("/suggest-shelf")
    public List<ShelfSuggestionResponse> suggestShelf(
            @RequestParam(name = "productId") Long productId,
            @RequestParam(name = "quantity") int quantity,
            @RequestParam(name = "warehouseId", required = false) Long warehouseId,
            @RequestParam(name = "limit", required = false) Integer limit
    ) {
        return shelfSuggestionService.suggest(productId, quantity, warehouseId, limit);
    }
}
