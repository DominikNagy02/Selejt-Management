package hu.unideb.inf.core.exception;

import hu.unideb.inf.core.enums.TaskStatus;
import hu.unideb.inf.core.enums.ZoneType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreExceptionTest {

    private static final class TestBusinessException extends WmsBusinessException {
        TestBusinessException(int status) {
            super("test-code", "Test Title", status, "Test error message");
        }
    }

    @Test
    void wmsBusinessExceptionValidatesHttpStatus() {
        var valid = new TestBusinessException(400);
        assertEquals("test-code", valid.getErrorCode());
        assertEquals("Test Title", valid.getTitle());
        assertEquals(400, valid.getHttpStatus());
        assertEquals("Test error message", valid.getMessage());

        assertThrows(IllegalArgumentException.class, () -> new TestBusinessException(99));
        assertThrows(IllegalArgumentException.class, () -> new TestBusinessException(600));
    }

    @Test
    void domainExceptionsStoreDetailsAndMessages() {
        var notFound = ResourceNotFoundException.of("Shelf", 42L);
        assertEquals("Shelf", notFound.getResourceType());
        assertEquals("42", notFound.getIdentifier());
        assertEquals(404, notFound.getHttpStatus());
        assertEquals("resource-not-found", notFound.getErrorCode());
        assertTrue(notFound.getMessage().contains("42"));

        var duplicate = DuplicateResourceException.of("Warehouse", "code", "WH-01");
        assertEquals("Warehouse", duplicate.getResourceType());
        assertEquals("code", duplicate.getField());
        assertEquals("WH-01", duplicate.getValue());
        assertEquals(409, duplicate.getHttpStatus());
        assertEquals("duplicate-resource", duplicate.getErrorCode());
        assertTrue(duplicate.getMessage().contains("WH-01"));

        var zoneMismatch = new ZoneMismatchException("SKU-MILK", ZoneType.CHILLED, "S-01", ZoneType.NORMAL);
        assertEquals("SKU-MILK", zoneMismatch.getProductSku());
        assertEquals(ZoneType.CHILLED, zoneMismatch.getRequiredZoneType());
        assertEquals("S-01", zoneMismatch.getShelfCode());
        assertEquals(ZoneType.NORMAL, zoneMismatch.getActualZoneType());
        assertEquals(409, zoneMismatch.getHttpStatus());
        assertEquals("zone-mismatch", zoneMismatch.getErrorCode());
        assertTrue(zoneMismatch.getMessage().contains("CHILLED"));

        var sameLocation = InvalidTransferException.sameLocation("S-01", 0);
        assertEquals(400, sameLocation.getHttpStatus());
        assertEquals("invalid-transfer", sameLocation.getErrorCode());
        assertTrue(sameLocation.getMessage().contains("S-01"));

        var sourceMismatch = InvalidTransferException.sourceLocationMismatch(5L, "S-02", 1);
        assertEquals(409, sourceMismatch.getHttpStatus());
        assertTrue(sourceMismatch.getMessage().contains("S-02"));

        var customTransfer = new InvalidTransferException("Érvénytelen áthelyezés");
        assertEquals(400, customTransfer.getHttpStatus());
        assertEquals("Érvénytelen áthelyezés", customTransfer.getMessage());

        var forbidden = new ForbiddenOperationException("Tiltott művelet");
        assertEquals(403, forbidden.getHttpStatus());
        assertEquals("forbidden-operation", forbidden.getErrorCode());
        assertEquals("Tiltott művelet", forbidden.getMessage());

        var insufficient = new InsufficientStockException("SKU-001", 10, 3);
        assertEquals("SKU-001", insufficient.getProductSku());
        assertEquals(10, insufficient.getRequestedQuantity());
        assertEquals(3, insufficient.getAvailableQuantity());
        assertEquals(409, insufficient.getHttpStatus());

        var insufficientNoSku = new InsufficientStockException(10, 3);
        assertNull(insufficientNoSku.getProductSku());
        assertEquals(10, insufficientNoSku.getRequestedQuantity());

        var insufficientBlankSku = new InsufficientStockException("   ", 10, 3);
        assertEquals("   ", insufficientBlankSku.getProductSku());
        assertTrue(insufficientBlankSku.getMessage().contains("Nincs elegendő elérhető készlet."));

        var conflict = new ReservationConflictException(1L, 10, 3);
        assertEquals(1L, conflict.getInventoryItemId());
        assertEquals(10, conflict.getRequestedQuantity());
        assertEquals(3, conflict.getAvailableQuantity());
        assertEquals(409, conflict.getHttpStatus());

        var invalidState = new InvalidTaskStateException(1L, TaskStatus.PENDING, TaskStatus.COMPLETED);
        assertEquals(1L, invalidState.getTaskId());
        assertEquals(TaskStatus.PENDING, invalidState.getCurrentStatus());
        assertEquals(TaskStatus.COMPLETED, invalidState.getTargetStatus());
        assertNotNull(invalidState.getMessage());

        var invalidStateNoId = new InvalidTaskStateException(TaskStatus.PENDING, TaskStatus.COMPLETED);
        assertNull(invalidStateNoId.getTaskId());

        var weightViolation = new CapacityViolation(
                "weight",
                new BigDecimal("100.000"),
                new BigDecimal("120.000"),
                "kg"
        );
        var volumeViolation = new CapacityViolation(
                "volume",
                new BigDecimal("1.000000"),
                new BigDecimal("1.500000"),
                "m³"
        );
        var capacityExceeded = new ShelfCapacityExceededException(
                "A-01-01",
                List.of(weightViolation, volumeViolation),
                "Kapacitás túllépve"
        );
        assertEquals("A-01-01", capacityExceeded.getShelfCode());
        assertEquals(2, capacityExceeded.getViolations().size());
        assertEquals(new BigDecimal("100.000"), capacityExceeded.getMaxLimit());
        assertEquals(new BigDecimal("120.000"), capacityExceeded.getCalculatedValue());
        assertEquals("kg, m³", capacityExceeded.getUnit());

        var singleViolation = new ShelfCapacityExceededException(
                "A-01-01",
                new BigDecimal("500"),
                new BigDecimal("550"),
                "kg"
        );
        assertEquals("kg", singleViolation.getUnit());
        assertEquals("weight", singleViolation.getViolations().get(0).dimension());

        var singleVolumeViolation = new ShelfCapacityExceededException(
                "A-01-01",
                new BigDecimal("1.0"),
                new BigDecimal("1.5"),
                "m³",
                "Térfogat túllépve"
        );
        assertEquals("m³", singleVolumeViolation.getUnit());
        assertEquals("volume", singleVolumeViolation.getViolations().get(0).dimension());

        assertThrows(IllegalArgumentException.class, () -> new ShelfCapacityExceededException("A-01-01", List.of(), "hiba"));

        var autoDual = new ShelfCapacityExceededException("A-01-01", List.of(weightViolation, volumeViolation));
        assertTrue(autoDual.getMessage().contains("súly- és térfogatkorlátja"));

        var autoVolume = new ShelfCapacityExceededException("A-01-01", List.of(volumeViolation));
        assertTrue(autoVolume.getMessage().contains("térfogata meghaladná"));
    }
}
