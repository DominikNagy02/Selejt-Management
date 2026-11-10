package hu.unideb.inf.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "wms")
public record WmsProperties(
        Picking picking,
        Reservation reservation
) {
    public WmsProperties {
        if (picking == null) {
            picking = new Picking("FEFO");
        }
        if (reservation == null) {
            reservation = new Reservation(30);
        }
    }

    public record Picking(String strategy) {
        public Picking {
            if (strategy == null || strategy.isBlank()) {
                strategy = "FEFO";
            }
        }
    }

    public record Reservation(int timeoutMinutes) {
        public Reservation {
            if (timeoutMinutes <= 0) {
                timeoutMinutes = 30;
            }
        }
    }
}
