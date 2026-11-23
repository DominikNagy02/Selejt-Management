package hu.unideb.inf.core.exception;

import hu.unideb.inf.core.enums.TaskStatus;

import java.util.Objects;

public class InvalidTaskStateException extends WmsBusinessException {

    public static final String ERROR_CODE = "invalid-task-state";
    public static final String TITLE = "Invalid Task State";

    private final Long taskId;
    private final TaskStatus currentStatus;
    private final TaskStatus targetStatus;

    public InvalidTaskStateException(TaskStatus currentStatus, TaskStatus targetStatus) {
        this(null, currentStatus, targetStatus);
    }

    public InvalidTaskStateException(Long taskId, TaskStatus currentStatus, TaskStatus targetStatus) {
        super(ERROR_CODE, TITLE, 409, buildMessage(taskId, currentStatus, targetStatus));
        this.taskId = taskId;
        this.currentStatus = currentStatus;
        this.targetStatus = targetStatus;
    }

    private static String buildMessage(Long taskId, TaskStatus currentStatus, TaskStatus targetStatus) {
        Objects.requireNonNull(currentStatus, "currentStatus must not be null");
        Objects.requireNonNull(targetStatus, "targetStatus must not be null");
        if (taskId != null) {
            return String.format(
                    "A feladat (#%d) %s állapotból nem vihető %s állapotba.",
                    taskId, currentStatus, targetStatus
            );
        }
        return String.format(
                "A feladat %s állapotból nem vihető %s állapotba.",
                currentStatus, targetStatus
        );
    }

    public Long getTaskId() {
        return taskId;
    }

    public TaskStatus getCurrentStatus() {
        return currentStatus;
    }

    public TaskStatus getTargetStatus() {
        return targetStatus;
    }
}
