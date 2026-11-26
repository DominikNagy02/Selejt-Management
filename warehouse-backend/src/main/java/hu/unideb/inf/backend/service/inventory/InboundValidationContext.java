package hu.unideb.inf.backend.service.inventory;

import hu.unideb.inf.backend.model.entity.ProductEntity;
import hu.unideb.inf.backend.model.entity.ShelfEntity;
import hu.unideb.inf.backend.service.capacity.CapacityCheckResult;

import java.util.Objects;

public record InboundValidationContext(
        ProductEntity product,
        ShelfEntity shelf,
        int tierLevel,
        String batchNumber,
        CapacityCheckResult capacityResult
) {
    public InboundValidationContext {
        Objects.requireNonNull(product, "product must not be null");
        Objects.requireNonNull(shelf, "shelf must not be null");
        Objects.requireNonNull(batchNumber, "batchNumber must not be null");
        Objects.requireNonNull(capacityResult, "capacityResult must not be null");
    }
}
