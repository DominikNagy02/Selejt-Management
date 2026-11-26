package hu.unideb.inf.backend.repository;

import hu.unideb.inf.backend.model.entity.StockReservationEntity;
import hu.unideb.inf.core.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface StockReservationRepository extends JpaRepository<StockReservationEntity, Long> {

    List<StockReservationEntity> findByStatusAndExpiresAtBefore(ReservationStatus status, Instant expiresAt);

    List<StockReservationEntity> findByInventoryItemId(Long inventoryItemId);

    List<StockReservationEntity> findByInventoryItemIdAndStatus(Long inventoryItemId, ReservationStatus status);

    List<StockReservationEntity> findByTaskLineId(Long taskLineId);

    List<StockReservationEntity> findByDeliveryOrderLineId(Long deliveryOrderLineId);
}
