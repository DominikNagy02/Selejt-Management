package hu.unideb.inf.backend.repository;

import hu.unideb.inf.backend.model.entity.StockMovementEntity;
import hu.unideb.inf.core.enums.MovementType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovementEntity, Long> {

    List<StockMovementEntity> findByLotId(Long lotId);

    List<StockMovementEntity> findByMovementType(MovementType movementType);

    List<StockMovementEntity> findByCreatedAtBetween(Instant from, Instant to);

    List<StockMovementEntity> findByMovementTypeAndCreatedAtBetween(MovementType movementType, Instant from, Instant to);

    List<StockMovementEntity> findByTaskId(Long taskId);

    List<StockMovementEntity> findByDeliveryOrderId(Long deliveryOrderId);
}
