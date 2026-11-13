package hu.unideb.inf.backend.service.capacity;

import hu.unideb.inf.core.exception.CapacityViolation;
import hu.unideb.inf.core.exception.ShelfCapacityExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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
        validator = new CapacityValidator(new CapacityCalculator());
        defaultShelf = new ShelfLoad(
                "S-04-12",
                new BigDecimal("320.000"),
                new BigDecimal("2.000"),
                new BigDecimal("500.000"),
                new BigDecimal("5.000")
        );
        defaultItem = new ItemSize(new BigDecimal("2.000"), new BigDecimal("0.050"));
    }

    // F2.5: Súlykorlát túllépése + PROJEKT_TERV 6.2 üzenetformátum (F2.11)
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
        assertEquals(1, ex.getViolations().size());
        assertEquals("weight", ex.getViolations().get(0).dimension());
        assertEquals(
                "A polc (S-04-12) teherbírása meghaladná a maximális 500 kg-ot. Számított új súly: 540 kg.",
                ex.getMessage()
        );
    }

    // F2.6: Térfogatkorlát túllépése
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
        assertEquals(1, ex.getViolations().size());
        assertEquals("volume", ex.getViolations().get(0).dimension());
        assertEquals(
                "A polc (S-04-12) térfogata meghaladná a maximális 5 m³-t. Számított új térfogat: 7 m³.",
                ex.getMessage()
        );

        CapacityCheckResult checkResult = validator.check(defaultShelf, bulkyItem, 50);
        assertTrue(checkResult.weightOk());
        assertFalse(checkResult.volumeOk());
        assertFalse(checkResult.isAllowed());
    }

    // F2.10: Mindkét korlát túllépésekor mindkét dimenzió struktúráltan megmarad
    @Test
    void validateShouldRejectWhenBothWeightAndVolumeExceeded() {
        ItemSize heavyAndBulky = new ItemSize(new BigDecimal("5.000"), new BigDecimal("0.100"));

        ShelfCapacityExceededException ex = assertThrows(
                ShelfCapacityExceededException.class,
                () -> validator.validate(defaultShelf, heavyAndBulky, 50)
        );

        assertEquals("kg, m³", ex.getUnit());
        assertEquals(2, ex.getViolations().size());

        CapacityViolation weightViolation = ex.getViolations().get(0);
        assertEquals("weight", weightViolation.dimension());
        assertEquals(0, new BigDecimal("500").compareTo(weightViolation.maxLimit()));
        assertEquals(0, new BigDecimal("570").compareTo(weightViolation.calculatedValue()));
        assertEquals("kg", weightViolation.unit());

        CapacityViolation volumeViolation = ex.getViolations().get(1);
        assertEquals("volume", volumeViolation.dimension());
        assertEquals(0, new BigDecimal("5").compareTo(volumeViolation.maxLimit()));
        assertEquals(0, new BigDecimal("7").compareTo(volumeViolation.calculatedValue()));
        assertEquals("m³", volumeViolation.unit());

        assertEquals(
                "A polc (S-04-12) súly- és térfogatkorlátja is túllépésre kerülne: súly 570 / 500 kg, térfogat 7 / 5 m³.",
                ex.getMessage()
        );

        CapacityCheckResult checkResult = validator.check(defaultShelf, heavyAndBulky, 50);
        assertFalse(checkResult.isAllowed());
    }

    // F2.7: Határeset (pontosan a maximum még megengedett) + BigDecimal pontosság (0.1 + 0.2 == 0.3)
    @Test
    void validateShouldAcceptExactCapacityLimit() {
        ShelfLoad precisionShelf = new ShelfLoad(
                "S-01-01",
                new BigDecimal("0.1"),
                new BigDecimal("0.1"),
                new BigDecimal("0.3"),
                new BigDecimal("0.3")
        );
        ItemSize precisionItem = new ItemSize(new BigDecimal("0.2"), new BigDecimal("0.2"));

        CapacityCheckResult result = validator.validate(precisionShelf, precisionItem, 1);

        assertTrue(result.isAllowed());
        assertTrue(result.weightOk());
        assertTrue(result.volumeOk());
        assertEquals(0, new BigDecimal("0.3").compareTo(result.newWeight()));
        assertEquals(0, new BigDecimal("0.3").compareTo(result.newVolume()));
    }

    // F2.8 + F2.15: 0 vagy negatív mennyiség elutasítása paraméterezett teszttel
    @ParameterizedTest
    @ValueSource(ints = {0, -1, -100})
    void validateShouldRejectNonPositiveQuantity(int invalidQuantity) {
        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(defaultShelf, defaultItem, invalidQuantity)
        );
    }
}
