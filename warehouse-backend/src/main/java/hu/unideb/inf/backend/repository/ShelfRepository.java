package hu.unideb.inf.backend.repository;

import hu.unideb.inf.backend.model.entity.ShelfEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShelfRepository extends JpaRepository<ShelfEntity, Long> {

    Optional<ShelfEntity> findByCode(String code);

    boolean existsByCode(String code);

    List<ShelfEntity> findByZoneId(Long zoneId);

    List<ShelfEntity> findByZoneIdAndActiveTrue(Long zoneId);

    List<ShelfEntity> findByZoneWarehouseId(Long warehouseId);
}
