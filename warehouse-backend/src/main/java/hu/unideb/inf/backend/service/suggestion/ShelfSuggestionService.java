package hu.unideb.inf.backend.service.suggestion;

import hu.unideb.inf.backend.model.entity.InventoryItemEntity;
import hu.unideb.inf.backend.model.entity.ProductEntity;
import hu.unideb.inf.backend.model.entity.ShelfEntity;
import hu.unideb.inf.backend.repository.InventoryItemRepository;
import hu.unideb.inf.backend.repository.ProductRepository;
import hu.unideb.inf.backend.repository.ShelfRepository;
import hu.unideb.inf.core.dto.ShelfSuggestionResponse;
import hu.unideb.inf.core.enums.InventoryStatus;
import hu.unideb.inf.core.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ShelfSuggestionService {

    private final ProductRepository productRepository;
    private final ShelfRepository shelfRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final ShelfSuggestionEngine shelfSuggestionEngine;

    public ShelfSuggestionService(
            ProductRepository productRepository,
            ShelfRepository shelfRepository,
            InventoryItemRepository inventoryItemRepository,
            ShelfSuggestionEngine shelfSuggestionEngine
    ) {
        this.productRepository = Objects.requireNonNull(
                productRepository,
                "productRepository must not be null"
        );
        this.shelfRepository = Objects.requireNonNull(
                shelfRepository,
                "shelfRepository must not be null"
        );
        this.inventoryItemRepository = Objects.requireNonNull(
                inventoryItemRepository,
                "inventoryItemRepository must not be null"
        );
        this.shelfSuggestionEngine = Objects.requireNonNull(
                shelfSuggestionEngine,
                "shelfSuggestionEngine must not be null"
        );
    }

    public List<ShelfSuggestionResponse> suggest(
            Long productId,
            int quantity,
            Long warehouseId,
            Integer maxSuggestions
    ) {
        Objects.requireNonNull(productId, "productId must not be null");
        ProductEntity product = productRepository.findById(productId)
                .filter(ProductEntity::isActive)
                .orElseThrow(() -> ResourceNotFoundException.of("Product", productId));

        int limit = maxSuggestions != null
                ? maxSuggestions
                : ShelfSuggestionEngine.DEFAULT_MAX_SUGGESTIONS;

        List<ShelfEntity> shelves = warehouseId != null
                ? shelfRepository.findByZoneWarehouseId(warehouseId)
                : shelfRepository.findAll();

        Set<Long> sameProductShelfIds = inventoryItemRepository.findByLotProductId(productId)
                .stream()
                .filter(item -> item.getStatus() == InventoryStatus.STORED && item.getQuantity() > 0)
                .map(InventoryItemEntity::getShelf)
                .map(ShelfEntity::getId)
                .collect(Collectors.toSet());

        List<ShelfCandidate> candidates = shelves.stream()
                .map(shelf -> new ShelfCandidate(
                        shelf.getId(),
                        shelf.toShelfLoad(),
                        shelf.getZone().getZoneType(),
                        isShelfActive(shelf),
                        sameProductShelfIds.contains(shelf.getId())
                ))
                .toList();

        return shelfSuggestionEngine.suggest(
                        candidates,
                        product.getRequiredZoneType(),
                        product.toItemSize(),
                        quantity,
                        limit
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private boolean isShelfActive(ShelfEntity shelf) {
        return shelf.isActive()
                && shelf.getZone().isActive()
                && shelf.getZone().getWarehouse().isActive();
    }

    private ShelfSuggestionResponse toResponse(ShelfSuggestion suggestion) {
        return new ShelfSuggestionResponse(
                suggestion.shelfId(),
                suggestion.shelfCode(),
                suggestion.zoneType(),
                suggestion.freeWeightKg(),
                suggestion.freeVolumeM3()
        );
    }
}
