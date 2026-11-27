package hu.unideb.inf.backend.service.task;

import hu.unideb.inf.backend.model.entity.DeliveryOrderEntity;
import hu.unideb.inf.backend.model.entity.DeliveryOrderLineEntity;
import hu.unideb.inf.backend.model.entity.LotEntity;
import hu.unideb.inf.backend.model.entity.ProductEntity;
import hu.unideb.inf.backend.model.entity.StockReservationEntity;
import hu.unideb.inf.backend.model.entity.TaskEntity;
import hu.unideb.inf.backend.model.entity.TaskLineEntity;
import hu.unideb.inf.backend.repository.DeliveryOrderRepository;
import hu.unideb.inf.backend.repository.LotRepository;
import hu.unideb.inf.backend.repository.ProductRepository;
import hu.unideb.inf.backend.repository.StockReservationRepository;
import hu.unideb.inf.backend.repository.TaskRepository;
import hu.unideb.inf.backend.service.masterdata.PageMapper;
import hu.unideb.inf.backend.service.reservation.StockReservationService;
import hu.unideb.inf.core.dto.DeliveryOrderCreateRequest;
import hu.unideb.inf.core.dto.DeliveryOrderItemRequest;
import hu.unideb.inf.core.dto.DeliveryOrderItemResponse;
import hu.unideb.inf.core.dto.DeliveryOrderResponse;
import hu.unideb.inf.core.dto.PageResponse;
import hu.unideb.inf.core.enums.DeliveryOrderStatus;
import hu.unideb.inf.core.enums.ReservationStatus;
import hu.unideb.inf.core.enums.TaskStatus;
import hu.unideb.inf.core.exception.DuplicateResourceException;
import hu.unideb.inf.core.exception.ResourceNotFoundException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class DeliveryOrderService {

    private static final String RESOURCE_TYPE = "DeliveryOrder";

    private final DeliveryOrderRepository deliveryOrderRepository;
    private final ProductRepository productRepository;
    private final LotRepository lotRepository;
    private final TaskRepository taskRepository;
    private final StockReservationRepository stockReservationRepository;
    private final StockReservationService stockReservationService;
    private final Clock clock;

    public DeliveryOrderService(
            DeliveryOrderRepository deliveryOrderRepository,
            ProductRepository productRepository,
            LotRepository lotRepository,
            TaskRepository taskRepository,
            StockReservationRepository stockReservationRepository,
            StockReservationService stockReservationService,
            Clock clock
    ) {
        this.deliveryOrderRepository = Objects.requireNonNull(
                deliveryOrderRepository,
                "deliveryOrderRepository must not be null"
        );
        this.productRepository = Objects.requireNonNull(productRepository, "productRepository must not be null");
        this.lotRepository = Objects.requireNonNull(lotRepository, "lotRepository must not be null");
        this.taskRepository = Objects.requireNonNull(taskRepository, "taskRepository must not be null");
        this.stockReservationRepository = Objects.requireNonNull(
                stockReservationRepository,
                "stockReservationRepository must not be null"
        );
        this.stockReservationService = Objects.requireNonNull(
                stockReservationService,
                "stockReservationService must not be null"
        );
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public PageResponse<DeliveryOrderResponse> findAll(
            DeliveryOrderStatus status,
            String customerName,
            Pageable pageable
    ) {
        String search = customerName == null ? null : customerName.trim();
        if (status == null && (search == null || search.isEmpty()) && pageable != null && pageable.isPaged()) {
            return PageMapper.fromPage(deliveryOrderRepository.findAll(pageable).map(this::toResponse));
        }

        List<DeliveryOrderEntity> candidates = status != null
                ? deliveryOrderRepository.findByStatus(status)
                : deliveryOrderRepository.findByCustomerNameContainingIgnoreCase(search);

        String lowered = search == null ? null : search.toLowerCase(Locale.ROOT);
        List<DeliveryOrderResponse> items = candidates.stream()
                .filter(order -> lowered == null || lowered.isEmpty()
                        || order.getCustomerName().toLowerCase(Locale.ROOT).contains(lowered))
                .map(this::toResponse)
                .toList();
        return PageMapper.paginate(items, pageable);
    }

    public DeliveryOrderResponse findById(Long id) {
        return toResponse(getOrderOrThrow(id));
    }

    @Transactional
    public DeliveryOrderResponse create(DeliveryOrderCreateRequest request, Long createdBy) {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(createdBy, "createdBy must not be null");

        String doNumber = request.doNumber().trim();
        if (deliveryOrderRepository.existsByDoNumber(doNumber)) {
            throw DuplicateResourceException.of(RESOURCE_TYPE, "doNumber", doNumber);
        }

        DeliveryOrderEntity order = new DeliveryOrderEntity(
                doNumber,
                request.customerName().trim(),
                DeliveryOrderStatus.OPEN,
                request.dueDate(),
                createdBy
        );
        order.setCreatedAt(Instant.now(clock));

        int lineNo = 1;
        for (DeliveryOrderItemRequest item : request.items()) {
            ProductEntity product = productRepository.findById(item.productId())
                    .filter(ProductEntity::isActive)
                    .orElseThrow(() -> ResourceNotFoundException.of("Product", item.productId()));

            LotEntity requestedLot = null;
            if (item.requestedLotId() != null) {
                requestedLot = lotRepository.findById(item.requestedLotId())
                        .orElseThrow(() -> ResourceNotFoundException.of("Lot", item.requestedLotId()));
            }

            order.addLine(new DeliveryOrderLineEntity(
                    order,
                    lineNo++,
                    product,
                    requestedLot,
                    item.requestedQuantity(),
                    0
            ));
        }

        return toResponse(deliveryOrderRepository.saveAndFlush(order));
    }

    @Transactional
    public DeliveryOrderResponse cancel(Long id, String reason) {
        DeliveryOrderEntity order = getOrderOrThrow(id);
        if (order.getStatus() == DeliveryOrderStatus.SHIPPED || order.getStatus() == DeliveryOrderStatus.CANCELLED) {
            throw new IllegalArgumentException(
                    "A(z) " + order.getStatus() + " státuszú kiszállítási megbízás nem vonható vissza."
            );
        }

        Instant now = Instant.now(clock);
        for (DeliveryOrderLineEntity line : order.getLines()) {
            for (StockReservationEntity reservation : stockReservationRepository.findByDeliveryOrderLineId(line.getId())) {
                if (reservation.getStatus() == ReservationStatus.ACTIVE) {
                    stockReservationService.release(reservation.getId());
                }
            }
        }

        for (TaskEntity task : taskRepository.findByDeliveryOrderId(order.getId())) {
            if (task.getStatus() == TaskStatus.PENDING || task.getStatus() == TaskStatus.IN_PROGRESS) {
                for (TaskLineEntity taskLine : task.getLines()) {
                    for (StockReservationEntity reservation : stockReservationRepository.findByTaskLineId(taskLine.getId())) {
                        if (reservation.getStatus() == ReservationStatus.ACTIVE) {
                            stockReservationService.release(reservation.getId());
                        }
                    }
                }
                task.setStatus(TaskStatus.CANCELLED);
                task.setCancelReason(reason != null && !reason.isBlank() ? reason.trim() : "Delivery order cancelled");
                task.setCancelledAt(now);
                taskRepository.saveAndFlush(task);
            }
        }

        order.setStatus(DeliveryOrderStatus.CANCELLED);
        return toResponse(deliveryOrderRepository.saveAndFlush(order));
    }

    public DeliveryOrderEntity getOrderOrThrow(Long id) {
        Objects.requireNonNull(id, "id must not be null");
        return deliveryOrderRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of(RESOURCE_TYPE, id));
    }

    DeliveryOrderResponse toResponse(DeliveryOrderEntity entity) {
        List<DeliveryOrderItemResponse> items = entity.getLines().stream()
                .map(this::toItemResponse)
                .toList();
        return new DeliveryOrderResponse(
                entity.getId(),
                entity.getDoNumber(),
                entity.getCustomerName(),
                entity.getStatus(),
                entity.getDueDate(),
                entity.getCreatedBy(),
                null,
                items,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private DeliveryOrderItemResponse toItemResponse(DeliveryOrderLineEntity line) {
        ProductEntity product = line.getProduct();
        Long lotId = line.getRequestedLot() != null ? line.getRequestedLot().getId() : null;
        return new DeliveryOrderItemResponse(
                line.getId(),
                product.getId(),
                product.getSku(),
                product.getName(),
                lotId,
                line.getQuantity(),
                line.getPickedQuantity()
        );
    }
}
