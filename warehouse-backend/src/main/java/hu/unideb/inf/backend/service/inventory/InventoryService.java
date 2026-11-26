package hu.unideb.inf.backend.service.inventory;

import hu.unideb.inf.backend.model.entity.InventoryItemEntity;
import hu.unideb.inf.backend.model.entity.LotEntity;
import hu.unideb.inf.backend.model.entity.ProductEntity;
import hu.unideb.inf.backend.model.entity.ShelfEntity;
import hu.unideb.inf.backend.repository.InventoryItemRepository;
import hu.unideb.inf.backend.repository.LotRepository;
import hu.unideb.inf.backend.repository.ShelfRepository;
import hu.unideb.inf.backend.service.masterdata.PageMapper;
import hu.unideb.inf.core.dto.InboundRequest;
import hu.unideb.inf.core.dto.InboundResponse;
import hu.unideb.inf.core.dto.PageResponse;
import hu.unideb.inf.core.enums.InventoryStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class InventoryService {

    private final InboundValidator inboundValidator;
    private final LotRepository lotRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final ShelfRepository shelfRepository;
    private final Clock clock;

    public InventoryService(
            InboundValidator inboundValidator,
            LotRepository lotRepository,
            InventoryItemRepository inventoryItemRepository,
            ShelfRepository shelfRepository,
            Clock clock
    ) {
        this.inboundValidator = Objects.requireNonNull(
                inboundValidator,
                "inboundValidator must not be null"
        );
        this.lotRepository = Objects.requireNonNull(
                lotRepository,
                "lotRepository must not be null"
        );
        this.inventoryItemRepository = Objects.requireNonNull(
                inventoryItemRepository,
                "inventoryItemRepository must not be null"
        );
        this.shelfRepository = Objects.requireNonNull(
                shelfRepository,
                "shelfRepository must not be null"
        );
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public PageResponse<InboundResponse> findAll(
            Long shelfId,
            Long productId,
            Long lotId,
            InventoryStatus status,
            Pageable pageable
    ) {
        if (shelfId == null && productId == null && lotId == null && status == null
                && pageable != null && pageable.isPaged()) {
            return PageMapper.fromPage(inventoryItemRepository.findAll(pageable).map(this::toResponse));
        }

        List<InventoryItemEntity> candidates;
        if (lotId != null && status != null) {
            candidates = inventoryItemRepository.findByStatusAndLotId(status, lotId);
        } else if (lotId != null) {
            candidates = inventoryItemRepository.findByLotId(lotId);
        } else if (shelfId != null) {
            candidates = inventoryItemRepository.findByShelfId(shelfId);
        } else if (productId != null) {
            candidates = inventoryItemRepository.findByLotProductId(productId);
        } else {
            candidates = inventoryItemRepository.findAll();
        }

        List<InboundResponse> items = candidates.stream()
                .filter(item -> shelfId == null || item.getShelf().getId().equals(shelfId))
                .filter(item -> productId == null || item.getLot().getProduct().getId().equals(productId))
                .filter(item -> lotId == null || item.getLot().getId().equals(lotId))
                .filter(item -> status == null || item.getStatus() == status)
                .map(this::toResponse)
                .toList();
        return PageMapper.paginate(items, pageable);
    }

    @Transactional
    public InboundResponse receive(InboundRequest request) {
        InboundValidationContext context = inboundValidator.validate(request);
        Instant now = Instant.now(clock);

        ProductEntity product = context.product();
        ShelfEntity shelf = context.shelf();
        int tierLevel = context.tierLevel();

        LotEntity lot = upsertLot(product, context.batchNumber(), request.expirationDate());
        InventoryItemEntity savedItem = upsertInventoryItem(shelf, lot, tierLevel, request.quantity(), now);

        shelf.setCurrentWeightKg(context.capacityResult().newWeight());
        shelf.setCurrentVolumeM3(context.capacityResult().newVolume());
        ShelfEntity savedShelf = shelfRepository.save(shelf);

        return new InboundResponse(
                savedItem.getId(),
                product.getId(),
                product.getSku(),
                product.getName(),
                savedShelf.getId(),
                savedShelf.getCode(),
                savedItem.getTierLevel(),
                lot.getLotNumber(),
                lot.getExpirationDate(),
                savedItem.getQuantity(),
                savedShelf.getCurrentWeightKg(),
                savedShelf.getCurrentVolumeM3(),
                savedItem.getStatus(),
                now
        );
    }

    private LotEntity upsertLot(ProductEntity product, String batchNumber, LocalDate expirationDate) {
        return lotRepository.findByProductIdAndLotNumber(product.getId(), batchNumber)
                .map(existing -> {
                    if (existing.getExpirationDate() == null && expirationDate != null) {
                        existing.setExpirationDate(expirationDate);
                        return lotRepository.save(existing);
                    }
                    return existing;
                })
                .orElseGet(() -> lotRepository.save(new LotEntity(product, batchNumber, expirationDate)));
    }

    private InventoryItemEntity upsertInventoryItem(
            ShelfEntity shelf,
            LotEntity lot,
            int tierLevel,
            int incomingQuantity,
            Instant now
    ) {
        InventoryItemEntity item = inventoryItemRepository
                .findByShelfIdAndLotIdAndTierLevel(shelf.getId(), lot.getId(), tierLevel)
                .map(existing -> mergeQuantity(existing, incomingQuantity, now))
                .orElseGet(() -> new InventoryItemEntity(
                        shelf,
                        lot,
                        tierLevel,
                        incomingQuantity,
                        0,
                        InventoryStatus.STORED,
                        now
                ));
        return inventoryItemRepository.save(item);
    }

    private InventoryItemEntity mergeQuantity(
            InventoryItemEntity existing,
            int incomingQuantity,
            Instant now
    ) {
        if (existing.getStatus() == InventoryStatus.DEPLETED) {
            existing.setStatus(InventoryStatus.STORED);
            existing.setQuantity(incomingQuantity);
            existing.setReservedQuantity(0);
            existing.setReceivedAt(now);
        } else {
            existing.setQuantity(existing.getQuantity() + incomingQuantity);
        }
        return existing;
    }

    private InboundResponse toResponse(InventoryItemEntity item) {
        LotEntity lot = item.getLot();
        ProductEntity product = lot.getProduct();
        ShelfEntity shelf = item.getShelf();
        return new InboundResponse(
                item.getId(),
                product.getId(),
                product.getSku(),
                product.getName(),
                shelf.getId(),
                shelf.getCode(),
                item.getTierLevel(),
                lot.getLotNumber(),
                lot.getExpirationDate(),
                item.getQuantity(),
                shelf.getCurrentWeightKg(),
                shelf.getCurrentVolumeM3(),
                item.getStatus(),
                item.getReceivedAt()
        );
    }
}
