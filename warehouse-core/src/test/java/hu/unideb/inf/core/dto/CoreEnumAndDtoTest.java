package hu.unideb.inf.core.dto;

import hu.unideb.inf.core.enums.DeliveryOrderStatus;
import hu.unideb.inf.core.enums.InventoryStatus;
import hu.unideb.inf.core.enums.MovementType;
import hu.unideb.inf.core.enums.ReservationStatus;
import hu.unideb.inf.core.enums.ReservationType;
import hu.unideb.inf.core.enums.Role;
import hu.unideb.inf.core.enums.StorageType;
import hu.unideb.inf.core.enums.TaskStatus;
import hu.unideb.inf.core.enums.TaskType;
import hu.unideb.inf.core.enums.ZoneType;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreEnumAndDtoTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    private static void assertRecordBasics(Object same1, Object same2, Object different) {
        assertEquals(same1, same1);
        assertEquals(same1, same2);
        assertEquals(same1.hashCode(), same2.hashCode());
        assertNotEquals(same1, different);
        assertNotEquals(same1, null);
        assertNotEquals(same1, "other-type");
        assertNotNull(same1.toString());
    }

    @Test
    void allCoreEnumsContainExpectedConstants() {
        assertEquals(Role.ADMIN, Role.valueOf("ADMIN"));
        assertEquals(3, Role.values().length);

        assertEquals(ZoneType.NORMAL, ZoneType.valueOf("NORMAL"));
        assertEquals(3, ZoneType.values().length);

        assertEquals(MovementType.INBOUND, MovementType.valueOf("INBOUND"));
        assertEquals(5, MovementType.values().length);

        assertEquals(TaskType.PICKING, TaskType.valueOf("PICKING"));
        assertEquals(4, TaskType.values().length);

        assertEquals(StorageType.RACK, StorageType.valueOf("RACK"));
        assertEquals(2, StorageType.values().length);

        assertEquals(InventoryStatus.STORED, InventoryStatus.valueOf("STORED"));
        assertEquals(3, InventoryStatus.values().length);

        assertEquals(ReservationType.PICKING, ReservationType.valueOf("PICKING"));
        assertEquals(2, ReservationType.values().length);

        assertEquals(ReservationStatus.ACTIVE, ReservationStatus.valueOf("ACTIVE"));
        assertEquals(4, ReservationStatus.values().length);

        assertEquals(DeliveryOrderStatus.OPEN, DeliveryOrderStatus.valueOf("OPEN"));
        assertEquals(5, DeliveryOrderStatus.values().length);

        assertEquals(TaskStatus.PENDING, TaskStatus.valueOf("PENDING"));
        assertEquals(4, TaskStatus.values().length);
    }

    @Test
    void warehouseZoneShelfAndProductDtosValidateAndBehaveCorrectly() {
        Instant now = Instant.now();

        var whReq1 = new WarehouseRequest("WH-01", "Központi Raktár", "Debrecen", true);
        var whReq2 = new WarehouseRequest("WH-01", "Központi Raktár", "Debrecen", true);
        var whReqDiff = new WarehouseRequest("WH-02", "Másik", "Budapest", false);
        assertTrue(validator.validate(whReq1).isEmpty());
        assertRecordBasics(whReq1, whReq2, whReqDiff);

        var whRes1 = new WarehouseResponse(1L, "WH-01", "Központi Raktár", "Debrecen", true, now, now);
        var whRes2 = new WarehouseResponse(1L, "WH-01", "Központi Raktár", "Debrecen", true, now, now);
        var whResDiff = new WarehouseResponse(2L, "WH-02", "Másik", "Budapest", false, now, now);
        assertEquals("WH-01", whRes1.code());
        assertRecordBasics(whRes1, whRes2, whResDiff);

        var zoneReq1 = new ZoneRequest(1L, "Z-01", "Normál Zóna", ZoneType.NORMAL, true);
        var zoneReq2 = new ZoneRequest(1L, "Z-01", "Normál Zóna", ZoneType.NORMAL, true);
        var zoneReqDiff = new ZoneRequest(2L, "Z-02", "Hűtött", ZoneType.CHILLED, false);
        assertTrue(validator.validate(zoneReq1).isEmpty());
        assertRecordBasics(zoneReq1, zoneReq2, zoneReqDiff);

        var zoneRes1 = new ZoneResponse(1L, 1L, "Z-01", "Normál Zóna", ZoneType.NORMAL, true, now, now);
        var zoneRes2 = new ZoneResponse(1L, 1L, "Z-01", "Normál Zóna", ZoneType.NORMAL, true, now, now);
        var zoneResDiff = new ZoneResponse(2L, 1L, "Z-02", "Hűtött", ZoneType.CHILLED, false, now, now);
        assertEquals(ZoneType.NORMAL, zoneRes1.zoneType());
        assertRecordBasics(zoneRes1, zoneRes2, zoneResDiff);

        var shelfReq1 = new ShelfRequest(
                1L, "A-01-01", StorageType.RACK, 4,
                new BigDecimal("1000.000"), new BigDecimal("2.500000"), true
        );
        var shelfReq2 = new ShelfRequest(
                1L, "A-01-01", StorageType.RACK, 4,
                new BigDecimal("1000.000"), new BigDecimal("2.500000"), true
        );
        var shelfReqDiff = new ShelfRequest(
                2L, "B-01-01", StorageType.GROUND, 1,
                new BigDecimal("500.000"), new BigDecimal("1.000000"), false
        );
        assertTrue(validator.validate(shelfReq1).isEmpty());
        assertRecordBasics(shelfReq1, shelfReq2, shelfReqDiff);

        var invalidShelf = new ShelfRequest(null, "", null, 0, BigDecimal.ZERO, BigDecimal.ZERO, true);
        assertFalse(validator.validate(invalidShelf).isEmpty());

        var shelfRes1 = new ShelfResponse(
                1L, 1L, "Z-01", "A-01-01", StorageType.RACK, 4,
                new BigDecimal("1000.000"), new BigDecimal("2.500000"),
                BigDecimal.ZERO, BigDecimal.ZERO, true, now, now
        );
        var shelfRes2 = new ShelfResponse(
                1L, 1L, "Z-01", "A-01-01", StorageType.RACK, 4,
                new BigDecimal("1000.000"), new BigDecimal("2.500000"),
                BigDecimal.ZERO, BigDecimal.ZERO, true, now, now
        );
        var shelfResDiff = new ShelfResponse(
                2L, 1L, "Z-01", "A-01-02", StorageType.GROUND, 1,
                new BigDecimal("500.000"), new BigDecimal("1.000000"),
                BigDecimal.ZERO, BigDecimal.ZERO, false, now, now
        );
        assertEquals("A-01-01", shelfRes1.code());
        assertRecordBasics(shelfRes1, shelfRes2, shelfResDiff);

        var prodReq1 = new ProductRequest(
                "SKU-001", "5991234567890", "Termék", "Leírás",
                new BigDecimal("1.500"), new BigDecimal("0.010000"),
                true, ZoneType.CHILLED, 10, true
        );
        var prodReq2 = new ProductRequest(
                "SKU-001", "5991234567890", "Termék", "Leírás",
                new BigDecimal("1.500"), new BigDecimal("0.010000"),
                true, ZoneType.CHILLED, 10, true
        );
        var prodReqDiff = new ProductRequest(
                "SKU-002", null, "Másik", null,
                new BigDecimal("2.000"), new BigDecimal("0.020000"),
                false, ZoneType.NORMAL, 5, false
        );
        assertTrue(validator.validate(prodReq1).isEmpty());
        assertRecordBasics(prodReq1, prodReq2, prodReqDiff);

        var prodRes1 = new ProductResponse(
                1L, "SKU-001", "5991234567890", "Termék", "Leírás",
                new BigDecimal("1.500"), new BigDecimal("0.010000"),
                true, ZoneType.CHILLED, 10, true, now, now
        );
        var prodRes2 = new ProductResponse(
                1L, "SKU-001", "5991234567890", "Termék", "Leírás",
                new BigDecimal("1.500"), new BigDecimal("0.010000"),
                true, ZoneType.CHILLED, 10, true, now, now
        );
        var prodResDiff = new ProductResponse(
                2L, "SKU-002", null, "Másik", null,
                new BigDecimal("2.000"), new BigDecimal("0.020000"),
                false, ZoneType.NORMAL, 5, false, now, now
        );
        assertEquals("SKU-001", prodRes1.sku());
        assertRecordBasics(prodRes1, prodRes2, prodResDiff);
    }

    @Test
    void inboundTransferScrapAndSuggestionDtosValidateAndBehaveCorrectly() {
        Instant now = Instant.now();
        LocalDate expiry = LocalDate.now().plusDays(30);

        var inbReq1 = new InboundRequest(1L, 1L, 0, "LOT-01", expiry, 25);
        var inbReq2 = new InboundRequest(1L, 1L, 0, "LOT-01", expiry, 25);
        var inbReqDiff = new InboundRequest(2L, 2L, 1, "LOT-02", expiry, 10);
        assertTrue(validator.validate(inbReq1).isEmpty());
        assertRecordBasics(inbReq1, inbReq2, inbReqDiff);

        var inbRes1 = new InboundResponse(
                1L, 1L, "SKU-001", "Termék", 1L, "A-01-01",
                0, "LOT-01", expiry, 25, InventoryStatus.STORED, now
        );
        var inbRes2 = new InboundResponse(
                1L, 1L, "SKU-001", "Termék", 1L, "A-01-01",
                0, "LOT-01", expiry, 25, InventoryStatus.STORED, now
        );
        var inbResDiff = new InboundResponse(
                2L, 2L, "SKU-002", "Másik", 2L, "B-01-01",
                1, "LOT-02", expiry, 10, InventoryStatus.BLOCKED, now
        );
        assertEquals(25, inbRes1.quantity());
        assertRecordBasics(inbRes1, inbRes2, inbResDiff);

        var sug1 = new ShelfSuggestionResponse(1L, "A-01-01", ZoneType.NORMAL, new BigDecimal("500.000"), new BigDecimal("1.500000"));
        var sug2 = new ShelfSuggestionResponse(1L, "A-01-01", ZoneType.NORMAL, new BigDecimal("500.000"), new BigDecimal("1.500000"));
        var sugDiff = new ShelfSuggestionResponse(2L, "B-01-01", ZoneType.CHILLED, new BigDecimal("200.000"), new BigDecimal("0.500000"));
        assertEquals("A-01-01", sug1.shelfCode());
        assertRecordBasics(sug1, sug2, sugDiff);

        var trReq1 = new TransferRequest(1L, 1L, 0, 2L, 1, 10, "Áttárolás");
        var trReq2 = new TransferRequest(1L, 1L, 0, 2L, 1, 10, "Áttárolás");
        var trReqDiff = new TransferRequest(2L, 2L, 1, 3L, 0, 5, "Másik");
        assertTrue(validator.validate(trReq1).isEmpty());
        assertRecordBasics(trReq1, trReq2, trReqDiff);

        var scReq1 = new ScrapRequest(1L, 2, "Sérült csomagolás");
        var scReq2 = new ScrapRequest(1L, 2, "Sérült csomagolás");
        var scReqDiff = new ScrapRequest(2L, 1, "Lejárt");
        assertTrue(validator.validate(scReq1).isEmpty());
        assertRecordBasics(scReq1, scReq2, scReqDiff);

        var invalidScrap = new ScrapRequest(1L, 0, "   ");
        assertEquals(2, validator.validate(invalidScrap).size());
    }

    @Test
    void orderTaskAuditSummaryAndAuthUserDtosValidateAndBehaveCorrectly() {
        Instant now = Instant.now();

        var itemReq1 = new DeliveryOrderItemRequest(1L, 5);
        var itemReq2 = new DeliveryOrderItemRequest(1L, 5);
        var itemReqDiff = new DeliveryOrderItemRequest(2L, 10);
        assertRecordBasics(itemReq1, itemReq2, itemReqDiff);

        var itemRes1 = new DeliveryOrderItemResponse(1L, 1L, "SKU-001", "Termék", 5, 0);
        var itemRes2 = new DeliveryOrderItemResponse(1L, 1L, "SKU-001", "Termék", 5, 0);
        var itemResDiff = new DeliveryOrderItemResponse(2L, 2L, "SKU-002", "Másik", 10, 5);
        assertRecordBasics(itemRes1, itemRes2, itemResDiff);

        var orderReq1 = new DeliveryOrderCreateRequest("DO-001", "Vevő Kft.", "Budapest", now, List.of(itemReq1));
        var orderReq2 = new DeliveryOrderCreateRequest("DO-001", "Vevő Kft.", "Budapest", now, List.of(itemReq1));
        var orderReqDiff = new DeliveryOrderCreateRequest("DO-002", "Másik Kft.", "Debrecen", now, List.of(itemReqDiff));
        assertTrue(validator.validate(orderReq1).isEmpty());
        assertRecordBasics(orderReq1, orderReq2, orderReqDiff);

        var orderRes1 = new DeliveryOrderResponse(
                1L, "DO-001", "Vevő Kft.", "Budapest", DeliveryOrderStatus.OPEN,
                now, 1L, "dispatcher", List.of(itemRes1), now, now
        );
        var orderRes2 = new DeliveryOrderResponse(
                1L, "DO-001", "Vevő Kft.", "Budapest", DeliveryOrderStatus.OPEN,
                now, 1L, "dispatcher", List.of(itemRes1), now, now
        );
        var orderResDiff = new DeliveryOrderResponse(
                2L, "DO-002", "Másik", "Debrecen", DeliveryOrderStatus.SHIPPED,
                now, 2L, "admin", List.of(itemResDiff), now, now
        );
        assertEquals("DO-001", orderRes1.doNumber());
        assertRecordBasics(orderRes1, orderRes2, orderResDiff);

        var taskReq1 = new TaskCreateRequest(TaskType.PICKING, 3, 1L, 1L, 1L, 0, null, null, 5, 1L, 2L, "Megjegyzés");
        var taskReq2 = new TaskCreateRequest(TaskType.PICKING, 3, 1L, 1L, 1L, 0, null, null, 5, 1L, 2L, "Megjegyzés");
        var taskReqDiff = new TaskCreateRequest(TaskType.PUTAWAY, 1, 2L, null, null, null, 2L, 1, 10, null, null, null);
        assertTrue(validator.validate(taskReq1).isEmpty());
        assertRecordBasics(taskReq1, taskReq2, taskReqDiff);

        var compReq1 = new TaskCompleteRequest(5, "Teljesítve");
        var compReq2 = new TaskCompleteRequest(5, "Teljesítve");
        var compReqDiff = new TaskCompleteRequest(3, "Részleges");
        assertTrue(validator.validate(compReq1).isEmpty());
        assertRecordBasics(compReq1, compReq2, compReqDiff);

        var taskRes1 = new TaskResponse(
                1L, TaskType.PICKING, TaskStatus.PENDING, 3, 1L, "SKU-001", 1L,
                1L, "A-01-01", 0, null, null, null, 5, 1L, "DO-001",
                2L, "worker1", 1L, "dispatcher", "Megjegyzés", now, null, null, now
        );
        var taskRes2 = new TaskResponse(
                1L, TaskType.PICKING, TaskStatus.PENDING, 3, 1L, "SKU-001", 1L,
                1L, "A-01-01", 0, null, null, null, 5, 1L, "DO-001",
                2L, "worker1", 1L, "dispatcher", "Megjegyzés", now, null, null, now
        );
        var taskResDiff = new TaskResponse(
                2L, TaskType.SCRAP, TaskStatus.COMPLETED, 5, 2L, "SKU-002", 2L,
                2L, "B-01-01", 1, null, null, null, 2, null, null,
                2L, "worker1", 1L, "admin", "Selejt", now, now, now, now
        );
        assertEquals(TaskStatus.PENDING, taskRes1.status());
        assertRecordBasics(taskRes1, taskRes2, taskResDiff);

        var audit1 = new AuditLogResponse(
                1L, MovementType.INBOUND, 1L, "SKU-001", "Termék", 1L,
                null, null, null, 1L, "A-01-01", 0, 25, "Bevételezés", null, 1L, "admin", now
        );
        var audit2 = new AuditLogResponse(
                1L, MovementType.INBOUND, 1L, "SKU-001", "Termék", 1L,
                null, null, null, 1L, "A-01-01", 0, 25, "Bevételezés", null, 1L, "admin", now
        );
        var auditDiff = new AuditLogResponse(
                2L, MovementType.SCRAP, 2L, "SKU-002", "Másik", 2L,
                1L, "A-01-01", 0, null, null, null, 1, "Sérült", 3L, 2L, "worker1", now
        );
        assertEquals(MovementType.INBOUND, audit1.movementType());
        assertRecordBasics(audit1, audit2, auditDiff);

        var sum1 = new InventorySummaryItemDto(1L, "SKU-001", "Termék", 50, 10, 40, 20, false);
        var sum2 = new InventorySummaryItemDto(1L, "SKU-001", "Termék", 50, 10, 40, 20, false);
        var sumDiff = new InventorySummaryItemDto(2L, "SKU-002", "Másik", 5, 0, 5, 10, true);
        assertFalse(sum1.belowMinimum());
        assertRecordBasics(sum1, sum2, sumDiff);

        var userCreate1 = new UserCreateRequest("worker1", "Password123", "Kiss János", Role.WORKER, true);
        var userCreate2 = new UserCreateRequest("worker1", "Password123", "Kiss János", Role.WORKER, true);
        var userCreateDiff = new UserCreateRequest("admin2", "Password456", "Nagy Béla", Role.ADMIN, true);
        assertTrue(validator.validate(userCreate1).isEmpty());
        assertRecordBasics(userCreate1, userCreate2, userCreateDiff);

        var userUpdate1 = new UserUpdateRequest("Kiss János", null, Role.DISPATCHER, true);
        var userUpdate2 = new UserUpdateRequest("Kiss János", null, Role.DISPATCHER, true);
        var userUpdateDiff = new UserUpdateRequest("Nagy Béla", "NewPass123", Role.ADMIN, false);
        assertTrue(validator.validate(userUpdate1).isEmpty());
        assertRecordBasics(userUpdate1, userUpdate2, userUpdateDiff);

        var userRes1 = new UserResponse(1L, "worker1", "Kiss János", Role.WORKER, true, now, now);
        var userRes2 = new UserResponse(1L, "worker1", "Kiss János", Role.WORKER, true, now, now);
        var userResDiff = new UserResponse(2L, "admin", "Admin", Role.ADMIN, true, now, now);
        assertRecordBasics(userRes1, userRes2, userResDiff);

        var loginReq1 = new LoginRequest("worker1", "Password123");
        var loginReq2 = new LoginRequest("worker1", "Password123");
        var loginReqDiff = new LoginRequest("admin", "AdminPass123");
        assertTrue(validator.validate(loginReq1).isEmpty());
        assertRecordBasics(loginReq1, loginReq2, loginReqDiff);

        var loginRes1 = new LoginResponse("jwt", "Bearer", now, 1L, "worker1", "Kiss János", Role.WORKER);
        var loginRes2 = new LoginResponse("jwt", "Bearer", now, 1L, "worker1", "Kiss János", Role.WORKER);
        var loginResDiff = new LoginResponse("jwt2", "Bearer", now, 2L, "admin", "Admin", Role.ADMIN);
        assertRecordBasics(loginRes1, loginRes2, loginResDiff);

        var page1 = new PageResponse<>(List.of(userRes1), 0, 20, 1L, 1, true, true);
        var page2 = new PageResponse<>(List.of(userRes1), 0, 20, 1L, 1, true, true);
        var pageDiff = new PageResponse<>(List.of(userResDiff), 1, 10, 2L, 1, false, true);
        assertEquals(1, page1.content().size());
        assertRecordBasics(page1, page2, pageDiff);
    }
}