package hu.unideb.inf.backend.service.inventory;

import hu.unideb.inf.backend.model.entity.ProductEntity;
import hu.unideb.inf.backend.model.entity.ShelfEntity;
import hu.unideb.inf.backend.repository.ProductRepository;
import hu.unideb.inf.backend.repository.ShelfRepository;
import hu.unideb.inf.backend.service.capacity.CapacityCheckResult;
import hu.unideb.inf.backend.service.capacity.CapacityValidator;
import hu.unideb.inf.core.dto.InboundRequest;
import hu.unideb.inf.core.enums.ZoneType;
import hu.unideb.inf.core.exception.ResourceNotFoundException;
import hu.unideb.inf.core.exception.ZoneMismatchException;
import hu.unideb.inf.shared.schemas.ValidationConstants;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class InboundValidator {

    private final ProductRepository productRepository;
    private final ShelfRepository shelfRepository;
    private final CapacityValidator capacityValidator;

    public InboundValidator(
            ProductRepository productRepository,
            ShelfRepository shelfRepository,
            CapacityValidator capacityValidator
    ) {
        this.productRepository = Objects.requireNonNull(
                productRepository,
                "productRepository must not be null"
        );
        this.shelfRepository = Objects.requireNonNull(
                shelfRepository,
                "shelfRepository must not be null"
        );
        this.capacityValidator = Objects.requireNonNull(
                capacityValidator,
                "capacityValidator must not be null"
        );
    }

    public InboundValidationContext validate(InboundRequest request) {
        Objects.requireNonNull(request, "request must not be null");

        ProductEntity product = productRepository.findById(request.productId())
                .filter(ProductEntity::isActive)
                .orElseThrow(() -> ResourceNotFoundException.of("Product", request.productId()));

        ShelfEntity shelf = shelfRepository.findById(request.shelfId())
                .filter(this::isShelfActive)
                .orElseThrow(() -> ResourceNotFoundException.of("Shelf", request.shelfId()));

        int tierLevel = request.tierLevel() != null
                ? request.tierLevel()
                : ValidationConstants.DEFAULT_TIER_LEVEL;
        if (tierLevel < 0 || tierLevel >= shelf.getMaxTiers()) {
            throw ResourceNotFoundException.of("ShelfTier", shelf.getCode() + "#" + tierLevel);
        }

        ZoneType actualZoneType = shelf.getZone().getZoneType();
        if (actualZoneType != product.getRequiredZoneType()) {
            throw new ZoneMismatchException(
                    product.getSku(),
                    product.getRequiredZoneType(),
                    shelf.getCode(),
                    actualZoneType
            );
        }

        CapacityCheckResult capacityResult = capacityValidator.validate(
                shelf.toShelfLoad(),
                product.toItemSize(),
                request.quantity()
        );

        String batchNumber = request.batchNumber().trim();
        return new InboundValidationContext(product, shelf, tierLevel, batchNumber, capacityResult);
    }

    private boolean isShelfActive(ShelfEntity shelf) {
        return shelf.isActive()
                && shelf.getZone().isActive()
                && shelf.getZone().getWarehouse().isActive();
    }
}
