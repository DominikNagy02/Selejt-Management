package hu.unideb.inf.backend;

import hu.unideb.inf.backend.config.WmsProperties;
import hu.unideb.inf.backend.service.capacity.CapacityValidator;
import hu.unideb.inf.backend.service.picking.PickingStrategy;
import hu.unideb.inf.backend.service.picking.PickingStrategyType;
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

    @Autowired
    private PickingStrategy pickingStrategy;

    @Test
    void contextLoads() {
        assertNotNull(wmsProperties);
        assertEquals(PickingStrategyType.FEFO, wmsProperties.picking().strategy());
        assertEquals(30, wmsProperties.reservation().timeoutMinutes());
        assertNotNull(capacityValidator);
        assertNotNull(pickingStrategy);
        assertEquals(PickingStrategyType.FEFO, pickingStrategy.type());
    }
}
