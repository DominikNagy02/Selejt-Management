package hu.unideb.inf.backend.service.inventory;

import hu.unideb.inf.backend.model.entity.InventoryItemEntity;
import hu.unideb.inf.backend.model.entity.ProductEntity;
import hu.unideb.inf.backend.model.entity.ShelfEntity;
import hu.unideb.inf.backend.service.capacity.CapacityCheckResult;

import java.util.Objects;

public record TransferValidationContext(
        InventoryItemEntity sourceItem,
        ProductEntity product,
        ShelfEntity sourceShelf,
        ShelfEntity targetShelf,
        int targetTierLevel,
        boolean sameShelf,
        CapacityCheckResult targetCapacityResult
) {
    public TransferValidationContext {
        Objects.requireNonNull(sourceItem, "sourceItem must not be null");
        Objects.requireNonNull(product, "product must not be null");
        Objects.requireNonNull(sourceShelf, "sourceShelf must not be null");
        Objects.requireNonNull(targetShelf, "targetShelf must not be null");
        Objects.requireNonNull(targetCapacityResult, "targetCapacityResult must not be null");
    }
}
