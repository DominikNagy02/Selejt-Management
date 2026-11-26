package hu.unideb.inf.backend.repository;

import hu.unideb.inf.backend.model.entity.DeliveryOrderEntity;
import hu.unideb.inf.core.enums.DeliveryOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryOrderRepository extends JpaRepository<DeliveryOrderEntity, Long> {

    Optional<DeliveryOrderEntity> findByDoNumber(String doNumber);

    boolean existsByDoNumber(String doNumber);

    List<DeliveryOrderEntity> findByStatus(DeliveryOrderStatus status);

    List<DeliveryOrderEntity> findByCustomerNameContainingIgnoreCase(String customerName);
}
