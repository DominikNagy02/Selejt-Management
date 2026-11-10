package hu.unideb.inf.backend;

import hu.unideb.inf.backend.config.WmsProperties;
import hu.unideb.inf.backend.service.capacity.CapacityValidator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
    }
}
