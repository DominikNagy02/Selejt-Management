package hu.unideb.inf.backend.service.reservation;

import hu.unideb.inf.backend.config.WmsProperties;
import hu.unideb.inf.backend.model.entity.InventoryItemEntity;
import hu.unideb.inf.backend.model.entity.ProductEntity;
import hu.unideb.inf.backend.model.entity.ShelfEntity;
import hu.unideb.inf.backend.model.entity.StockReservationEntity;
import hu.unideb.inf.backend.repository.InventoryItemRepository;
import hu.unideb.inf.backend.repository.ProductRepository;
import hu.unideb.inf.backend.repository.ShelfRepository;
import hu.unideb.inf.backend.repository.StockReservationRepository;
import hu.unideb.inf.backend.service.picking.Allocation;
import hu.unideb.inf.backend.service.picking.PickingStrategy;
import hu.unideb.inf.backend.service.picking.StockLot;
import hu.unideb.inf.core.enums.InventoryStatus;
import hu.unideb.inf.core.enums.ReservationStatus;
import hu.unideb.inf.core.exception.InsufficientStockException;
import hu.unideb.inf.core.exception.ReservationConflictException;
import hu.unideb.inf.core.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class StockReservationService {

    private final ProductRepository productRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final ShelfRepository shelfRepository;
    private final StockReservationRepository stockReservationRepository;
    private final PickingStrategy pickingStrategy;
    private final WmsProperties wmsProperties;
    private final Clock clock;

    public StockReservationService(
            ProductRepository productRepository,
            InventoryItemRepository inventoryItemRepository,
            ShelfRepository shelfRepository,
            StockReservationRepository stockReservationRepository,
            PickingStrategy pickingStrategy,
            WmsProperties wmsProperties,
            Clock clock
    ) {
        this.productRepository = Objects.requireNonNull(productRepository, "productRepository must not be null");
        this.inventoryItemRepository = Objects.requireNonNull(
                inventoryItemRepository,
                "inventoryItemRepository must not be null"
        );
        this.shelfRepository = Objects.requireNonNull(shelfRepository, "shelfRepository must not be null");
        this.stockReservationRepository = Objects.requireNonNull(
                stockReservationRepository,
                "stockReservationRepository must not be null"
        );
        this.pickingStrategy = Objects.requireNonNull(pickingStrategy, "pickingStrategy must not be null");
        this.wmsProperties = Objects.requireNonNull(wmsProperties, "wmsProperties must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public int availableQuantity(InventoryItemEntity item) {
        if (item == null || item.getStatus() != InventoryStatus.STORED) {
            return 0;
        }
        return Math.max(0, item.getQuantity() - item.getReservedQuantity());
    }

    public int availableQuantityForProduct(Long productId, Long warehouseId) {
        Objects.requireNonNull(productId, "productId must not be null");
        return findEligibleItems(productId, warehouseId).stream()
                .mapToInt(this::availableQuantity)
                .sum();
    }

    @Transactional
    public List<StockReservationEntity> reserve(ReservationCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        ProductEntity product = productRepository.findById(command.productId())
                .filter(ProductEntity::isActive)
                .orElseThrow(() -> ResourceNotFoundException.of("Product", command.productId()));

        List<InventoryItemEntity> eligibleItems = findEligibleItems(product.getId(), command.warehouseId());
        Map<Long, InventoryItemEntity> itemsById = new LinkedHashMap<>();
        List<StockLot> stockLots = new ArrayList<>(eligibleItems.size());

        for (InventoryItemEntity item : eligibleItems) {
            int available = availableQuantity(item);
            itemsById.put(item.getId(), item);
            stockLots.add(new StockLot(
                    item.getId(),
                    item.getShelf().getCode(),
                    item.getTierLevel(),
                    item.getLot().getLotNumber(),
                    available,
                    item.getReceivedAt(),
                    item.getLot().getExpirationDate()
            ));
        }

        List<Allocation> allocations;
        try {
            allocations = pickingStrategy.allocate(stockLots, command.quantity());
        } catch (InsufficientStockException ex) {
            throw new InsufficientStockException(
                    product.getSku(),
                    ex.getRequestedQuantity(),
                    ex.getAvailableQuantity()
            );
        }

        Instant now = Instant.now(clock);
        Instant expiresAt = now.plus(Duration.ofMinutes(wmsProperties.reservation().timeoutMinutes()));

        List<StockReservationEntity> created = new ArrayList<>(allocations.size());
        for (Allocation allocation : allocations) {
            InventoryItemEntity item = itemsById.get(allocation.inventoryItemId());
            int available = availableQuantity(item);
            if (allocation.quantity() > available) {
                throw new ReservationConflictException(item.getId(), allocation.quantity(), available);
            }

            item.setReservedQuantity(item.getReservedQuantity() + allocation.quantity());
            inventoryItemRepository.save(item);

            StockReservationEntity reservation = new StockReservationEntity(
                    item,
                    command.taskLine(),
                    command.deliveryOrderLine(),
                    command.reservationType(),
                    allocation.quantity(),
                    ReservationStatus.ACTIVE,
                    expiresAt,
                    command.createdBy()
            );
            created.add(stockReservationRepository.save(reservation));
        }

        return created;
    }

    @Transactional
    public StockReservationEntity release(Long reservationId) {
        StockReservationEntity reservation = getActiveReservationOrThrow(reservationId);
        InventoryItemEntity item = reservation.getInventoryItem();
        int updatedReserved = Math.max(0, item.getReservedQuantity() - reservation.getQuantity());
        item.setReservedQuantity(updatedReserved);
        inventoryItemRepository.save(item);

        reservation.setStatus(ReservationStatus.RELEASED);
        reservation.setClosedAt(Instant.now(clock));
        return stockReservationRepository.save(reservation);
    }

    @Transactional
    public StockReservationEntity consume(Long reservationId) {
        StockReservationEntity reservation = getActiveReservationOrThrow(reservationId);
        InventoryItemEntity item = reservation.getInventoryItem();
        int quantity = reservation.getQuantity();
        if (item.getQuantity() < quantity || item.getReservedQuantity() < quantity) {
            throw new ReservationConflictException(item.getId(), quantity, availableQuantity(item));
        }

        int remainingQuantity = item.getQuantity() - quantity;
        item.setQuantity(remainingQuantity);
        item.setReservedQuantity(item.getReservedQuantity() - quantity);
        if (remainingQuantity == 0) {
            item.setStatus(InventoryStatus.DEPLETED);
        }
        inventoryItemRepository.save(item);

        ProductEntity product = item.getLot().getProduct();
        ShelfEntity shelf = item.getShelf();
        BigDecimal removedWeight = product.getUnitWeightKg().multiply(BigDecimal.valueOf(quantity));
        BigDecimal removedVolume = product.getUnitVolumeM3().multiply(BigDecimal.valueOf(quantity));
        shelf.setCurrentWeightKg(shelf.getCurrentWeightKg().subtract(removedWeight).max(BigDecimal.ZERO));
        shelf.setCurrentVolumeM3(shelf.getCurrentVolumeM3().subtract(removedVolume).max(BigDecimal.ZERO));
        shelfRepository.save(shelf);

        reservation.setStatus(ReservationStatus.CONSUMED);
        reservation.setClosedAt(Instant.now(clock));
        return stockReservationRepository.save(reservation);
    }

    private StockReservationEntity getActiveReservationOrThrow(Long reservationId) {
        Objects.requireNonNull(reservationId, "reservationId must not be null");
        StockReservationEntity reservation = stockReservationRepository.findById(reservationId)
                .orElseThrow(() -> ResourceNotFoundException.of("StockReservation", reservationId));
        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            throw new ReservationConflictException(
                    reservation.getInventoryItem().getId(),
                    reservation.getQuantity(),
                    0
            );
        }
        return reservation;
    }

    private List<InventoryItemEntity> findEligibleItems(Long productId, Long warehouseId) {
        return inventoryItemRepository.findByLotProductId(productId).stream()
                .filter(item -> availableQuantity(item) > 0)
                .filter(item -> isShelfActive(item.getShelf()))
                .filter(item -> warehouseId == null
                        || item.getShelf().getZone().getWarehouse().getId().equals(warehouseId))
                .toList();
    }

    private boolean isShelfActive(ShelfEntity shelf) {
        return shelf.isActive()
                && shelf.getZone().isActive()
                && shelf.getZone().getWarehouse().isActive();
    }
}
