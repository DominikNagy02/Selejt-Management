package hu.unideb.inf.backend.service.capacity;

import hu.unideb.inf.core.exception.ShelfCapacityExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CapacityCalculatorTest {

    private CapacityCalculator calculator;
    private ShelfLoad shelf;

    @BeforeEach
    void setUp() {
        calculator = new CapacityCalculator();
        shelf = new ShelfLoad(
                "S-04-12",
                new BigDecimal("320.000"),
                new BigDecimal("2.150"),
                new BigDecimal("500.000"),
                new BigDecimal("5.000")
        );
    }

    @Test
    void calculateNewWeightShouldMatchDocumentedFormula() {
        // 320 kg + 2.5 kg * 10 = 345 kg
        ItemSize item = new ItemSize(new BigDecimal("2.500"), new BigDecimal("0.010"));

        BigDecimal newWeight = calculator.calculateNewWeight(shelf, item, 10);

        assertEquals(0, new BigDecimal("345.000").compareTo(newWeight));
    }

    @Test
    void calculateNewVolumeShouldMatchDocumentedFormula() {
        // 2.15 m³ + 0.01 m³ * 50 = 2.65 m³
        ItemSize item = new ItemSize(new BigDecimal("2.500"), new BigDecimal("0.010"));

        BigDecimal newVolume = calculator.calculateNewVolume(shelf, item, 50);

        assertEquals(0, new BigDecimal("2.650").compareTo(newVolume));
    }

    @Test
    void calculatorShouldRejectNullInputs() {
        ItemSize item = new ItemSize(new BigDecimal("1.0"), new BigDecimal("0.1"));

        assertThrows(NullPointerException.class, () -> calculator.calculateNewWeight(null, item, 1));
        assertThrows(NullPointerException.class, () -> calculator.calculateNewVolume(shelf, null, 1));
    }

    @Test
    void shelfLoadShouldRejectInvalidValues() {
        BigDecimal zero = BigDecimal.ZERO;
        BigDecimal positive = new BigDecimal("10");
        BigDecimal negative = new BigDecimal("-1");

        assertThrows(IllegalArgumentException.class,
                () -> new ShelfLoad("   ", zero, zero, positive, positive));
        assertThrows(IllegalArgumentException.class,
                () -> new ShelfLoad("S-01", negative, zero, positive, positive));
        assertThrows(IllegalArgumentException.class,
                () -> new ShelfLoad("S-01", zero, negative, positive, positive));
        assertThrows(IllegalArgumentException.class,
                () -> new ShelfLoad("S-01", zero, zero, zero, positive));
        assertThrows(IllegalArgumentException.class,
                () -> new ShelfLoad("S-01", zero, zero, positive, zero));
    }

    @Test
    void itemSizeShouldRejectNonPositiveValues() {
        BigDecimal positive = new BigDecimal("1.0");

        assertThrows(IllegalArgumentException.class,
                () -> new ItemSize(BigDecimal.ZERO, positive));
        assertThrows(IllegalArgumentException.class,
                () -> new ItemSize(new BigDecimal("-0.5"), positive));
        assertThrows(IllegalArgumentException.class,
                () -> new ItemSize(positive, BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> new ItemSize(positive, new BigDecimal("-0.1")));
    }

    @Test
    void shelfCapacityExceededExceptionConstructorsShouldFormatAndValidate() {
        ShelfCapacityExceededException weightEx = new ShelfCapacityExceededException(
                "S-04-12",
                new BigDecimal("500.000"),
                new BigDecimal("540.500"),
                "kg"
        );
        assertEquals(
                "A polc (S-04-12) teherbírása meghaladná a maximális 500 kg-ot. Számított új súly: 540.5 kg.",
                weightEx.getMessage()
        );
        assertEquals("weight", weightEx.getViolations().get(0).dimension());

        ShelfCapacityExceededException volumeEx = new ShelfCapacityExceededException(
                "S-04-12",
                new BigDecimal("5.000"),
                new BigDecimal("5.250"),
                "m³"
        );
        assertEquals(
                "A polc (S-04-12) térfogata meghaladná a maximális 5 m³-t. Számított új térfogat: 5.25 m³.",
                volumeEx.getMessage()
        );
        assertEquals("volume", volumeEx.getViolations().get(0).dimension());

        assertThrows(IllegalArgumentException.class,
                () -> new ShelfCapacityExceededException("S-04-12", List.of(), "Üres lista"));
    }
}

