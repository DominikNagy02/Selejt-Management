package hu.unideb.inf.backend.service.capacity;

import hu.unideb.inf.core.exception.ShelfCapacityExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CapacityValidatorTest {

    private CapacityValidator validator;
    private ShelfLoad defaultShelf;
    private ItemSize defaultItem;

    @BeforeEach
    void setUp() {
        validator = new CapacityValidator();
        defaultShelf = new ShelfLoad(
                "S-04-12",
                new BigDecimal("320.000"),
                new BigDecimal("2.000"),
                new BigDecimal("500.000"),
                new BigDecimal("5.000")
        );
        defaultItem = new ItemSize(new BigDecimal("2.000"), new BigDecimal("0.050"));
    }

    // F2.5: Súlykorlát túllépése
    @Test
    void validateShouldRejectWeightLimitViolation() {
        ItemSize heavyItem = new ItemSize(new BigDecimal("2.000"), new BigDecimal("0.010"));

        ShelfCapacityExceededException ex = assertThrows(
                ShelfCapacityExceededException.class,
                () -> validator.validate(defaultShelf, heavyItem, 110)
        );

        assertEquals("S-04-12", ex.getShelfCode());
        assertEquals(0, new BigDecimal("500.000").compareTo(ex.getMaxLimit()));
        assertEquals(0, new BigDecimal("540.000").compareTo(ex.getCalculatedValue()));
        assertEquals("kg", ex.getUnit());
        assertTrue(ex.getMessage().contains("kg"));
    }

    // F2.6: Térfogatkorlát túllépése (és mindkettő túllépése)
    @Test
    void validateShouldRejectVolumeLimitViolation() {
        ItemSize bulkyItem = new ItemSize(new BigDecimal("1.000"), new BigDecimal("0.100"));

        ShelfCapacityExceededException ex = assertThrows(
                ShelfCapacityExceededException.class,
                () -> validator.validate(defaultShelf, bulkyItem, 50)
        );

        assertEquals("S-04-12", ex.getShelfCode());
        assertEquals(0, new BigDecimal("5.000").compareTo(ex.getMaxLimit()));
        assertEquals(0, new BigDecimal("7.000").compareTo(ex.getCalculatedValue()));
        assertEquals("m³", ex.getUnit());
        assertTrue(ex.getMessage().contains("m³"));
    }

    @Test
    void validateShouldRejectWhenBothWeightAndVolumeExceeded() {
        ItemSize heavyAndBulky = new ItemSize(new BigDecimal("5.000"), new BigDecimal("0.100"));

        ShelfCapacityExceededException ex = assertThrows(
                ShelfCapacityExceededException.class,
                () -> validator.validate(defaultShelf, heavyAndBulky, 50)
        );

        assertEquals("kg, m³", ex.getUnit());
        assertTrue(ex.getMessage().contains("kg"));
        assertTrue(ex.getMessage().contains("m³"));

        CapacityCheckResult checkResult = validator.check(defaultShelf, heavyAndBulky, 50);
        assertFalse(checkResult.isAllowed());
    }

    // F2.7: Határeset (pontosan a maximum még megengedett) + BigDecimal pontosság (0.1 + 0.2 == 0.3)
    @Test
    void validateShouldAcceptExactCapacityLimit() {
        ShelfLoad precisionShelf = new ShelfLoad("S-01-01", 0.1, 0.1, 0.3, 0.3);
        ItemSize precisionItem = new ItemSize(0.2, 0.2);

        CapacityCheckResult result = validator.validate(precisionShelf, precisionItem, 1);

        assertTrue(result.isAllowed());
        assertTrue(result.weightOk());
        assertTrue(result.volumeOk());
        assertEquals(0, new BigDecimal("0.3").compareTo(result.newWeight()));
        assertEquals(0, new BigDecimal("0.3").compareTo(result.newVolume()));
    }

    // F2.8: 0 vagy negatív mennyiség elutasítása
    @Test
    void validateShouldRejectNonPositiveQuantity() {
        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(defaultShelf, defaultItem, 0)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(defaultShelf, defaultItem, -1)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(defaultShelf, defaultItem, -100)
        );
    }
}
