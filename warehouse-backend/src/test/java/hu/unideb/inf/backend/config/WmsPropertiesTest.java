package hu.unideb.inf.backend.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WmsPropertiesTest {

    @Test
    void shouldUseDefaultValuesWhenNullSectionsProvided() {
        WmsProperties defaults = new WmsProperties(null, null);

        assertEquals("FEFO", defaults.picking().strategy());
        assertEquals(30, defaults.reservation().timeoutMinutes());
    }

    @Test
    void shouldFallbackToDefaultsWhenNullBlankOrNonPositiveValuesProvided() {
        WmsProperties nullStrategy = new WmsProperties(
                new WmsProperties.Picking(null),
                new WmsProperties.Reservation(-5)
        );
        assertEquals("FEFO", nullStrategy.picking().strategy());
        assertEquals(30, nullStrategy.reservation().timeoutMinutes());

        WmsProperties blankStrategy = new WmsProperties(
                new WmsProperties.Picking("   "),
                new WmsProperties.Reservation(0)
        );
        assertEquals("FEFO", blankStrategy.picking().strategy());
        assertEquals(30, blankStrategy.reservation().timeoutMinutes());
    }

    @Test
    void shouldKeepExplicitValidValues() {
        WmsProperties properties = new WmsProperties(
                new WmsProperties.Picking("FIFO"),
                new WmsProperties.Reservation(45)
        );

        assertEquals("FIFO", properties.picking().strategy());
        assertEquals(45, properties.reservation().timeoutMinutes());
    }
}
