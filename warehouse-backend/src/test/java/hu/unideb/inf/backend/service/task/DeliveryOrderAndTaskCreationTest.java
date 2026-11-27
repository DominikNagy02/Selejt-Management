package hu.unideb.inf.backend.service.task;

import hu.unideb.inf.backend.model.entity.ProductEntity;
import hu.unideb.inf.backend.model.entity.ShelfEntity;
import hu.unideb.inf.backend.model.entity.WarehouseEntity;
import hu.unideb.inf.backend.model.entity.ZoneEntity;
import hu.unideb.inf.backend.repository.DeliveryOrderRepository;
import hu.unideb.inf.backend.repository.InventoryItemRepository;
import hu.unideb.inf.backend.repository.LotRepository;
import hu.unideb.inf.backend.repository.ProductRepository;
import hu.unideb.inf.backend.repository.ShelfRepository;
import hu.unideb.inf.backend.repository.StockReservationRepository;
import hu.unideb.inf.backend.repository.TaskRepository;
import hu.unideb.inf.backend.repository.WarehouseRepository;
import hu.unideb.inf.backend.repository.ZoneRepository;
import hu.unideb.inf.backend.service.inventory.InventoryService;
import hu.unideb.inf.backend.service.reservation.ReservationCommand;
import hu.unideb.inf.backend.service.reservation.StockReservationService;
import hu.unideb.inf.core.dto.DeliveryOrderCreateRequest;
import hu.unideb.inf.core.dto.DeliveryOrderItemRequest;
import hu.unideb.inf.core.dto.DeliveryOrderResponse;
import hu.unideb.inf.core.dto.InboundRequest;
import hu.unideb.inf.core.dto.InboundResponse;
import hu.unideb.inf.core.dto.PageResponse;
import hu.unideb.inf.core.dto.TaskCompleteRequest;
import hu.unideb.inf.core.dto.TaskCreateRequest;
import hu.unideb.inf.core.dto.TaskLineRequest;
import hu.unideb.inf.core.dto.TaskResponse;
import hu.unideb.inf.core.enums.DeliveryOrderStatus;
import hu.unideb.inf.core.enums.ReservationStatus;
import hu.unideb.inf.core.enums.Role;
import hu.unideb.inf.core.enums.StorageType;
import hu.unideb.inf.core.enums.TaskStatus;
import hu.unideb.inf.core.enums.TaskType;
import hu.unideb.inf.core.enums.ZoneType;
import hu.unideb.inf.core.exception.DuplicateResourceException;
import hu.unideb.inf.core.exception.ForbiddenOperationException;
import hu.unideb.inf.core.exception.InvalidTaskStateException;
import hu.unideb.inf.core.exception.ReservationConflictException;
import hu.unideb.inf.core.exception.ResourceNotFoundException;
import hu.unideb.inf.core.exception.ZoneMismatchException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(properties = "wms.scheduling.enabled=false")
@ActiveProfiles("test")
class DeliveryOrderAndTaskCreationTest {

    @Autowired
    private DeliveryOrderService deliveryOrderService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private StockReservationService stockReservationService;

    @Autowired
    private StockReservationRepository stockReservationRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private DeliveryOrderRepository deliveryOrderRepository;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private LotRepository lotRepository;

    @Autowired
    private ShelfRepository shelfRepository;

    @Autowired
    private ZoneRepository zoneRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long dispatcherId;
    private Long workerId;
    private ShelfEntity sourceShelf;
    private ShelfEntity targetShelf;
    private ShelfEntity chilledShelf;
    private ProductEntity product;

    @BeforeEach
    void setUp() {
        cleanUp();
        jdbcTemplate.update(
                "INSERT INTO users (username, password_hash, full_name, role, active, version, created_at, updated_at) "
                        + "VALUES ('f9_disp', 'hash', 'Dispatcher', 'DISPATCHER', true, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
        );
        jdbcTemplate.update(
                "INSERT INTO users (username, password_hash, full_name, role, active, version, created_at, updated_at) "
                        + "VALUES ('f9_worker', 'hash', 'Worker', 'WORKER', true, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
        );
        dispatcherId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = 'f9_disp'", Long.class);
        workerId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = 'f9_worker'", Long.class);

        WarehouseEntity wh = warehouseRepository.save(new WarehouseEntity("WH-F9-01", "Debrecen", "Cím", true));
        ZoneEntity normalZone = zoneRepository.save(new ZoneEntity(wh, "Z-F9-N", "Normál", ZoneType.NORMAL, true));
        ZoneEntity chilledZone = zoneRepository.save(new ZoneEntity(wh, "Z-F9-C", "Hűtött", ZoneType.CHILLED, true));
        sourceShelf = shelfRepository.save(new ShelfEntity(
                normalZone, "S-F9-01", StorageType.RACK, 3,
                new BigDecimal("200.000"), new BigDecimal("4.000000"), true
        ));
        targetShelf = shelfRepository.save(new ShelfEntity(
                normalZone, "S-F9-02", StorageType.RACK, 3,
                new BigDecimal("200.000"), new BigDecimal("4.000000"), true
        ));
        chilledShelf = shelfRepository.save(new ShelfEntity(
                chilledZone, "S-F9-03", StorageType.RACK, 2,
                new BigDecimal("200.000"), new BigDecimal("4.000000"), true
        ));
        product = productRepository.save(new ProductEntity(
                "SKU-F9-01", "5994000000001", "Kapcsoló", null,
                new BigDecimal("1.000"), new BigDecimal("0.010000"), ZoneType.NORMAL, 5, true
        ));
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    private void cleanUp() {
        stockReservationRepository.deleteAll();
        taskRepository.deleteAll();
        deliveryOrderRepository.deleteAll();
        inventoryItemRepository.deleteAll();
        lotRepository.deleteAll();
        shelfRepository.deleteAll();
        zoneRepository.deleteAll();
        warehouseRepository.deleteAll();
        productRepository.deleteAll();
        jdbcTemplate.update("DELETE FROM users WHERE username IN ('f9_disp', 'f9_worker')");
    }

    @Test
    void createsDeliveryOrderAndPickingTransferPutawayAndScrapTasks() {
        InboundResponse stock = inventoryService.receive(new InboundRequest(
                product.getId(), sourceShelf.getId(), 0, "BATCH-F9-1", LocalDate.of(2027, 12, 31), 20
        ));
        Long lotId = lotRepository.findByProductIdAndLotNumber(product.getId(), "BATCH-F9-1").orElseThrow().getId();

        DeliveryOrderResponse order = deliveryOrderService.create(new DeliveryOrderCreateRequest(
                "DO-2026-001",
                "Alfa Kft.",
                LocalDate.of(2026, 12, 20),
                List.of(new DeliveryOrderItemRequest(product.getId(), lotId, 5))
        ), dispatcherId);
        assertNotNull(order.id());
        assertEquals(DeliveryOrderStatus.OPEN, order.status());
        assertEquals(1, order.items().size());
        assertEquals(order.id(), deliveryOrderService.findById(order.id()).id());
        assertEquals(1, deliveryOrderService.findAll(null, null, PageRequest.of(0, 10)).totalElements());
        assertEquals(1, deliveryOrderService.findAll(DeliveryOrderStatus.OPEN, "alfa", Pageable.unpaged()).content().size());
        assertEquals(1, deliveryOrderService.findAll(null, "Alfa", PageRequest.of(0, 10)).content().size());

        assertThrows(DuplicateResourceException.class, () -> deliveryOrderService.create(new DeliveryOrderCreateRequest(
                "DO-2026-001", "Másik", null, List.of(new DeliveryOrderItemRequest(product.getId(), null, 1))
        ), dispatcherId));
        assertThrows(ResourceNotFoundException.class, () -> deliveryOrderService.findById(99999L));

        // PICKING task linked to DO -> creates reservation and transitions DO to IN_PICKING
        TaskResponse pickingTask = taskService.create(new TaskCreateRequest(
                TaskType.PICKING, 1, product.getId(), null, null, null, null, null,
                5, order.id(), workerId, "Sürgős kigyűjtés", null
        ), dispatcherId);
        assertEquals(TaskStatus.PENDING, pickingTask.status());
        assertEquals(1, pickingTask.priority());
        assertEquals(5, pickingTask.quantity());
        assertEquals(1, pickingTask.lines().size());
        assertEquals(DeliveryOrderStatus.IN_PICKING, deliveryOrderService.findById(order.id()).status());

        // TRANSFER task with explicit inventoryId
        TaskResponse transferExplicit = taskService.create(new TaskCreateRequest(
                TaskType.TRANSFER, 2, product.getId(), stock.inventoryItemId(),
                sourceShelf.getId(), 0, targetShelf.getId(), 1, 4, null, workerId, "Áthelyezés", null
        ), dispatcherId);
        assertEquals(TaskType.TRANSFER, transferExplicit.taskType());
        assertEquals("S-F9-02", transferExplicit.toShelfCode());
        assertEquals(1, transferExplicit.toTierLevel());

        // TRANSFER task without inventoryId (auto FIFO/FEFO allocation)
        TaskResponse transferAuto = taskService.create(new TaskCreateRequest(
                TaskType.TRANSFER, null, product.getId(), null,
                null, null, targetShelf.getId(), 2, 3, null, workerId, null, null
        ), dispatcherId);
        assertEquals(3, transferAuto.priority());
        assertEquals(3, transferAuto.quantity());

        // PUTAWAY task with explicit line
        TaskResponse putawayTask = taskService.create(new TaskCreateRequest(
                TaskType.PUTAWAY, 4, product.getId(), null, null, null, targetShelf.getId(), 0, 2, null, null, "Betárolás",
                List.of(new TaskLineRequest(null, lotId, null, null, targetShelf.getId(), 0, 2))
        ), dispatcherId);
        assertEquals(TaskType.PUTAWAY, putawayTask.taskType());
        assertEquals("S-F9-02", putawayTask.toShelfCode());

        // SCRAP task
        TaskResponse scrapTask = taskService.create(new TaskCreateRequest(
                TaskType.SCRAP, 5, product.getId(), stock.inventoryItemId(),
                sourceShelf.getId(), 0, null, null, 2, null, null, "Selejt", null
        ), dispatcherId);
        assertEquals(TaskType.SCRAP, scrapTask.taskType());

        // Queries & priority order for my-tasks
        assertEquals(5, taskService.findAll(null, null, null, null, PageRequest.of(0, 10)).totalElements());
        assertEquals(1, taskService.findAll(TaskStatus.PENDING, TaskType.PICKING, order.id(), workerId, PageRequest.of(0, 10)).content().size());
        PageResponse<TaskResponse> myTasks = taskService.findMyTasks(workerId, null, PageRequest.of(0, 10));
        assertEquals(3, myTasks.totalElements());
        assertEquals(1, myTasks.content().get(0).priority());
        assertEquals(2, myTasks.content().get(1).priority());
        assertEquals(3, myTasks.content().get(2).priority());
        assertEquals(3, taskService.findMyTasks(workerId, TaskStatus.PENDING, PageRequest.of(0, 10)).totalElements());
        assertEquals(pickingTask.id(), taskService.findById(pickingTask.id()).id());

        // Validation guards
        assertThrows(IllegalArgumentException.class, () -> taskService.create(new TaskCreateRequest(
                TaskType.PICKING, 1, product.getId(), null, null, null, null, null, 1, null, null, null, null
        ), dispatcherId));
        assertThrows(IllegalArgumentException.class, () -> taskService.create(new TaskCreateRequest(
                TaskType.TRANSFER, 2, product.getId(), stock.inventoryItemId(), null, null, null, null, 1, null, null, null, null
        ), dispatcherId));
        assertThrows(IllegalArgumentException.class, () -> taskService.create(new TaskCreateRequest(
                TaskType.SCRAP, 2, product.getId(), null, null, null, null, null, 1, null, null, null, null
        ), dispatcherId));
        assertThrows(ZoneMismatchException.class, () -> taskService.create(new TaskCreateRequest(
                TaskType.PUTAWAY, 3, product.getId(), stock.inventoryItemId(), null, null, chilledShelf.getId(), 0, 1, null, null, null, null
        ), dispatcherId));
        assertThrows(ResourceNotFoundException.class, () -> taskService.create(new TaskCreateRequest(
                TaskType.PUTAWAY, 3, product.getId(), null, null, null, targetShelf.getId(), 9, 1, null, null, null, null
        ), dispatcherId));
        assertThrows(ReservationConflictException.class, () -> taskService.create(new TaskCreateRequest(
                TaskType.TRANSFER, 2, product.getId(), stock.inventoryItemId(), sourceShelf.getId(), 0, targetShelf.getId(), 1, 50, null, null, null, null
        ), dispatcherId));
        assertThrows(ResourceNotFoundException.class, () -> taskService.findById(99999L));
    }

    @Test
    void acceptCompleteCancelAndDeliveryOrderCancelEnforceRulesAndRebindReservations() {
        InboundResponse stock = inventoryService.receive(new InboundRequest(
                product.getId(), sourceShelf.getId(), 0, "BATCH-LC-1", LocalDate.of(2027, 12, 31), 10
        ));

        DeliveryOrderResponse order = deliveryOrderService.create(new DeliveryOrderCreateRequest(
                "DO-2026-002",
                "Béta Zrt.",
                LocalDate.of(2026, 12, 25),
                List.of(new DeliveryOrderItemRequest(product.getId(), null, 4))
        ), dispatcherId);

        TaskResponse picking = taskService.create(new TaskCreateRequest(
                TaskType.PICKING, 1, product.getId(), null, null, null, null, null,
                4, order.id(), workerId, "Komissiózás", null
        ), dispatcherId);

        // Another worker cannot accept a task assigned to workerId
        assertThrows(ForbiddenOperationException.class, () -> taskService.accept(picking.id(), dispatcherId));
        // Worker cannot cancel PENDING task
        assertThrows(ForbiddenOperationException.class, () -> taskService.cancel(picking.id(), "Mégse", workerId, Role.WORKER));

        TaskResponse accepted = taskService.accept(picking.id(), workerId);
        assertEquals(TaskStatus.IN_PROGRESS, accepted.status());
        assertNotNull(accepted.startedAt());

        // Foreign worker cannot complete; wrong shelf code fails with 400
        assertThrows(ForbiddenOperationException.class, () -> taskService.complete(
                picking.id(), new TaskCompleteRequest("S-F9-01", 4, null), dispatcherId, Role.WORKER
        ));
        assertThrows(IllegalArgumentException.class, () -> taskService.complete(
                picking.id(), new TaskCompleteRequest("ROSSZ-POLC", 4, null), workerId, Role.WORKER
        ));
        // Worker cancel requires reason
        assertThrows(IllegalArgumentException.class, () -> taskService.cancel(picking.id(), "  ", workerId, Role.WORKER));

        TaskResponse completed = taskService.complete(
                picking.id(), new TaskCompleteRequest("S-F9-01", 4, "Rendben"), workerId, Role.WORKER
        );
        assertEquals(TaskStatus.COMPLETED, completed.status());
        assertNotNull(completed.completedAt());
        assertEquals(DeliveryOrderStatus.PICKED, deliveryOrderService.findById(order.id()).status());
        assertEquals(6, inventoryItemRepository.findById(stock.inventoryItemId()).orElseThrow().getQuantity());
        assertThrows(InvalidTaskStateException.class, () -> taskService.accept(picking.id(), workerId));

        // Transfer task moving full remaining item (6 units) with an active external picking reservation (2 units) -> rebinds!
        var externalRes = stockReservationService.reserve(
                ReservationCommand.forPicking(product.getId(), 2, dispatcherId, null, null, null)
        ).get(0);
        TaskResponse transfer = taskService.create(new TaskCreateRequest(
                TaskType.TRANSFER, 2, product.getId(), stock.inventoryItemId(),
                sourceShelf.getId(), 0, targetShelf.getId(), 1, 4, null, workerId, null, null
        ), dispatcherId);
        // Cancel by worker while IN_PROGRESS releases reservation
        taskService.accept(transfer.id(), workerId);
        TaskResponse cancelledTransfer = taskService.cancel(transfer.id(), "Targoncahiba", workerId, Role.WORKER);
        assertEquals(TaskStatus.CANCELLED, cancelledTransfer.status());
        assertEquals(2, inventoryItemRepository.findById(stock.inventoryItemId()).orElseThrow().getReservedQuantity());

        // Now create a full-item transfer (after releasing external reservation or keeping it during full move)
        stockReservationService.release(externalRes.getId());
        TaskResponse fullTransfer = taskService.create(new TaskCreateRequest(
                TaskType.TRANSFER, 2, product.getId(), stock.inventoryItemId(),
                sourceShelf.getId(), 0, targetShelf.getId(), 1, 6, null, null, null, null
        ), dispatcherId);
        taskService.accept(fullTransfer.id(), workerId);
        TaskResponse movedDone = taskService.complete(
                fullTransfer.id(), new TaskCompleteRequest("S-F9-02", 6, "Átrakva"), workerId, Role.WORKER
        );
        assertEquals(TaskStatus.COMPLETED, movedDone.status());
        assertEquals(0, inventoryItemRepository.findById(stock.inventoryItemId()).orElseThrow().getQuantity());

        // Scrap and Putaway task completion
        Long movedItemId = inventoryItemRepository.findByShelfId(targetShelf.getId()).get(0).getId();
        TaskResponse scrap = taskService.create(new TaskCreateRequest(
                TaskType.SCRAP, 3, product.getId(), movedItemId,
                targetShelf.getId(), 1, null, null, 2, null, workerId, null, null
        ), dispatcherId);
        taskService.accept(scrap.id(), workerId);
        assertEquals(TaskStatus.COMPLETED, taskService.complete(
                scrap.id(), new TaskCompleteRequest("S-F9-02", 2, "Sérült"), workerId, Role.WORKER
        ).status());
        assertEquals(4, inventoryItemRepository.findById(movedItemId).orElseThrow().getQuantity());

        TaskResponse putaway = taskService.create(new TaskCreateRequest(
                TaskType.PUTAWAY, 3, product.getId(), movedItemId,
                null, null, sourceShelf.getId(), 0, 1, null, workerId, null, null
        ), dispatcherId);
        taskService.accept(putaway.id(), workerId);
        assertEquals(TaskStatus.COMPLETED, taskService.complete(
                putaway.id(), new TaskCompleteRequest("S-F9-01", 1, null), workerId, Role.WORKER
        ).status());

        // DeliveryOrder cancel releases active reservations and cancels linked open tasks
        DeliveryOrderResponse orderToCancel = deliveryOrderService.create(new DeliveryOrderCreateRequest(
                "DO-2026-003", "Gamma Bt.", null,
                List.of(new DeliveryOrderItemRequest(product.getId(), null, 3))
        ), dispatcherId);
        TaskResponse openPick = taskService.create(new TaskCreateRequest(
                TaskType.PICKING, 1, product.getId(), null, null, null, null, null,
                3, orderToCancel.id(), null, null, null
        ), dispatcherId);
        assertEquals(3, inventoryItemRepository.findById(movedItemId).orElseThrow().getReservedQuantity());

        DeliveryOrderResponse cancelledOrder = deliveryOrderService.cancel(orderToCancel.id(), "Vevő lemondta");
        assertEquals(DeliveryOrderStatus.CANCELLED, cancelledOrder.status());
        assertEquals(TaskStatus.CANCELLED, taskService.findById(openPick.id()).status());
        assertEquals(0, inventoryItemRepository.findById(movedItemId).orElseThrow().getReservedQuantity());
        assertEquals(0, stockReservationRepository.findByInventoryItemIdAndStatus(movedItemId, ReservationStatus.ACTIVE).size());
        assertThrows(IllegalArgumentException.class, () -> deliveryOrderService.cancel(orderToCancel.id(), "Ismét"));
    }
}
