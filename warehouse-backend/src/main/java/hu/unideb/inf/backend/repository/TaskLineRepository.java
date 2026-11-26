package hu.unideb.inf.backend.repository;

import hu.unideb.inf.backend.model.entity.TaskLineEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskLineRepository extends JpaRepository<TaskLineEntity, Long> {

    Optional<TaskLineEntity> findByTaskIdAndLineNo(Long taskId, int lineNo);

    List<TaskLineEntity> findByTaskId(Long taskId);
}
