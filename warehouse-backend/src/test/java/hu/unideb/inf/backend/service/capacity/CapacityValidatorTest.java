package hu.unideb.inf.backend.service.capacity;

import hu.unideb.inf.core.exception.ShelfCapacityExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
        defaultShelf = new ShelfLoad("S-04-12", 320.0, 2.0, 500.0, 5.0);
        defaultItem = new ItemSize(2.0, 0.05);
    }

    // F2.5: Súlykorlát túllépése
    @Test
    void validateShouldRejectWeightLimitViolation() {
        // Új súly: 320 + (2.0 * 110) = 540 kg > 500 kg; Új térfogat: 2.0 + (0.05 * 110) = 7.5 (állítsuk kisebbre a térfogatot, hogy csak a súly lépje túl)
        ItemSize heavyItem = new ItemSize(2.0, 0.01);

        ShelfCapacityExceededException ex = assertThrows(
                ShelfCapacityExceededException.class,
                () -> validator.validate(defaultShelf, heavyItem, 110)
        );

        assertEquals("S-04-12", ex.getShelfCode());
        assertEquals(500.0, ex.getMaxLimit());
        assertEquals(540.0, ex.getCalculatedValue());
        assertEquals("kg", ex.getUnit());
        assertTrue(ex.getMessage().contains("kg"));
    }

    // F2.6: Térfogatkorlát túllépése (és mindkettő túllépése)
    @Test
    void validateShouldRejectVolumeLimitViolation() {
        // Új súly: 320 + (1.0 * 50) = 370 kg <= 500 kg; Új térfogat: 2.0 + (0.1 * 50) = 7.0 m³ > 5.0 m³
        ItemSize bulkyItem = new ItemSize(1.0, 0.1);

        ShelfCapacityExceededException ex = assertThrows(
                ShelfCapacityExceededException.class,
                () -> validator.validate(defaultShelf, bulkyItem, 50)
        );

        assertEquals("S-04-12", ex.getShelfCode());
        assertEquals(5.0, ex.getMaxLimit());
        assertEquals(7.0, ex.getCalculatedValue());
        assertEquals("m³", ex.getUnit());
        assertTrue(ex.getMessage().contains("m³"));
    }

    @Test
    void validateShouldRejectWhenBothWeightAndVolumeExceeded() {
        ItemSize heavyAndBulky = new ItemSize(5.0, 0.1);

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

    // F2.7: Határeset (pontosan a maximum még megengedett)
    @Test
    void validateShouldAcceptExactCapacityLimit() {
        // Új súly: 320 + (1.8 * 100) = 500.0 kg == 500.0 kg
        // Új térfogat: 2.0 + (0.03 * 100) = 5.0 m³ == 5.0 m³
        ItemSize exactFitItem = new ItemSize(1.8, 0.03);

        CapacityCheckResult result = validator.validate(defaultShelf, exactFitItem, 100);

        assertTrue(result.isAllowed());
        assertTrue(result.weightOk());
        assertTrue(result.volumeOk());
        assertEquals(500.0, result.newWeight());
        assertEquals(5.0, result.newVolume());
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

