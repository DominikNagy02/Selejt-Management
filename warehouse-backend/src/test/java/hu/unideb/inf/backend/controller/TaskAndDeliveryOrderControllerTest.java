package hu.unideb.inf.backend.controller;

import hu.unideb.inf.backend.config.ClockConfig;
import hu.unideb.inf.backend.service.task.DeliveryOrderService;
import hu.unideb.inf.backend.service.task.TaskService;
import hu.unideb.inf.core.dto.DeliveryOrderCreateRequest;
import hu.unideb.inf.core.dto.DeliveryOrderItemResponse;
import hu.unideb.inf.core.dto.DeliveryOrderResponse;
import hu.unideb.inf.core.dto.PageResponse;
import hu.unideb.inf.core.dto.TaskCreateRequest;
import hu.unideb.inf.core.dto.TaskLineResponse;
import hu.unideb.inf.core.dto.TaskResponse;
import hu.unideb.inf.core.enums.DeliveryOrderStatus;
import hu.unideb.inf.core.enums.TaskStatus;
import hu.unideb.inf.core.enums.TaskType;
import hu.unideb.inf.core.exception.DuplicateResourceException;
import hu.unideb.inf.core.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {DeliveryOrderController.class, TaskController.class})
@Import({GlobalExceptionHandler.class, ClockConfig.class})
class TaskAndDeliveryOrderControllerTest {

    private static final Instant NOW = Instant.parse("2026-11-27T23:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DeliveryOrderService deliveryOrderService;

    @MockBean
    private TaskService taskService;

    @Test
    void deliveryOrderAndTaskCrudAndMyTasksEndpointsWorkAndMapErrors() throws Exception {
        DeliveryOrderResponse order = new DeliveryOrderResponse(
                10L, "DO-2026-100", "Alfa Kft.", DeliveryOrderStatus.OPEN,
                LocalDate.of(2026, 12, 31), 2L, null,
                List.of(new DeliveryOrderItemResponse(1L, 100L, "SKU-1", "Termék", null, 5, 0)),
                NOW, NOW
        );
        TaskResponse task = new TaskResponse(
                20L, TaskType.PICKING, TaskStatus.PENDING, 1,
                100L, "SKU-1", 50L, 5L, "S-01", 0, null, null, null,
                5, 10L, "DO-2026-100", 3L, null, 2L, null, "Sürgős",
                List.of(new TaskLineResponse(1L, 50L, 7L, "LOT-1", 5L, "S-01", 0, null, null, null, 5, 0)),
                NOW, null, null, NOW
        );

        when(deliveryOrderService.findAll(eq(DeliveryOrderStatus.OPEN), eq("Alfa"), any(Pageable.class)))
                .thenReturn(new PageResponse<>(List.of(order), 0, 20, 1, 1, true, true));
        when(deliveryOrderService.findById(10L)).thenReturn(order);
        when(deliveryOrderService.findById(999L)).thenThrow(ResourceNotFoundException.of("DeliveryOrder", 999L));
        when(deliveryOrderService.create(any(DeliveryOrderCreateRequest.class), eq(2L)))
                .thenReturn(order)
                .thenThrow(DuplicateResourceException.of("DeliveryOrder", "doNumber", "DO-2026-100"));

        when(taskService.findAll(eq(TaskStatus.PENDING), eq(TaskType.PICKING), eq(10L), eq(3L), any(Pageable.class)))
                .thenReturn(new PageResponse<>(List.of(task), 0, 20, 1, 1, true, true));
        when(taskService.findMyTasks(eq(3L), eq(TaskStatus.PENDING), any(Pageable.class)))
                .thenReturn(new PageResponse<>(List.of(task), 0, 20, 1, 1, true, true));
        when(taskService.findById(20L)).thenReturn(task);
        when(taskService.findById(999L)).thenThrow(ResourceNotFoundException.of("Task", 999L));
        when(taskService.create(any(TaskCreateRequest.class), eq(2L))).thenReturn(task);

        mockMvc.perform(get("/api/v1/delivery-orders")
                        .param("status", "OPEN")
                        .param("customerName", "Alfa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].doNumber").value("DO-2026-100"));

        mockMvc.perform(get("/api/v1/delivery-orders/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerName").value("Alfa Kft."));

        mockMvc.perform(get("/api/v1/delivery-orders/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://smartwms.hu/errors/resource-not-found"));

        String validDoBody = """
                {
                  "doNumber": "DO-2026-100",
                  "customerName": "Alfa Kft.",
                  "dueDate": "2026-12-31",
                  "items": [{"productId": 100, "requestedQuantity": 5}]
                }
                """;
        mockMvc.perform(post("/api/v1/delivery-orders")
                        .header("X-User-Id", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validDoBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10));

        mockMvc.perform(post("/api/v1/delivery-orders")
                        .header("X-User-Id", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validDoBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://smartwms.hu/errors/duplicate-resource"));

        mockMvc.perform(post("/api/v1/delivery-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"doNumber":"","customerName":"","items":[]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.doNumber").isNotEmpty());

        mockMvc.perform(get("/api/v1/tasks")
                        .param("status", "PENDING")
                        .param("taskType", "PICKING")
                        .param("deliveryOrderId", "10")
                        .param("assignedTo", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(20));

        mockMvc.perform(get("/api/v1/tasks/my-tasks")
                        .header("X-User-Id", "3")
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].priority").value(1));

        mockMvc.perform(get("/api/v1/tasks/20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.doNumber").value("DO-2026-100"));

        mockMvc.perform(get("/api/v1/tasks/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://smartwms.hu/errors/resource-not-found"));

        mockMvc.perform(post("/api/v1/tasks")
                        .header("X-User-Id", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"taskType":"PICKING","priority":1,"productId":100,"quantity":5,"deliveryOrderId":10,"assignedToId":3}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(20));

        mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"taskType":"PICKING","priority":9,"productId":100,"quantity":0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.quantity").isNotEmpty());
    }
}
