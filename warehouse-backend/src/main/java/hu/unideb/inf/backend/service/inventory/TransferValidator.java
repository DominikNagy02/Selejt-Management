package hu.unideb.inf.backend.service.inventory;

import hu.unideb.inf.backend.model.entity.InventoryItemEntity;
import hu.unideb.inf.backend.model.entity.ProductEntity;
import hu.unideb.inf.backend.model.entity.ShelfEntity;
import hu.unideb.inf.backend.repository.InventoryItemRepository;
import hu.unideb.inf.backend.repository.ShelfRepository;
import hu.unideb.inf.backend.service.capacity.CapacityCheckResult;
import hu.unideb.inf.backend.service.capacity.CapacityValidator;
import hu.unideb.inf.core.dto.TransferRequest;
import hu.unideb.inf.core.enums.ZoneType;
import hu.unideb.inf.core.exception.InsufficientStockException;
import hu.unideb.inf.core.exception.InvalidTransferException;
import hu.unideb.inf.core.exception.ResourceNotFoundException;
import hu.unideb.inf.core.exception.ZoneMismatchException;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class TransferValidator {

    private final InventoryItemRepository inventoryItemRepository;
    private final ShelfRepository shelfRepository;
    private final CapacityValidator capacityValidator;

    public TransferValidator(
            InventoryItemRepository inventoryItemRepository,
            ShelfRepository shelfRepository,
            CapacityValidator capacityValidator
    ) {
        this.inventoryItemRepository = Objects.requireNonNull(
                inventoryItemRepository,
                "inventoryItemRepository must not be null"
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

    public TransferValidationContext validate(TransferRequest request) {
        Objects.requireNonNull(request, "request must not be null");

        InventoryItemEntity sourceItem = inventoryItemRepository.findById(request.inventoryId())
                .orElseThrow(() -> ResourceNotFoundException.of("InventoryItem", request.inventoryId()));

        ShelfEntity sourceShelf = sourceItem.getShelf();
        if (!sourceShelf.getId().equals(request.fromShelfId())
                || sourceItem.getTierLevel() != request.fromTierLevel()) {
            throw InvalidTransferException.sourceLocationMismatch(
                    sourceItem.getId(),
                    sourceShelf.getCode(),
                    sourceItem.getTierLevel()
            );
        }

        boolean sameShelf = sourceShelf.getId().equals(request.toShelfId());
        if (sameShelf && sourceItem.getTierLevel() == request.toTierLevel()) {
            throw InvalidTransferException.sameLocation(sourceShelf.getCode(), request.toTierLevel());
        }

        ProductEntity product = sourceItem.getLot().getProduct();
        int availableQuantity = sourceItem.getAvailableQuantity();
        if (request.quantity() > availableQuantity) {
            throw new InsufficientStockException(product.getSku(), request.quantity(), availableQuantity);
        }

        ShelfEntity targetShelf = sameShelf
                ? sourceShelf
                : shelfRepository.findById(request.toShelfId())
                        .orElseThrow(() -> ResourceNotFoundException.of("Shelf", request.toShelfId()));

        if (!isShelfActive(targetShelf)) {
            throw ResourceNotFoundException.of("Shelf", request.toShelfId());
        }

        int targetTierLevel = request.toTierLevel();
        if (targetTierLevel < 0 || targetTierLevel >= targetShelf.getMaxTiers()) {
            throw ResourceNotFoundException.of("ShelfTier", targetShelf.getCode() + "#" + targetTierLevel);
        }

        ZoneType targetZoneType = targetShelf.getZone().getZoneType();
        if (targetZoneType != product.getRequiredZoneType()) {
            throw new ZoneMismatchException(
                    product.getSku(),
                    product.getRequiredZoneType(),
                    targetShelf.getCode(),
                    targetZoneType
            );
        }

        CapacityCheckResult targetCapacityResult = sameShelf
                ? new CapacityCheckResult(
                        targetShelf.getCurrentWeightKg(),
                        targetShelf.getCurrentVolumeM3(),
                        true,
                        true
                )
                : capacityValidator.validate(
                        targetShelf.toShelfLoad(),
                        product.toItemSize(),
                        request.quantity()
                );

        return new TransferValidationContext(
                sourceItem,
                product,
                sourceShelf,
                targetShelf,
                targetTierLevel,
                sameShelf,
                targetCapacityResult
        );
    }

    private boolean isShelfActive(ShelfEntity shelf) {
        return shelf.isActive()
                && shelf.getZone().isActive()
                && shelf.getZone().getWarehouse().isActive();
    }
}
