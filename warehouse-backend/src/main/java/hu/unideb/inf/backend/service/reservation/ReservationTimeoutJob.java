package hu.unideb.inf.backend.service.reservation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class ReservationTimeoutJob {

    private static final Logger log = LoggerFactory.getLogger(ReservationTimeoutJob.class);

    private final StockReservationService stockReservationService;

    public ReservationTimeoutJob(StockReservationService stockReservationService) {
        this.stockReservationService = Objects.requireNonNull(
                stockReservationService,
                "stockReservationService must not be null"
        );
    }

    @Scheduled(fixedDelayString = "${wms.reservation.cleanup-interval-ms:60000}")
    public int releaseExpiredReservations() {
        int expiredCount = stockReservationService.expireOverdueReservations();
        if (expiredCount > 0) {
            log.info("Auto-released {} expired stock reservation(s)", expiredCount);
        }
        return expiredCount;
    }
}
