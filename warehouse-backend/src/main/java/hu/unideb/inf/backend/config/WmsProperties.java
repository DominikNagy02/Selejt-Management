package hu.unideb.inf.backend.config;

import hu.unideb.inf.backend.service.picking.PickingStrategyType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "wms")
public record WmsProperties(
        @Valid @NotNull Picking picking,
        @Valid @NotNull Reservation reservation
) {
    public WmsProperties {
        if (picking == null) {
            picking = new Picking(PickingStrategyType.FEFO);
        }
        if (reservation == null) {
            reservation = new Reservation(30);
        }
    }

    public record Picking(@NotNull PickingStrategyType strategy) {
        public Picking {
            if (strategy == null) {
                strategy = PickingStrategyType.FEFO;
            }
        }
    }

    public record Reservation(@Positive int timeoutMinutes) {
        public Reservation {
            if (timeoutMinutes <= 0) {
                throw new IllegalArgumentException("timeoutMinutes must be greater than 0");
            }
        }
    }
}
