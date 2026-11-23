package hu.unideb.inf.backend.config;

import hu.unideb.inf.backend.service.picking.FefoPickingStrategy;
import hu.unideb.inf.backend.service.picking.FifoPickingStrategy;
import hu.unideb.inf.backend.service.picking.PickingStrategy;
import hu.unideb.inf.backend.service.picking.PickingStrategyType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class PickingStrategyConfig {

    @Bean
    public PickingStrategy pickingStrategy(WmsProperties wmsProperties, Clock clock) {
        PickingStrategyType strategyType = wmsProperties.picking().strategy();
        return switch (strategyType) {
            case FIFO -> new FifoPickingStrategy();
            case FEFO -> new FefoPickingStrategy(clock);
        };
    }
}

