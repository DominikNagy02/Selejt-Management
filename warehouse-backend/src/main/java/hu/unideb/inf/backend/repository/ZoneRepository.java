package hu.unideb.inf.backend.repository;

import hu.unideb.inf.backend.model.entity.ZoneEntity;
import hu.unideb.inf.core.enums.ZoneType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ZoneRepository extends JpaRepository<ZoneEntity, Long> {

    Optional<ZoneEntity> findByWarehouseIdAndCode(Long warehouseId, String code);

    boolean existsByWarehouseIdAndCode(Long warehouseId, String code);

    List<ZoneEntity> findByWarehouseId(Long warehouseId);

    List<ZoneEntity> findByWarehouseIdAndActiveTrue(Long warehouseId);

    List<ZoneEntity> findByZoneTypeAndActiveTrue(ZoneType zoneType);
}
