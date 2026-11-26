package hu.unideb.inf.core.dto;

import hu.unideb.inf.core.enums.TaskStatus;
import hu.unideb.inf.core.enums.TaskType;

import java.time.Instant;
import java.util.List;

public record TaskResponse(
        Long id,
        TaskType taskType,
        TaskStatus status,
        int priority,
        Long productId,
        String productSku,
        Long inventoryId,
        Long fromShelfId,
        String fromShelfCode,
        Integer fromTierLevel,
        Long toShelfId,
        String toShelfCode,
        Integer toTierLevel,
        int quantity,
        Long deliveryOrderId,
        String doNumber,
        Long assignedToId,
        String assignedToUsername,
        Long createdById,
        String createdByUsername,
        String note,
        List<TaskLineResponse> lines,
        Instant createdAt,
        Instant startedAt,
        Instant completedAt,
        Instant updatedAt
) {
}
