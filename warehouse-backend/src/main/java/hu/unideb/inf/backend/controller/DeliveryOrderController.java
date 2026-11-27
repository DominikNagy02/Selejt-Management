package hu.unideb.inf.backend.controller;

import hu.unideb.inf.backend.service.task.DeliveryOrderService;
import hu.unideb.inf.core.dto.DeliveryOrderCreateRequest;
import hu.unideb.inf.core.dto.DeliveryOrderResponse;
import hu.unideb.inf.core.dto.PageResponse;
import hu.unideb.inf.core.enums.DeliveryOrderStatus;
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
@RequestMapping("/api/v1/delivery-orders")
public class DeliveryOrderController {

    private final DeliveryOrderService deliveryOrderService;

    public DeliveryOrderController(DeliveryOrderService deliveryOrderService) {
        this.deliveryOrderService = Objects.requireNonNull(
                deliveryOrderService,
                "deliveryOrderService must not be null"
        );
    }

    @GetMapping
    public PageResponse<DeliveryOrderResponse> findAll(
            @RequestParam(name = "status", required = false) DeliveryOrderStatus status,
            @RequestParam(name = "customerName", required = false) String customerName,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return deliveryOrderService.findAll(status, customerName, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DeliveryOrderResponse create(
            @Valid @RequestBody DeliveryOrderCreateRequest request,
            @RequestHeader(name = "X-User-Id", defaultValue = "1") Long userId
    ) {
        return deliveryOrderService.create(request, userId);
    }

    @GetMapping("/{id}")
    public DeliveryOrderResponse findById(@PathVariable("id") Long id) {
        return deliveryOrderService.findById(id);
    }
}
