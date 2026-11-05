package hu.unideb.inf.backend;

import hu.unideb.inf.backend.config.WmsProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class WarehouseBackendApplicationTests {

    @Autowired
    private WmsProperties wmsProperties;

    @Test
    void contextLoads() {
        assertNotNull(wmsProperties);
        assertEquals("FEFO", wmsProperties.picking().strategy());
        assertEquals(30, wmsProperties.reservation().timeoutMinutes());
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
