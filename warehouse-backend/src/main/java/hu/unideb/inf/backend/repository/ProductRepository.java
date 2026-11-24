package hu.unideb.inf.backend.repository;

import hu.unideb.inf.backend.model.entity.ProductEntity;
import hu.unideb.inf.core.enums.ZoneType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

    Optional<ProductEntity> findBySku(String sku);

    Optional<ProductEntity> findByBarcode(String barcode);

    boolean existsBySku(String sku);

    boolean existsByBarcode(String barcode);

    List<ProductEntity> findAllByActiveTrue();

    List<ProductEntity> findByRequiredZoneType(ZoneType requiredZoneType);
}
