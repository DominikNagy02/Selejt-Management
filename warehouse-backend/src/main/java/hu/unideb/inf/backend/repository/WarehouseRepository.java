package hu.unideb.inf.backend.repository;

import hu.unideb.inf.backend.model.entity.WarehouseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseRepository extends JpaRepository<WarehouseEntity, Long> {

    Optional<WarehouseEntity> findByCode(String code);

    boolean existsByCode(String code);

    List<WarehouseEntity> findAllByActiveTrue();
}
