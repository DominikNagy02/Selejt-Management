package hu.unideb.inf.backend.repository;

import hu.unideb.inf.backend.model.entity.AuditEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEventEntity, Long> {

    List<AuditEventEntity> findByEntityTypeAndEntityId(String entityType, Long entityId);

    List<AuditEventEntity> findByEntityType(String entityType);

    List<AuditEventEntity> findByCreatedAtBetween(Instant from, Instant to);

    List<AuditEventEntity> findByPerformedBy(Long performedBy);
}
