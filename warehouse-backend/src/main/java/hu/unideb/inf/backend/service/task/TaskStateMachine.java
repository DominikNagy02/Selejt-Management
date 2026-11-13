package hu.unideb.inf.backend.service.task;

import hu.unideb.inf.core.enums.TaskStatus;
import hu.unideb.inf.core.exception.InvalidTaskStateException;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
public class TaskStateMachine {

    private static final Map<TaskStatus, Set<TaskStatus>> ALLOWED_TRANSITIONS;

    static {
        Map<TaskStatus, Set<TaskStatus>> map = new EnumMap<>(TaskStatus.class);
        map.put(TaskStatus.PENDING, Collections.unmodifiableSet(EnumSet.of(TaskStatus.IN_PROGRESS, TaskStatus.CANCELLED)));
        map.put(TaskStatus.IN_PROGRESS, Collections.unmodifiableSet(EnumSet.of(TaskStatus.COMPLETED, TaskStatus.CANCELLED)));
        map.put(TaskStatus.COMPLETED, Collections.emptySet());
        map.put(TaskStatus.CANCELLED, Collections.emptySet());
        ALLOWED_TRANSITIONS = Collections.unmodifiableMap(map);
    }

    public boolean canTransition(TaskStatus from, TaskStatus to) {
        Objects.requireNonNull(from, "from status must not be null");
        Objects.requireNonNull(to, "to status must not be null");
        return ALLOWED_TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }

    public TaskStatus transition(TaskStatus from, TaskStatus to) {
        return transition(null, from, to);
    }

    public TaskStatus transition(Long taskId, TaskStatus from, TaskStatus to) {
        if (!canTransition(from, to)) {
            throw new InvalidTaskStateException(taskId, from, to);
        }
        return to;
    }

    public Set<TaskStatus> allowedTargets(TaskStatus from) {
        Objects.requireNonNull(from, "from status must not be null");
        return ALLOWED_TRANSITIONS.getOrDefault(from, Set.of());
    }
}

