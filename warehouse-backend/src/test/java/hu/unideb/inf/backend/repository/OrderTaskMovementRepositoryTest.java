package hu.unideb.inf.backend.repository;

import hu.unideb.inf.backend.model.entity.AuditEventEntity;
import hu.unideb.inf.backend.model.entity.DeliveryOrderEntity;
import hu.unideb.inf.backend.model.entity.DeliveryOrderLineEntity;
import hu.unideb.inf.backend.model.entity.InventoryItemEntity;
import hu.unideb.inf.backend.model.entity.LotEntity;
import hu.unideb.inf.backend.model.entity.ProductEntity;
import hu.unideb.inf.backend.model.entity.ShelfEntity;
import hu.unideb.inf.backend.model.entity.StockMovementEntity;
import hu.unideb.inf.backend.model.entity.StockReservationEntity;
import hu.unideb.inf.backend.model.entity.TaskEntity;
import hu.unideb.inf.backend.model.entity.TaskLineEntity;
import hu.unideb.inf.backend.model.entity.WarehouseEntity;
import hu.unideb.inf.backend.model.entity.ZoneEntity;
import hu.unideb.inf.core.enums.DeliveryOrderStatus;
import hu.unideb.inf.core.enums.InventoryStatus;
import hu.unideb.inf.core.enums.MovementType;
import hu.unideb.inf.core.enums.ReservationStatus;
import hu.unideb.inf.core.enums.ReservationType;
import hu.unideb.inf.core.enums.StorageType;
import hu.unideb.inf.core.enums.TaskStatus;
import hu.unideb.inf.core.enums.TaskType;
import hu.unideb.inf.core.enums.ZoneType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OrderTaskMovementRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private ShelfRepository shelfRepository;

    @Autowired
    private LotRepository lotRepository;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private StockReservationRepository stockReservationRepository;

    @Autowired
    private DeliveryOrderRepository deliveryOrderRepository;

    @Autowired
    private DeliveryOrderLineRepository deliveryOrderLineRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskLineRepository taskLineRepository;

    @Autowired
    private StockMovementRepository stockMovementRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    private Long testUserId;
    private ProductEntity product;
    private ShelfEntity shelfSource;
    private ShelfEntity shelfTarget;
    private LotEntity lot;
    private InventoryItemEntity inventoryItem;

    @BeforeEach
    void setUp() {
        entityManager.getEntityManager().createNativeQuery(
                "INSERT INTO users (username, password_hash, full_name, role, active, version, created_at, updated_at) " +
                        "VALUES ('dispatcher_c4', 'hash123', 'Dispatcher User', 'DISPATCHER', true, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
        ).executeUpdate();

        Number id = (Number) entityManager.getEntityManager().createNativeQuery(
                "SELECT id FROM users WHERE username = 'dispatcher_c4'"
        ).getSingleResult();
        testUserId = id.longValue();

        product = productRepository.saveAndFlush(new ProductEntity(
                "SKU-ORDER-01", "BAR-ORDER-01", "Rendelés Termék", "Leírás",
                new BigDecimal("2.000"), new BigDecimal("0.003000"), ZoneType.NORMAL, 5, true
        ));

        WarehouseEntity wh = new WarehouseEntity("WH-ORD", "Raktár Ord", "Cím", true);
        ZoneEntity zone = new ZoneEntity(wh, "Z-ORD", "Zóna Ord", ZoneType.NORMAL, true);
        shelfSource = new ShelfEntity(zone, "SH-SRC-01", StorageType.RACK, 2, new BigDecimal("2000.000"), new BigDecimal("10.000000"), true);
        shelfTarget = new ShelfEntity(zone, "SH-TGT-01", StorageType.RACK, 2, new BigDecimal("2000.000"), new BigDecimal("10.000000"), true);
        zone.addShelf(shelfSource);
        zone.addShelf(shelfTarget);
        wh.addZone(zone);
        warehouseRepository.saveAndFlush(wh);

        lot = lotRepository.saveAndFlush(new LotEntity(product, "LOT-ORD-01", LocalDate.now().plusMonths(6)));
        inventoryItem = inventoryItemRepository.saveAndFlush(new InventoryItemEntity(
                shelfSource, lot, 0, 100, 0, InventoryStatus.STORED, Instant.now()
        ));
    }

    @Test
    @DisplayName("C4.7 Kiszállítási megbízás -> Feladat -> Feladat-lépések -> Készletmozgás lánc és lekérdezések")
    void persistAndQueryDeliveryOrderTaskAndMovements() {
        DeliveryOrderEntity order = new DeliveryOrderEntity(
                "DO-2026-0001", "Partner Kft.", DeliveryOrderStatus.OPEN, LocalDate.now().plusDays(3), testUserId
        );
        DeliveryOrderLineEntity line1 = new DeliveryOrderLineEntity(order, 1, product, lot, 25, 0);
        order.addLine(line1);

        DeliveryOrderEntity savedOrder = deliveryOrderRepository.saveAndFlush(order);
        assertNotNull(savedOrder.getId());
        assertEquals(1, savedOrder.getLines().size());
        assertTrue(deliveryOrderRepository.existsByDoNumber("DO-2026-0001"));
        DeliveryOrderLineEntity savedLine = savedOrder.getLines().get(0);

        TaskEntity task = new TaskEntity(
                TaskType.PICKING, TaskStatus.PENDING, 2, savedOrder, testUserId, null, "Kigyűjtési feladat"
        );
        TaskLineEntity taskLine = new TaskLineEntity(
                task, 1, lot, inventoryItem, shelfSource, 0, null, null, savedLine, 25, 0
        );
        task.addLine(taskLine);

        TaskEntity savedTask = taskRepository.saveAndFlush(task);
        assertNotNull(savedTask.getId());
        assertEquals(1, savedTask.getLines().size());
        TaskLineEntity savedTaskLine = savedTask.getLines().get(0);

        StockReservationEntity reservation = new StockReservationEntity(
                inventoryItem, savedTaskLine, savedLine, ReservationType.PICKING, 25,
                ReservationStatus.ACTIVE, Instant.now().plus(30, ChronoUnit.MINUTES), testUserId
        );
        stockReservationRepository.saveAndFlush(reservation);

        StockMovementEntity movement = new StockMovementEntity(
                MovementType.PICKING, lot, inventoryItem, shelfSource, 0, null, null,
                25, "Kiszállítási kigyűjtés", savedTask, savedOrder, testUserId
        );
        StockMovementEntity savedMovement = stockMovementRepository.saveAndFlush(movement);
        assertNotNull(savedMovement.getId());
        assertNotNull(savedMovement.getCreatedAt());

        AuditEventEntity auditEvent = new AuditEventEntity(
                "DELIVERY_ORDER", savedOrder.getId(), "CREATE", "DO létrehozva", testUserId
        );
        AuditEventEntity savedAudit = auditEventRepository.saveAndFlush(auditEvent);
        assertNotNull(savedAudit.getId());

        // Lekérdezések tesztelése
        Optional<DeliveryOrderEntity> byDoNumber = deliveryOrderRepository.findByDoNumber("DO-2026-0001");
        assertTrue(byDoNumber.isPresent());
        assertEquals("Partner Kft.", byDoNumber.get().getCustomerName());
        assertFalse(deliveryOrderRepository.findByStatus(DeliveryOrderStatus.OPEN).isEmpty());
        assertFalse(deliveryOrderRepository.findByCustomerNameContainingIgnoreCase("partner").isEmpty());

        assertFalse(deliveryOrderLineRepository.findByDeliveryOrderId(savedOrder.getId()).isEmpty());
        assertTrue(deliveryOrderLineRepository.findByDeliveryOrderIdAndLineNo(savedOrder.getId(), 1).isPresent());

        assertFalse(taskRepository.findByStatus(TaskStatus.PENDING).isEmpty());
        assertFalse(taskRepository.findByStatusAndPriority(TaskStatus.PENDING, 2).isEmpty());
        assertFalse(taskRepository.findByDeliveryOrderId(savedOrder.getId()).isEmpty());
        assertFalse(taskRepository.findByTaskType(TaskType.PICKING).isEmpty());

        assertFalse(taskLineRepository.findByTaskId(savedTask.getId()).isEmpty());
        assertTrue(taskLineRepository.findByTaskIdAndLineNo(savedTask.getId(), 1).isPresent());

        assertFalse(stockMovementRepository.findByLotId(lot.getId()).isEmpty());
        assertFalse(stockMovementRepository.findByMovementType(MovementType.PICKING).isEmpty());
        assertFalse(stockMovementRepository.findByTaskId(savedTask.getId()).isEmpty());
        assertFalse(stockMovementRepository.findByDeliveryOrderId(savedOrder.getId()).isEmpty());

        assertFalse(auditEventRepository.findByEntityTypeAndEntityId("DELIVERY_ORDER", savedOrder.getId()).isEmpty());
        assertFalse(auditEventRepository.findByEntityType("DELIVERY_ORDER").isEmpty());
        assertFalse(auditEventRepository.findByPerformedBy(testUserId).isEmpty());
    }

    @Test
    @DisplayName("C4.7 DeliveryOrder és Task @Version növekedése módosításkor")
    void deliveryOrderAndTaskVersionIncrementsOnUpdate() {
        DeliveryOrderEntity order = deliveryOrderRepository.saveAndFlush(new DeliveryOrderEntity(
                "DO-VER-01", "Vevő", DeliveryOrderStatus.OPEN, null, testUserId
        ));
        assertEquals(0L, order.getVersion());

        order.setStatus(DeliveryOrderStatus.IN_PICKING);
        DeliveryOrderEntity updatedOrder = deliveryOrderRepository.saveAndFlush(order);
        assertEquals(1L, updatedOrder.getVersion());

        TaskEntity task = taskRepository.saveAndFlush(new TaskEntity(
                TaskType.TRANSFER, TaskStatus.PENDING, 3, null, testUserId, null, "Megjegyzés"
        ));
        assertEquals(0L, task.getVersion());

        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setStartedAt(Instant.now());
        TaskEntity updatedTask = taskRepository.saveAndFlush(task);
        assertEquals(1L, updatedTask.getVersion());
    }
}
