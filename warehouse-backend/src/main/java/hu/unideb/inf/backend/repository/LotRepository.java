package hu.unideb.inf.backend.repository;

import hu.unideb.inf.backend.model.entity.LotEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LotRepository extends JpaRepository<LotEntity, Long> {

    Optional<LotEntity> findByProductIdAndLotNumber(Long productId, String lotNumber);

    List<LotEntity> findByProductId(Long productId);

    boolean existsByProductIdAndLotNumber(Long productId, String lotNumber);
}
