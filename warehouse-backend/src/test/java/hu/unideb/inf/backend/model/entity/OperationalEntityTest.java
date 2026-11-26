package hu.unideb.inf.backend.model.entity;

import hu.unideb.inf.core.enums.DeliveryOrderStatus;
import hu.unideb.inf.core.enums.InventoryStatus;
import hu.unideb.inf.core.enums.MovementType;
import hu.unideb.inf.core.enums.ReservationStatus;
import hu.unideb.inf.core.enums.ReservationType;
import hu.unideb.inf.core.enums.StorageType;
import hu.unideb.inf.core.enums.TaskStatus;
import hu.unideb.inf.core.enums.TaskType;
import hu.unideb.inf.core.enums.ZoneType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperationalEntityTest {

    @Test
    @DisplayName("LotEntity metódusok, lifecycle és equals/hashCode")
    void testLotEntity() {
        ProductEntity product = new ProductEntity();
        product.setId(1L);

        LotEntity lot1 = new LotEntity();
        lot1.setId(10L);
        lot1.setProduct(product);
        lot1.setLotNumber("LOT-01");
        LocalDate exp = LocalDate.of(2027, 1, 1);
        lot1.setExpirationDate(exp);
        Instant now = Instant.now();
        lot1.setCreatedAt(now);

        assertEquals(10L, lot1.getId());
        assertEquals(product, lot1.getProduct());
        assertEquals("LOT-01", lot1.getLotNumber());
        assertEquals(exp, lot1.getExpirationDate());
        assertEquals(now, lot1.getCreatedAt());

        LotEntity lot2 = new LotEntity(product, "LOT-01", exp);
        LotEntity lotDiffNumber = new LotEntity(product, "LOT-02", exp);

        assertEquals(lot1, lot1);
        assertEquals(lot1, lot2);
        assertNotEquals(lot1, lotDiffNumber);
        assertNotEquals(lot1, null);
        assertNotEquals(lot1, "not-a-lot");
        assertEquals(lot1.hashCode(), lot2.hashCode());
        assertTrue(lot1.toString().contains("LOT-01"));

        lot2.setCreatedAt(null);
        lot2.onCreate();
        assertNotNull(lot2.getCreatedAt());
    }

    @Test
    @DisplayName("InventoryItemEntity metódusok, lifecycle és equals/hashCode")
    void testInventoryItemEntity() {
        ShelfEntity shelf = new ShelfEntity();
        shelf.setCode("S-01");
        LotEntity lot = new LotEntity();
        lot.setLotNumber("L-01");

        InventoryItemEntity item1 = new InventoryItemEntity();
        item1.setId(100L);
        item1.setShelf(shelf);
        item1.setLot(lot);
        item1.setTierLevel(1);
        item1.setQuantity(50);
        item1.setReservedQuantity(10);
        item1.setStatus(InventoryStatus.STORED);
        Instant now = Instant.now();
        item1.setReceivedAt(now);
        item1.setVersion(0L);
        item1.setUpdatedAt(now);

        assertEquals(100L, item1.getId());
        assertEquals(shelf, item1.getShelf());
        assertEquals(lot, item1.getLot());
        assertEquals(1, item1.getTierLevel());
        assertEquals(50, item1.getQuantity());
        assertEquals(10, item1.getReservedQuantity());
        assertEquals(InventoryStatus.STORED, item1.getStatus());
        assertEquals(now, item1.getReceivedAt());
        assertEquals(0L, item1.getVersion());
        assertEquals(now, item1.getUpdatedAt());

        InventoryItemEntity item2 = new InventoryItemEntity(shelf, lot, 1, 50, 10, InventoryStatus.STORED, now);
        InventoryItemEntity itemDiffTier = new InventoryItemEntity(shelf, lot, 2, 50, 10, InventoryStatus.STORED, now);

        assertEquals(item1, item1);
        assertEquals(item1, item2);
        assertNotEquals(item1, itemDiffTier);
        assertNotEquals(item1, null);
        assertNotEquals(item1, "other");
        assertEquals(item1.hashCode(), item2.hashCode());
        assertTrue(item1.toString().contains("tierLevel=1"));

        item2.setReceivedAt(null);
        item2.setStatus(null);
        item2.onCreate();
        assertNotNull(item2.getReceivedAt());
        assertEquals(InventoryStatus.STORED, item2.getStatus());
        item2.onUpdate();

        item1.setStatus(null);
        assertEquals(0, item1.getAvailableQuantity());
    }

    @Test
    @DisplayName("DeliveryOrderEntity és DeliveryOrderLineEntity metódusok")
    void testDeliveryOrderAndLines() {
        DeliveryOrderEntity order1 = new DeliveryOrderEntity();
        order1.setId(1L);
        order1.setDoNumber("DO-01");
        order1.setCustomerName("Vevő 1");
        order1.setStatus(DeliveryOrderStatus.OPEN);
        LocalDate dueDate = LocalDate.of(2026, 12, 1);
        order1.setDueDate(dueDate);
        order1.setCreatedBy(10L);
        order1.setVersion(0L);
        Instant now = Instant.now();
        order1.setCreatedAt(now);
        order1.setUpdatedAt(now);
        order1.setLines(new ArrayList<>());

        assertEquals(1L, order1.getId());
        assertEquals("DO-01", order1.getDoNumber());
        assertEquals("Vevő 1", order1.getCustomerName());
        assertEquals(DeliveryOrderStatus.OPEN, order1.getStatus());
        assertEquals(dueDate, order1.getDueDate());
        assertEquals(10L, order1.getCreatedBy());
        assertEquals(0L, order1.getVersion());
        assertEquals(now, order1.getCreatedAt());
        assertEquals(now, order1.getUpdatedAt());

        DeliveryOrderEntity order2 = new DeliveryOrderEntity("DO-01", "Vevő 2", DeliveryOrderStatus.OPEN, dueDate, 10L);
        DeliveryOrderEntity orderDiff = new DeliveryOrderEntity("DO-02", "Vevő 2", DeliveryOrderStatus.OPEN, dueDate, 10L);

        assertEquals(order1, order1);
        assertEquals(order1, order2);
        assertNotEquals(order1, orderDiff);
        assertNotEquals(order1, null);
        assertNotEquals(order1, "str");
        assertEquals(order1.hashCode(), order2.hashCode());
        assertTrue(order1.toString().contains("DO-01"));

        order2.setCreatedAt(null);
        order2.setStatus(null);
        order2.onCreate();
        assertNotNull(order2.getCreatedAt());
        assertEquals(DeliveryOrderStatus.OPEN, order2.getStatus());
        order2.onUpdate();

        DeliveryOrderLineEntity line = new DeliveryOrderLineEntity();
        line.setId(5L);
        line.setLineNo(1);
        line.setQuantity(10);
        line.setPickedQuantity(0);
        ProductEntity prod = new ProductEntity();
        line.setProduct(prod);
        LotEntity lot = new LotEntity();
        line.setRequestedLot(lot);

        order1.addLine(line);
        assertEquals(1, order1.getLines().size());
        assertEquals(order1, line.getDeliveryOrder());
        assertEquals(prod, line.getProduct());
        assertEquals(lot, line.getRequestedLot());
        assertEquals(10, line.getQuantity());
        assertEquals(0, line.getPickedQuantity());

        DeliveryOrderLineEntity line2 = new DeliveryOrderLineEntity(order1, 1, prod, lot, 10, 0);
        DeliveryOrderLineEntity lineDiffNo = new DeliveryOrderLineEntity(order1, 2, prod, lot, 10, 0);
        assertEquals(line, line);
        assertEquals(line, line2);
        assertNotEquals(line, lineDiffNo);
        assertNotEquals(line, null);
        assertNotEquals(line, "other");
        assertEquals(line.hashCode(), line2.hashCode());
        assertTrue(line.toString().contains("lineNo=1"));

        order1.removeLine(line);
        assertEquals(0, order1.getLines().size());
        assertNull(line.getDeliveryOrder());
    }

    @Test
    @DisplayName("TaskEntity és TaskLineEntity metódusok")
    void testTaskAndTaskLines() {
        TaskEntity task1 = new TaskEntity();
        task1.setId(200L);
        task1.setTaskType(TaskType.PICKING);
        task1.setStatus(TaskStatus.PENDING);
        task1.setPriority(2);
        DeliveryOrderEntity doEntity = new DeliveryOrderEntity();
        task1.setDeliveryOrder(doEntity);
        task1.setCreatedBy(10L);
        task1.setAssignedTo(11L);
        task1.setNote("Megjegyzés");
        task1.setCancelReason("Ok");
        Instant now = Instant.now();
        task1.setCreatedAt(now);
        task1.setStartedAt(now);
        task1.setCompletedAt(now);
        task1.setCancelledAt(now);
        task1.setVersion(0L);
        task1.setLines(new ArrayList<>());

        assertEquals(200L, task1.getId());
        assertEquals(TaskType.PICKING, task1.getTaskType());
        assertEquals(TaskStatus.PENDING, task1.getStatus());
        assertEquals(2, task1.getPriority());
        assertEquals(doEntity, task1.getDeliveryOrder());
        assertEquals(10L, task1.getCreatedBy());
        assertEquals(11L, task1.getAssignedTo());
        assertEquals("Megjegyzés", task1.getNote());
        assertEquals("Ok", task1.getCancelReason());
        assertEquals(now, task1.getCreatedAt());
        assertEquals(now, task1.getStartedAt());
        assertEquals(now, task1.getCompletedAt());
        assertEquals(now, task1.getCancelledAt());
        assertEquals(0L, task1.getVersion());

        TaskEntity task2 = new TaskEntity(TaskType.PICKING, TaskStatus.PENDING, 2, doEntity, 10L, 11L, "Megjegyzés");
        task2.setId(200L);
        TaskEntity taskDiff = new TaskEntity();
        taskDiff.setId(201L);

        assertEquals(task1, task1);
        assertEquals(task1, task2);
        assertNotEquals(task1, taskDiff);
        assertNotEquals(task1, null);
        assertNotEquals(task1, "other");
        assertEquals(task1.hashCode(), task2.hashCode());
        assertTrue(task1.toString().contains("PICKING"));

        task2.setCreatedAt(null);
        task2.setStatus(null);
        task2.onCreate();
        assertNotNull(task2.getCreatedAt());
        assertEquals(TaskStatus.PENDING, task2.getStatus());

        TaskLineEntity line = new TaskLineEntity();
        line.setId(50L);
        line.setLineNo(1);
        LotEntity lot = new LotEntity();
        line.setLot(lot);
        InventoryItemEntity item = new InventoryItemEntity();
        line.setSourceItem(item);
        ShelfEntity shelf = new ShelfEntity();
        line.setSourceShelf(shelf);
        line.setSourceTierLevel(0);
        line.setTargetShelf(shelf);
        line.setTargetTierLevel(1);
        DeliveryOrderLineEntity doLine = new DeliveryOrderLineEntity();
        line.setDeliveryOrderLine(doLine);
        line.setQuantity(20);
        line.setConfirmedQuantity(20);

        task1.addLine(line);
        assertEquals(1, task1.getLines().size());
        assertEquals(task1, line.getTask());
        assertEquals(lot, line.getLot());
        assertEquals(item, line.getSourceItem());
        assertEquals(shelf, line.getSourceShelf());
        assertEquals(0, line.getSourceTierLevel());
        assertEquals(shelf, line.getTargetShelf());
        assertEquals(1, line.getTargetTierLevel());
        assertEquals(doLine, line.getDeliveryOrderLine());
        assertEquals(20, line.getQuantity());
        assertEquals(20, line.getConfirmedQuantity());

        TaskLineEntity line2 = new TaskLineEntity(task1, 1, lot, item, shelf, 0, shelf, 1, doLine, 20, 20);
        TaskLineEntity lineDiffNo = new TaskLineEntity(task1, 2, lot, item, shelf, 0, shelf, 1, doLine, 20, 20);
        assertEquals(line, line);
        assertEquals(line, line2);
        assertNotEquals(line, lineDiffNo);
        assertNotEquals(line, null);
        assertNotEquals(line, "diff");
        assertEquals(line.hashCode(), line2.hashCode());
        assertTrue(line.toString().contains("quantity=20"));

        task1.removeLine(line);
        assertEquals(0, task1.getLines().size());
        assertNull(line.getTask());
    }

    @Test
    @DisplayName("StockReservationEntity metódusok")
    void testStockReservationEntity() {
        InventoryItemEntity item = new InventoryItemEntity();
        TaskLineEntity taskLine = new TaskLineEntity();
        DeliveryOrderLineEntity doLine = new DeliveryOrderLineEntity();
        Instant now = Instant.now();

        StockReservationEntity res1 = new StockReservationEntity();
        res1.setId(300L);
        res1.setInventoryItem(item);
        res1.setTaskLine(taskLine);
        res1.setDeliveryOrderLine(doLine);
        res1.setReservationType(ReservationType.PICKING);
        res1.setQuantity(15);
        res1.setStatus(ReservationStatus.ACTIVE);
        res1.setExpiresAt(now);
        res1.setCreatedBy(10L);
        res1.setCreatedAt(now);
        res1.setClosedAt(now);

        assertEquals(300L, res1.getId());
        assertEquals(item, res1.getInventoryItem());
        assertEquals(taskLine, res1.getTaskLine());
        assertEquals(doLine, res1.getDeliveryOrderLine());
        assertEquals(ReservationType.PICKING, res1.getReservationType());
        assertEquals(15, res1.getQuantity());
        assertEquals(ReservationStatus.ACTIVE, res1.getStatus());
        assertEquals(now, res1.getExpiresAt());
        assertEquals(10L, res1.getCreatedBy());
        assertEquals(now, res1.getCreatedAt());
        assertEquals(now, res1.getClosedAt());

        StockReservationEntity res2 = new StockReservationEntity(item, taskLine, doLine, ReservationType.PICKING, 15, ReservationStatus.ACTIVE, now, 10L);
        res2.setId(300L);
        StockReservationEntity resDiff = new StockReservationEntity();
        resDiff.setId(301L);

        assertEquals(res1, res1);
        assertEquals(res1, res2);
        assertNotEquals(res1, resDiff);
        assertNotEquals(res1, null);
        assertNotEquals(res1, "other");
        assertEquals(res1.hashCode(), res2.hashCode());
        assertTrue(res1.toString().contains("PICKING"));

        res2.setCreatedAt(null);
        res2.setStatus(null);
        res2.onCreate();
        assertNotNull(res2.getCreatedAt());
        assertEquals(ReservationStatus.ACTIVE, res2.getStatus());
    }

    @Test
    @DisplayName("StockMovementEntity és AuditEventEntity metódusok")
    void testMovementAndAuditEntities() {
        LotEntity lot = new LotEntity();
        InventoryItemEntity item = new InventoryItemEntity();
        ShelfEntity shelf = new ShelfEntity();
        TaskEntity task = new TaskEntity();
        DeliveryOrderEntity doEntity = new DeliveryOrderEntity();
        Instant now = Instant.now();

        StockMovementEntity mov1 = new StockMovementEntity();
        mov1.setId(400L);
        mov1.setMovementType(MovementType.TRANSFER);
        mov1.setLot(lot);
        mov1.setInventoryItem(item);
        mov1.setFromShelf(shelf);
        mov1.setFromTierLevel(0);
        mov1.setToShelf(shelf);
        mov1.setToTierLevel(1);
        mov1.setQuantity(5);
        mov1.setReason("Áthelyezés");
        mov1.setTask(task);
        mov1.setDeliveryOrder(doEntity);
        mov1.setPerformedBy(10L);
        mov1.setCreatedAt(now);

        assertEquals(400L, mov1.getId());
        assertEquals(MovementType.TRANSFER, mov1.getMovementType());
        assertEquals(lot, mov1.getLot());
        assertEquals(item, mov1.getInventoryItem());
        assertEquals(shelf, mov1.getFromShelf());
        assertEquals(0, mov1.getFromTierLevel());
        assertEquals(shelf, mov1.getToShelf());
        assertEquals(1, mov1.getToTierLevel());
        assertEquals(5, mov1.getQuantity());
        assertEquals("Áthelyezés", mov1.getReason());
        assertEquals(task, mov1.getTask());
        assertEquals(doEntity, mov1.getDeliveryOrder());
        assertEquals(10L, mov1.getPerformedBy());
        assertEquals(now, mov1.getCreatedAt());

        StockMovementEntity mov2 = new StockMovementEntity(MovementType.TRANSFER, lot, item, shelf, 0, shelf, 1, 5, "Áthelyezés", task, doEntity, 10L);
        mov2.setId(400L);
        StockMovementEntity movDiff = new StockMovementEntity();
        movDiff.setId(401L);

        assertEquals(mov1, mov1);
        assertEquals(mov1, mov2);
        assertNotEquals(mov1, movDiff);
        assertNotEquals(mov1, null);
        assertNotEquals(mov1, "other");
        assertEquals(mov1.hashCode(), mov2.hashCode());
        assertTrue(mov1.toString().contains("TRANSFER"));

        mov2.setCreatedAt(null);
        mov2.onCreate();
        assertNotNull(mov2.getCreatedAt());

        AuditEventEntity audit1 = new AuditEventEntity();
        audit1.setId(500L);
        audit1.setEntityType("TASK");
        audit1.setEntityId(200L);
        audit1.setAction("COMPLETE");
        audit1.setDetails("Task completed");
        audit1.setPerformedBy(10L);
        audit1.setCreatedAt(now);

        assertEquals(500L, audit1.getId());
        assertEquals("TASK", audit1.getEntityType());
        assertEquals(200L, audit1.getEntityId());
        assertEquals("COMPLETE", audit1.getAction());
        assertEquals("Task completed", audit1.getDetails());
        assertEquals(10L, audit1.getPerformedBy());
        assertEquals(now, audit1.getCreatedAt());

        AuditEventEntity audit2 = new AuditEventEntity("TASK", 200L, "COMPLETE", "Task completed", 10L);
        audit2.setId(500L);
        AuditEventEntity auditDiff = new AuditEventEntity();
        auditDiff.setId(501L);

        assertEquals(audit1, audit1);
        assertEquals(audit1, audit2);
        assertNotEquals(audit1, auditDiff);
        assertNotEquals(audit1, null);
        assertNotEquals(audit1, "other");
        assertEquals(audit1.hashCode(), audit2.hashCode());
        assertTrue(audit1.toString().contains("COMPLETE"));

        audit2.setCreatedAt(null);
        audit2.onCreate();
        assertNotNull(audit2.getCreatedAt());
    }
}
