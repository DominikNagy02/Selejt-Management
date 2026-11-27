package hu.unideb.inf.backend.service.task;

import hu.unideb.inf.backend.model.entity.DeliveryOrderEntity;
import hu.unideb.inf.backend.model.entity.DeliveryOrderLineEntity;
import hu.unideb.inf.backend.model.entity.InventoryItemEntity;
import hu.unideb.inf.backend.model.entity.LotEntity;
import hu.unideb.inf.backend.model.entity.ProductEntity;
import hu.unideb.inf.backend.model.entity.ShelfEntity;
import hu.unideb.inf.backend.model.entity.StockReservationEntity;
import hu.unideb.inf.backend.model.entity.TaskEntity;
import hu.unideb.inf.backend.model.entity.TaskLineEntity;
import hu.unideb.inf.backend.repository.DeliveryOrderRepository;
import hu.unideb.inf.backend.repository.InventoryItemRepository;
import hu.unideb.inf.backend.repository.LotRepository;
import hu.unideb.inf.backend.repository.ProductRepository;
import hu.unideb.inf.backend.repository.ShelfRepository;
import hu.unideb.inf.backend.repository.StockReservationRepository;
import hu.unideb.inf.backend.repository.TaskLineRepository;
import hu.unideb.inf.backend.repository.TaskRepository;
import hu.unideb.inf.backend.service.inventory.InventoryService;
import hu.unideb.inf.backend.service.masterdata.PageMapper;
import hu.unideb.inf.backend.service.reservation.ReservationCommand;
import hu.unideb.inf.backend.service.reservation.StockReservationService;
import hu.unideb.inf.core.dto.InboundResponse;
import hu.unideb.inf.core.dto.PageResponse;
import hu.unideb.inf.core.dto.ScrapRequest;
import hu.unideb.inf.core.dto.TaskCompleteRequest;
import hu.unideb.inf.core.dto.TaskCreateRequest;
import hu.unideb.inf.core.dto.TaskLineRequest;
import hu.unideb.inf.core.dto.TaskLineResponse;
import hu.unideb.inf.core.dto.TaskResponse;
import hu.unideb.inf.core.dto.TransferRequest;
import hu.unideb.inf.core.enums.DeliveryOrderStatus;
import hu.unideb.inf.core.enums.ReservationStatus;
import hu.unideb.inf.core.enums.ReservationType;
import hu.unideb.inf.core.enums.Role;
import hu.unideb.inf.core.enums.TaskStatus;
import hu.unideb.inf.core.enums.TaskType;
import hu.unideb.inf.core.exception.ForbiddenOperationException;
import hu.unideb.inf.core.exception.ResourceNotFoundException;
import hu.unideb.inf.core.exception.ZoneMismatchException;
import hu.unideb.inf.shared.schemas.ValidationConstants;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class TaskService {

    private static final String RESOURCE_TYPE = "Task";

    private final TaskRepository taskRepository;
    private final TaskLineRepository taskLineRepository;
    private final DeliveryOrderRepository deliveryOrderRepository;
    private final ProductRepository productRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final LotRepository lotRepository;
    private final ShelfRepository shelfRepository;
    private final StockReservationRepository stockReservationRepository;
    private final StockReservationService stockReservationService;
    private final TaskStateMachine taskStateMachine;
    private final InventoryService inventoryService;
    private final Clock clock;

    public TaskService(
            TaskRepository taskRepository,
            TaskLineRepository taskLineRepository,
            DeliveryOrderRepository deliveryOrderRepository,
            ProductRepository productRepository,
            InventoryItemRepository inventoryItemRepository,
            LotRepository lotRepository,
            ShelfRepository shelfRepository,
            StockReservationRepository stockReservationRepository,
            StockReservationService stockReservationService,
            TaskStateMachine taskStateMachine,
            InventoryService inventoryService,
            Clock clock
    ) {
        this.taskRepository = Objects.requireNonNull(taskRepository, "taskRepository must not be null");
        this.taskLineRepository = Objects.requireNonNull(taskLineRepository, "taskLineRepository must not be null");
        this.deliveryOrderRepository = Objects.requireNonNull(
                deliveryOrderRepository,
                "deliveryOrderRepository must not be null"
        );
        this.productRepository = Objects.requireNonNull(productRepository, "productRepository must not be null");
        this.inventoryItemRepository = Objects.requireNonNull(
                inventoryItemRepository,
                "inventoryItemRepository must not be null"
        );
        this.lotRepository = Objects.requireNonNull(lotRepository, "lotRepository must not be null");
        this.shelfRepository = Objects.requireNonNull(shelfRepository, "shelfRepository must not be null");
        this.stockReservationRepository = Objects.requireNonNull(
                stockReservationRepository,
                "stockReservationRepository must not be null"
        );
        this.stockReservationService = Objects.requireNonNull(
                stockReservationService,
                "stockReservationService must not be null"
        );
        this.taskStateMachine = Objects.requireNonNull(taskStateMachine, "taskStateMachine must not be null");
        this.inventoryService = Objects.requireNonNull(inventoryService, "inventoryService must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public PageResponse<TaskResponse> findAll(
            TaskStatus status,
            TaskType taskType,
            Long deliveryOrderId,
            Long assignedTo,
            Pageable pageable
    ) {
        if (status == null && taskType == null && deliveryOrderId == null && assignedTo == null
                && pageable != null && pageable.isPaged()) {
            return PageMapper.fromPage(taskRepository.findAll(pageable).map(this::toResponse));
        }

        List<TaskResponse> items = taskRepository.findAll().stream()
                .filter(task -> status == null || task.getStatus() == status)
                .filter(task -> taskType == null || task.getTaskType() == taskType)
                .filter(task -> deliveryOrderId == null
                        || (task.getDeliveryOrder() != null
                        && task.getDeliveryOrder().getId().equals(deliveryOrderId)))
                .filter(task -> assignedTo == null || Objects.equals(task.getAssignedTo(), assignedTo))
                .map(this::toResponse)
                .toList();
        return PageMapper.paginate(items, pageable);
    }

    public PageResponse<TaskResponse> findMyTasks(Long workerId, TaskStatus status, Pageable pageable) {
        Objects.requireNonNull(workerId, "workerId must not be null");
        List<TaskResponse> items = taskRepository.findByAssignedToOrderByPriorityAscCreatedAtAsc(workerId).stream()
                .filter(task -> status != null
                        ? task.getStatus() == status
                        : (task.getStatus() == TaskStatus.PENDING || task.getStatus() == TaskStatus.IN_PROGRESS))
                .map(this::toResponse)
                .toList();
        return PageMapper.paginate(items, pageable);
    }

    public TaskResponse findById(Long id) {
        return toResponse(getTaskOrThrow(id));
    }

    @Transactional
    public TaskResponse create(TaskCreateRequest request, Long createdBy) {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(createdBy, "createdBy must not be null");

        ProductEntity product = productRepository.findById(request.productId())
                .filter(ProductEntity::isActive)
                .orElseThrow(() -> ResourceNotFoundException.of("Product", request.productId()));

        int priority = request.priority() != null ? request.priority() : ValidationConstants.DEFAULT_PRIORITY;
        DeliveryOrderEntity deliveryOrder = resolveDeliveryOrder(request);

        TaskEntity task = new TaskEntity(
                request.taskType(),
                TaskStatus.PENDING,
                priority,
                deliveryOrder,
                createdBy,
                request.assignedToId(),
                request.note()
        );
        task.setCreatedAt(Instant.now(clock));
        task = taskRepository.saveAndFlush(task);

        switch (request.taskType()) {
            case PICKING -> createPickingLines(task, product, deliveryOrder, request.quantity(), createdBy);
            case TRANSFER -> createTransferLines(task, product, request, createdBy);
            case PUTAWAY -> createPutawayLines(task, product, request);
            case SCRAP -> createScrapLines(task, request);
        }

        return toResponse(taskRepository.saveAndFlush(task));
    }

    @Transactional
    public TaskResponse accept(Long taskId, Long workerId) {
        Objects.requireNonNull(workerId, "workerId must not be null");
        TaskEntity task = getTaskOrThrow(taskId);
        taskStateMachine.transition(taskId, task.getStatus(), TaskStatus.IN_PROGRESS);
        if (task.getAssignedTo() != null && !task.getAssignedTo().equals(workerId)) {
            throw new ForbiddenOperationException("A feladat már másik raktároshoz van hozzárendelve.");
        }

        task.setAssignedTo(workerId);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setStartedAt(Instant.now(clock));
        return toResponse(taskRepository.saveAndFlush(task));
    }

    @Transactional
    public TaskResponse complete(Long taskId, TaskCompleteRequest request, Long actorId, Role actorRole) {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(actorId, "actorId must not be null");
        TaskEntity task = getTaskOrThrow(taskId);
        if (actorRole == Role.WORKER && !Objects.equals(task.getAssignedTo(), actorId)) {
            throw new ForbiddenOperationException("Csak a feladathoz kijelölt raktáros zárhatja le a feladatot.");
        }

        taskStateMachine.transition(taskId, task.getStatus(), TaskStatus.COMPLETED);
        verifyScannedShelfCode(task, request.scannedShelfCode().trim());

        for (TaskLineEntity line : task.getLines()) {
            switch (task.getTaskType()) {
                case PICKING -> completePickingLine(line);
                case TRANSFER -> completeTransferLine(line, request.note());
                case SCRAP -> completeScrapLine(line, request.note());
                case PUTAWAY -> line.setConfirmedQuantity(line.getQuantity());
            }
            taskLineRepository.saveAndFlush(line);
        }

        updateDeliveryOrderIfPicked(task.getDeliveryOrder());

        task.setStatus(TaskStatus.COMPLETED);
        task.setCompletedAt(Instant.now(clock));
        if (request.note() != null && !request.note().isBlank()) {
            task.setNote(request.note().trim());
        }
        return toResponse(taskRepository.saveAndFlush(task));
    }

    @Transactional
    public TaskResponse cancel(Long taskId, String reason, Long actorId, Role actorRole) {
        Objects.requireNonNull(actorId, "actorId must not be null");
        TaskEntity task = getTaskOrThrow(taskId);
        if (actorRole == Role.WORKER) {
            if (task.getStatus() != TaskStatus.IN_PROGRESS || !Objects.equals(task.getAssignedTo(), actorId)) {
                throw new ForbiddenOperationException(
                        "Raktáros csak saját, folyamatban lévő (IN_PROGRESS) feladatot szakíthat meg."
                );
            }
            if (reason == null || reason.isBlank()) {
                throw new IllegalArgumentException("Raktárosi megszakításhoz indoklás megadása kötelező.");
            }
        }

        taskStateMachine.transition(taskId, task.getStatus(), TaskStatus.CANCELLED);
        for (TaskLineEntity line : task.getLines()) {
            releaseActiveLineReservations(line);
        }

        task.setStatus(TaskStatus.CANCELLED);
        task.setCancelReason(reason != null && !reason.isBlank() ? reason.trim() : null);
        task.setCancelledAt(Instant.now(clock));
        return toResponse(taskRepository.saveAndFlush(task));
    }

    private void verifyScannedShelfCode(TaskEntity task, String scannedShelfCode) {
        boolean matched = task.getLines().stream()
                .map(line -> expectedShelfForCompletion(task.getTaskType(), line))
                .filter(Objects::nonNull)
                .anyMatch(shelf -> shelf.getCode().equalsIgnoreCase(scannedShelfCode));
        if (!matched) {
            throw new IllegalArgumentException(
                    "A beolvasott polckód (" + scannedShelfCode + ") nem egyezik a feladat polcával."
            );
        }
    }

    private ShelfEntity expectedShelfForCompletion(TaskType taskType, TaskLineEntity line) {
        return (taskType == TaskType.PICKING || taskType == TaskType.SCRAP)
                ? line.getSourceShelf()
                : line.getTargetShelf();
    }

    private void completePickingLine(TaskLineEntity line) {
        for (StockReservationEntity reservation : stockReservationRepository.findByTaskLineId(line.getId())) {
            if (reservation.getStatus() == ReservationStatus.ACTIVE) {
                stockReservationService.consume(reservation.getId());
            }
        }
        line.setConfirmedQuantity(line.getQuantity());
        DeliveryOrderLineEntity doLine = line.getDeliveryOrderLine();
        if (doLine != null) {
            doLine.setPickedQuantity(Math.min(doLine.getQuantity(), doLine.getPickedQuantity() + line.getQuantity()));
        }
    }

    private void completeTransferLine(TaskLineEntity line, String note) {
        releaseActiveLineReservations(line);

        InventoryItemEntity sourceItem = inventoryItemRepository.findById(line.getSourceItem().getId())
                .orElseThrow(() -> ResourceNotFoundException.of("InventoryItem", line.getSourceItem().getId()));

        List<StockReservationEntity> otherActiveReservations = List.of();
        int reboundQty = 0;
        if (line.getQuantity() == sourceItem.getQuantity() && sourceItem.getReservedQuantity() > 0) {
            otherActiveReservations = stockReservationRepository.findByInventoryItemIdAndStatus(
                    sourceItem.getId(),
                    ReservationStatus.ACTIVE
            );
            reboundQty = otherActiveReservations.stream().mapToInt(StockReservationEntity::getQuantity).sum();
            sourceItem.setReservedQuantity(Math.max(0, sourceItem.getReservedQuantity() - reboundQty));
            inventoryItemRepository.saveAndFlush(sourceItem);
        }

        InboundResponse moved = inventoryService.transfer(new TransferRequest(
                sourceItem.getId(),
                line.getSourceShelf().getId(),
                defaultTier(line.getSourceTierLevel()),
                line.getTargetShelf().getId(),
                defaultTier(line.getTargetTierLevel()),
                line.getQuantity(),
                note
        ));

        if (!otherActiveReservations.isEmpty()) {
            InventoryItemEntity targetItem = inventoryItemRepository.findById(moved.inventoryItemId())
                    .orElseThrow(() -> ResourceNotFoundException.of("InventoryItem", moved.inventoryItemId()));
            targetItem.setReservedQuantity(targetItem.getReservedQuantity() + reboundQty);
            inventoryItemRepository.saveAndFlush(targetItem);
            for (StockReservationEntity reservation : otherActiveReservations) {
                reservation.setInventoryItem(targetItem);
                stockReservationRepository.saveAndFlush(reservation);
            }
        }

        line.setConfirmedQuantity(line.getQuantity());
    }

    private void completeScrapLine(TaskLineEntity line, String note) {
        releaseActiveLineReservations(line);
        String reason = note != null && !note.isBlank() ? note.trim() : "Selejtezési feladat lezárva";
        inventoryService.scrap(new ScrapRequest(line.getSourceItem().getId(), line.getQuantity(), reason));
        line.setConfirmedQuantity(line.getQuantity());
    }

    private void releaseActiveLineReservations(TaskLineEntity line) {
        for (StockReservationEntity reservation : stockReservationRepository.findByTaskLineId(line.getId())) {
            if (reservation.getStatus() == ReservationStatus.ACTIVE) {
                stockReservationService.release(reservation.getId());
            }
        }
    }

    private void updateDeliveryOrderIfPicked(DeliveryOrderEntity order) {
        if (order == null || order.getLines().isEmpty()) {
            return;
        }
        boolean allPicked = order.getLines().stream()
                .allMatch(line -> line.getPickedQuantity() >= line.getQuantity());
        if (allPicked) {
            order.setStatus(DeliveryOrderStatus.PICKED);
            deliveryOrderRepository.saveAndFlush(order);
        }
    }

    private DeliveryOrderEntity resolveDeliveryOrder(TaskCreateRequest request) {
        if (request.taskType() == TaskType.PICKING && request.deliveryOrderId() == null) {
            throw new IllegalArgumentException("Kigyűjtési feladathoz a deliveryOrderId megadása kötelező.");
        }
        if (request.deliveryOrderId() == null) {
            return null;
        }
        return deliveryOrderRepository.findById(request.deliveryOrderId())
                .orElseThrow(() -> ResourceNotFoundException.of("DeliveryOrder", request.deliveryOrderId()));
    }

    private void createPickingLines(
            TaskEntity task,
            ProductEntity product,
            DeliveryOrderEntity deliveryOrder,
            int quantity,
            Long createdBy
    ) {
        DeliveryOrderLineEntity doLine = deliveryOrder.getLines().stream()
                .filter(line -> line.getProduct().getId().equals(product.getId()))
                .findFirst()
                .orElse(null);

        if (deliveryOrder.getStatus() == DeliveryOrderStatus.OPEN) {
            deliveryOrder.setStatus(DeliveryOrderStatus.IN_PICKING);
            deliveryOrderRepository.saveAndFlush(deliveryOrder);
        }

        List<StockReservationEntity> reservations = stockReservationService.reserve(
                ReservationCommand.forPicking(product.getId(), quantity, createdBy, null, null, doLine)
        );

        int lineNo = 1;
        for (StockReservationEntity reservation : reservations) {
            InventoryItemEntity item = reservation.getInventoryItem();
            TaskLineEntity savedLine = taskLineRepository.saveAndFlush(new TaskLineEntity(
                    task,
                    lineNo++,
                    item.getLot(),
                    item,
                    item.getShelf(),
                    item.getTierLevel(),
                    null,
                    null,
                    doLine,
                    reservation.getQuantity(),
                    0
            ));
            task.getLines().add(savedLine);
            reservation.setTaskLine(savedLine);
            stockReservationRepository.saveAndFlush(reservation);
        }
    }

    private void createTransferLines(
            TaskEntity task,
            ProductEntity product,
            TaskCreateRequest request,
            Long createdBy
    ) {
        List<TaskLineRequest> requestedLines = resolveLineRequests(request);
        int lineNo = 1;
        for (TaskLineRequest lineReq : requestedLines) {
            int targetTier = defaultTier(lineReq.toTierLevel());
            ShelfEntity targetShelf = resolveActiveShelfAndTier(lineReq.toShelfId(), targetTier, product);

            if (lineReq.inventoryId() != null) {
                InventoryItemEntity sourceItem = inventoryItemRepository.findById(lineReq.inventoryId())
                        .orElseThrow(() -> ResourceNotFoundException.of("InventoryItem", lineReq.inventoryId()));
                TaskLineEntity savedLine = taskLineRepository.saveAndFlush(new TaskLineEntity(
                        task,
                        lineNo++,
                        sourceItem.getLot(),
                        sourceItem,
                        sourceItem.getShelf(),
                        sourceItem.getTierLevel(),
                        targetShelf,
                        targetTier,
                        null,
                        lineReq.quantity(),
                        0
                ));
                task.getLines().add(savedLine);
                stockReservationService.reserveItem(
                        sourceItem,
                        lineReq.quantity(),
                        ReservationType.TRANSFER,
                        createdBy,
                        savedLine,
                        null
                );
            } else {
                List<StockReservationEntity> reservations = stockReservationService.reserve(
                        ReservationCommand.forTransfer(product.getId(), lineReq.quantity(), createdBy, null, null)
                );
                for (StockReservationEntity reservation : reservations) {
                    InventoryItemEntity sourceItem = reservation.getInventoryItem();
                    TaskLineEntity savedLine = taskLineRepository.saveAndFlush(new TaskLineEntity(
                            task,
                            lineNo++,
                            sourceItem.getLot(),
                            sourceItem,
                            sourceItem.getShelf(),
                            sourceItem.getTierLevel(),
                            targetShelf,
                            targetTier,
                            null,
                            reservation.getQuantity(),
                            0
                    ));
                    task.getLines().add(savedLine);
                    reservation.setTaskLine(savedLine);
                    stockReservationRepository.saveAndFlush(reservation);
                }
            }
        }
    }

    private void createPutawayLines(TaskEntity task, ProductEntity product, TaskCreateRequest request) {
        List<TaskLineRequest> requestedLines = resolveLineRequests(request);
        int lineNo = 1;
        for (TaskLineRequest lineReq : requestedLines) {
            int targetTier = defaultTier(lineReq.toTierLevel());
            ShelfEntity targetShelf = resolveActiveShelfAndTier(lineReq.toShelfId(), targetTier, product);
            LotEntity lot = resolveLot(lineReq, product);
            task.addLine(new TaskLineEntity(
                    task,
                    lineNo++,
                    lot,
                    null,
                    null,
                    null,
                    targetShelf,
                    targetTier,
                    null,
                    lineReq.quantity(),
                    0
            ));
        }
    }

    private void createScrapLines(TaskEntity task, TaskCreateRequest request) {
        List<TaskLineRequest> requestedLines = resolveLineRequests(request);
        int lineNo = 1;
        for (TaskLineRequest lineReq : requestedLines) {
            if (lineReq.inventoryId() == null) {
                throw new IllegalArgumentException("Selejtezési feladathoz az inventoryId megadása kötelező.");
            }
            InventoryItemEntity sourceItem = inventoryItemRepository.findById(lineReq.inventoryId())
                    .orElseThrow(() -> ResourceNotFoundException.of("InventoryItem", lineReq.inventoryId()));
            task.addLine(new TaskLineEntity(
                    task,
                    lineNo++,
                    sourceItem.getLot(),
                    sourceItem,
                    sourceItem.getShelf(),
                    sourceItem.getTierLevel(),
                    null,
                    null,
                    null,
                    lineReq.quantity(),
                    0
            ));
        }
    }

    private List<TaskLineRequest> resolveLineRequests(TaskCreateRequest request) {
        if (request.lines() != null && !request.lines().isEmpty()) {
            return request.lines();
        }
        return List.of(new TaskLineRequest(
                request.inventoryId(),
                null,
                request.fromShelfId(),
                request.fromTierLevel(),
                request.toShelfId(),
                request.toTierLevel(),
                request.quantity()
        ));
    }

    private ShelfEntity resolveActiveShelfAndTier(Long shelfId, int tierLevel, ProductEntity product) {
        if (shelfId == null) {
            throw new IllegalArgumentException("A célpolc (toShelfId) megadása kötelező.");
        }
        ShelfEntity shelf = shelfRepository.findById(shelfId)
                .filter(s -> s.isActive() && s.getZone().isActive() && s.getZone().getWarehouse().isActive())
                .orElseThrow(() -> ResourceNotFoundException.of("Shelf", shelfId));
        if (tierLevel < 0 || tierLevel >= shelf.getMaxTiers()) {
            throw ResourceNotFoundException.of("ShelfTier", shelf.getCode() + "#" + tierLevel);
        }
        if (shelf.getZone().getZoneType() != product.getRequiredZoneType()) {
            throw new ZoneMismatchException(
                    product.getSku(),
                    product.getRequiredZoneType(),
                    shelf.getCode(),
                    shelf.getZone().getZoneType()
            );
        }
        return shelf;
    }

    private LotEntity resolveLot(TaskLineRequest lineReq, ProductEntity product) {
        if (lineReq.lotId() != null) {
            return lotRepository.findById(lineReq.lotId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Lot", lineReq.lotId()));
        }
        if (lineReq.inventoryId() != null) {
            return inventoryItemRepository.findById(lineReq.inventoryId())
                    .orElseThrow(() -> ResourceNotFoundException.of("InventoryItem", lineReq.inventoryId()))
                    .getLot();
        }
        return lotRepository.findByProductId(product.getId()).stream()
                .findFirst()
                .orElseThrow(() -> ResourceNotFoundException.of("Lot", product.getSku()));
    }

    private int defaultTier(Integer tierLevel) {
        return tierLevel != null ? tierLevel : ValidationConstants.DEFAULT_TIER_LEVEL;
    }

    protected TaskEntity getTaskOrThrow(Long id) {
        Objects.requireNonNull(id, "id must not be null");
        return taskRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of(RESOURCE_TYPE, id));
    }

    TaskResponse toResponse(TaskEntity task) {
        List<TaskLineResponse> lineResponses = task.getLines().stream()
                .map(this::toLineResponse)
                .toList();
        TaskLineEntity firstLine = task.getLines().isEmpty() ? null : task.getLines().get(0);
        ProductEntity product = firstLine != null ? firstLine.getLot().getProduct() : null;
        InventoryItemEntity sourceItem = firstLine != null ? firstLine.getSourceItem() : null;
        ShelfEntity sourceShelf = firstLine != null ? firstLine.getSourceShelf() : null;
        ShelfEntity targetShelf = firstLine != null ? firstLine.getTargetShelf() : null;
        int totalQuantity = task.getLines().stream().mapToInt(TaskLineEntity::getQuantity).sum();
        DeliveryOrderEntity order = task.getDeliveryOrder();

        return new TaskResponse(
                task.getId(),
                task.getTaskType(),
                task.getStatus(),
                task.getPriority(),
                product != null ? product.getId() : null,
                product != null ? product.getSku() : null,
                sourceItem != null ? sourceItem.getId() : null,
                sourceShelf != null ? sourceShelf.getId() : null,
                sourceShelf != null ? sourceShelf.getCode() : null,
                firstLine != null ? firstLine.getSourceTierLevel() : null,
                targetShelf != null ? targetShelf.getId() : null,
                targetShelf != null ? targetShelf.getCode() : null,
                firstLine != null ? firstLine.getTargetTierLevel() : null,
                totalQuantity,
                order != null ? order.getId() : null,
                order != null ? order.getDoNumber() : null,
                task.getAssignedTo(),
                null,
                task.getCreatedBy(),
                null,
                task.getNote(),
                lineResponses,
                task.getCreatedAt(),
                task.getStartedAt(),
                task.getCompletedAt(),
                task.getCompletedAt() != null ? task.getCompletedAt() : task.getCreatedAt()
        );
    }

    private TaskLineResponse toLineResponse(TaskLineEntity line) {
        ShelfEntity sourceShelf = line.getSourceShelf();
        ShelfEntity targetShelf = line.getTargetShelf();
        return new TaskLineResponse(
                line.getId(),
                line.getSourceItem() != null ? line.getSourceItem().getId() : null,
                line.getLot().getId(),
                line.getLot().getLotNumber(),
                sourceShelf != null ? sourceShelf.getId() : null,
                sourceShelf != null ? sourceShelf.getCode() : null,
                line.getSourceTierLevel(),
                targetShelf != null ? targetShelf.getId() : null,
                targetShelf != null ? targetShelf.getCode() : null,
                line.getTargetTierLevel(),
                line.getQuantity(),
                line.getConfirmedQuantity()
        );
    }
}
