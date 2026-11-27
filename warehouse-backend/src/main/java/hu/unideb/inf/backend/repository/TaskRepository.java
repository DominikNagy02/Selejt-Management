package hu.unideb.inf.backend.repository;

import hu.unideb.inf.backend.model.entity.TaskEntity;
import hu.unideb.inf.core.enums.TaskStatus;
import hu.unideb.inf.core.enums.TaskType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<TaskEntity, Long> {

    List<TaskEntity> findByStatus(TaskStatus status);

    List<TaskEntity> findByStatusAndPriority(TaskStatus status, int priority);

    List<TaskEntity> findByAssignedToAndStatus(Long assignedTo, TaskStatus status);

    List<TaskEntity> findByAssignedToOrderByPriorityAscCreatedAtAsc(Long assignedTo);

    List<TaskEntity> findByDeliveryOrderId(Long deliveryOrderId);

    List<TaskEntity> findByTaskType(TaskType taskType);
}
