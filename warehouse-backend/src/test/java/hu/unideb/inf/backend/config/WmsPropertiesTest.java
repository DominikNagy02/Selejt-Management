package hu.unideb.inf.backend.config;

import hu.unideb.inf.backend.service.picking.PickingStrategyType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WmsPropertiesTest {

    @Test
    void shouldUseDefaultValuesWhenNullSectionsProvided() {
        WmsProperties defaults = new WmsProperties(null, null);

        assertEquals(PickingStrategyType.FEFO, defaults.picking().strategy());
        assertEquals(30, defaults.reservation().timeoutMinutes());
    }

    @Test
    void shouldDefaultStrategyToFefoWhenNullAndRejectNonPositiveTimeout() {
        WmsProperties nullStrategy = new WmsProperties(
                new WmsProperties.Picking(null),
                new WmsProperties.Reservation(15)
        );
        assertEquals(PickingStrategyType.FEFO, nullStrategy.picking().strategy());
        assertEquals(15, nullStrategy.reservation().timeoutMinutes());

        assertThrows(
                IllegalArgumentException.class,
                () -> new WmsProperties.Reservation(0)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new WmsProperties.Reservation(-5)
        );
    }

    @Test
    void shouldKeepExplicitValidValues() {
        WmsProperties properties = new WmsProperties(
                new WmsProperties.Picking(PickingStrategyType.FIFO),
                new WmsProperties.Reservation(45)
        );

        assertEquals(PickingStrategyType.FIFO, properties.picking().strategy());
        assertEquals(45, properties.reservation().timeoutMinutes());
    }
}
