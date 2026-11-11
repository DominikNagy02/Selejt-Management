package hu.unideb.inf.backend.service.task;

import hu.unideb.inf.core.enums.TaskStatus;
import hu.unideb.inf.core.exception.InvalidTaskStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskStateMachineTest {

    private TaskStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new TaskStateMachine();
    }

    // F4.4: Paraméterezett teszt minden engedélyezett átmenetre (4 db)
    @ParameterizedTest
    @CsvSource({
            "PENDING, IN_PROGRESS",
            "PENDING, CANCELLED",
            "IN_PROGRESS, COMPLETED",
            "IN_PROGRESS, CANCELLED"
    })
    void shouldAllowValidTransitions(TaskStatus from, TaskStatus to) {
        assertTrue(stateMachine.canTransition(from, to));
        assertEquals(to, stateMachine.transition(from, to));
        assertEquals(to, stateMachine.transition(42L, from, to));
    }

    // F4.5: Paraméterezett teszt minden tiltott átmenetre (12 db, beleértve az önmagába mutatókat is)
    @ParameterizedTest
    @CsvSource({
            "PENDING, PENDING",
            "PENDING, COMPLETED",
            "IN_PROGRESS, PENDING",
            "IN_PROGRESS, IN_PROGRESS",
            "COMPLETED, PENDING",
            "COMPLETED, IN_PROGRESS",
            "COMPLETED, COMPLETED",
            "COMPLETED, CANCELLED",
            "CANCELLED, PENDING",
            "CANCELLED, IN_PROGRESS",
            "CANCELLED, COMPLETED",
            "CANCELLED, CANCELLED"
    })
    void shouldRejectForbiddenTransitions(TaskStatus from, TaskStatus to) {
        assertFalse(stateMachine.canTransition(from, to));

        InvalidTaskStateException withId = assertThrows(
                InvalidTaskStateException.class,
                () -> stateMachine.transition(12L, from, to)
        );
        assertEquals(12L, withId.getTaskId());
        assertEquals(from, withId.getCurrentStatus());
        assertEquals(to, withId.getTargetStatus());
        assertEquals(
                String.format("A feladat (#12) %s állapotból nem vihető %s állapotba.", from, to),
                withId.getMessage()
        );

        InvalidTaskStateException withoutId = assertThrows(
                InvalidTaskStateException.class,
                () -> stateMachine.transition(from, to)
        );
        assertNull(withoutId.getTaskId());
        assertEquals(from, withoutId.getCurrentStatus());
        assertEquals(to, withoutId.getTargetStatus());
        assertEquals(
                String.format("A feladat %s állapotból nem vihető %s állapotba.", from, to),
                withoutId.getMessage()
        );
    }

    @Test
    void allowedTargetsShouldReturnExpectedSetForEachState() {
        assertEquals(Set.of(TaskStatus.IN_PROGRESS, TaskStatus.CANCELLED), stateMachine.allowedTargets(TaskStatus.PENDING));
        assertEquals(Set.of(TaskStatus.COMPLETED, TaskStatus.CANCELLED), stateMachine.allowedTargets(TaskStatus.IN_PROGRESS));
        assertTrue(stateMachine.allowedTargets(TaskStatus.COMPLETED).isEmpty());
        assertTrue(stateMachine.allowedTargets(TaskStatus.CANCELLED).isEmpty());
    }

    @Test
    void shouldRejectNullStatuses() {
        assertThrows(NullPointerException.class, () -> stateMachine.canTransition(null, TaskStatus.IN_PROGRESS));
        assertThrows(NullPointerException.class, () -> stateMachine.canTransition(TaskStatus.PENDING, null));
        assertThrows(NullPointerException.class, () -> stateMachine.allowedTargets(null));
    }
}

