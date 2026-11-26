package hu.unideb.inf.backend.repository;

import hu.unideb.inf.backend.model.entity.DeliveryOrderLineEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryOrderLineRepository extends JpaRepository<DeliveryOrderLineEntity, Long> {

    Optional<DeliveryOrderLineEntity> findByDeliveryOrderIdAndLineNo(Long deliveryOrderId, int lineNo);

    List<DeliveryOrderLineEntity> findByDeliveryOrderId(Long deliveryOrderId);
}
