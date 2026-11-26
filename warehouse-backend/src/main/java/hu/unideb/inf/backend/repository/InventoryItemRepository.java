package hu.unideb.inf.backend.repository;

import hu.unideb.inf.backend.model.entity.InventoryItemEntity;
import hu.unideb.inf.core.enums.InventoryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItemEntity, Long> {

    Optional<InventoryItemEntity> findByShelfIdAndLotIdAndTierLevel(Long shelfId, Long lotId, int tierLevel);

    List<InventoryItemEntity> findByShelfId(Long shelfId);

    List<InventoryItemEntity> findByLotId(Long lotId);

    List<InventoryItemEntity> findByStatusAndLotId(InventoryStatus status, Long lotId);

    List<InventoryItemEntity> findByLotProductId(Long productId);

    List<InventoryItemEntity> findByShelfZoneWarehouseId(Long warehouseId);
}
