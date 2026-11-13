package hu.unideb.inf.backend.config;

import hu.unideb.inf.backend.service.picking.PickingStrategy;
import hu.unideb.inf.backend.service.picking.PickingStrategyType;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class PickingStrategyConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
            .withUserConfiguration(TestWmsPropertiesConfig.class, ClockConfig.class, PickingStrategyConfig.class);

    @Configuration
    @EnableConfigurationProperties(WmsProperties.class)
    static class TestWmsPropertiesConfig {
    }

    @Test
    void shouldLoadFefoStrategyByDefault() {
        contextRunner.run(context -> {
            assertNull(context.getStartupFailure());
            PickingStrategy strategy = context.getBean(PickingStrategy.class);
            assertEquals(PickingStrategyType.FEFO, strategy.type());
        });
    }

    @Test
    void shouldLoadFifoStrategyWhenConfigured() {
        contextRunner
                .withPropertyValues("wms.picking.strategy=FIFO")
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    PickingStrategy strategy = context.getBean(PickingStrategy.class);
                    assertEquals(PickingStrategyType.FIFO, strategy.type());
                });
    }

    @Test
    void shouldFailContextStartupWhenStrategyIsInvalid() {
        contextRunner
                .withPropertyValues("wms.picking.strategy=INVALID_STRATEGY")
                .run(context -> assertNotNull(context.getStartupFailure()));
    }

    @Test
    void shouldFailContextStartupWhenTimeoutIsNonPositive() {
        contextRunner
                .withPropertyValues("wms.reservation.timeout-minutes=0")
                .run(context -> assertNotNull(context.getStartupFailure()));
    }
}

