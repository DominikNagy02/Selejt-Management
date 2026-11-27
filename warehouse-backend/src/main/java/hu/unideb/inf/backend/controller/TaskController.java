package hu.unideb.inf.backend.controller;

import hu.unideb.inf.backend.service.task.TaskService;
import hu.unideb.inf.core.dto.PageResponse;
import hu.unideb.inf.core.dto.TaskCreateRequest;
import hu.unideb.inf.core.dto.TaskResponse;
import hu.unideb.inf.core.enums.TaskStatus;
import hu.unideb.inf.core.enums.TaskType;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = Objects.requireNonNull(taskService, "taskService must not be null");
    }

    @GetMapping
    public PageResponse<TaskResponse> findAll(
            @RequestParam(name = "status", required = false) TaskStatus status,
            @RequestParam(name = "taskType", required = false) TaskType taskType,
            @RequestParam(name = "deliveryOrderId", required = false) Long deliveryOrderId,
            @RequestParam(name = "assignedTo", required = false) Long assignedTo,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return taskService.findAll(status, taskType, deliveryOrderId, assignedTo, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(
            @Valid @RequestBody TaskCreateRequest request,
            @RequestHeader(name = "X-User-Id", defaultValue = "1") Long userId
    ) {
        return taskService.create(request, userId);
    }

    @GetMapping("/my-tasks")
    public PageResponse<TaskResponse> findMyTasks(
            @RequestHeader(name = "X-User-Id", defaultValue = "1") Long workerId,
            @RequestParam(name = "status", required = false) TaskStatus status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return taskService.findMyTasks(workerId, status, pageable);
    }

    @GetMapping("/{id}")
    public TaskResponse findById(@PathVariable("id") Long id) {
        return taskService.findById(id);
    }
}
