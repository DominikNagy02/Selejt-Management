package hu.unideb.inf.backend;

import hu.unideb.inf.backend.config.WmsProperties;
import hu.unideb.inf.backend.service.capacity.CapacityCheckResult;
import hu.unideb.inf.backend.service.capacity.CapacityValidator;
import hu.unideb.inf.backend.service.capacity.ItemSize;
import hu.unideb.inf.backend.service.capacity.ShelfLoad;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class WarehouseBackendApplicationTests {

    @Autowired
    private WmsProperties wmsProperties;

    @Autowired
    private CapacityValidator capacityValidator;

    @Test
    void contextLoads() {
        assertNotNull(wmsProperties);
        assertEquals("FEFO", wmsProperties.picking().strategy());
        assertEquals(30, wmsProperties.reservation().timeoutMinutes());

        assertNotNull(capacityValidator);
        CapacityCheckResult result = capacityValidator.validate(
                new ShelfLoad("S-01-01", 100.0, 1.0, 500.0, 5.0),
                new ItemSize(2.5, 0.05),
                10
        );
        assertTrue(result.isAllowed());
    }

    @Test
    void wmsPropertiesFallbackDefaults() {
        WmsProperties defaults = new WmsProperties(null, null);
        assertEquals("FEFO", defaults.picking().strategy());
        assertEquals(30, defaults.reservation().timeoutMinutes());

        WmsProperties blankAndNonPositive = new WmsProperties(
                new WmsProperties.Picking("   "),
                new WmsProperties.Reservation(0)
        );
        assertEquals("FEFO", blankAndNonPositive.picking().strategy());
        assertEquals(30, blankAndNonPositive.reservation().timeoutMinutes());
    }
}
