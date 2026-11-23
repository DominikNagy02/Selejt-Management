package hu.unideb.inf.core.dto;

import hu.unideb.inf.shared.schemas.ValidationConstants;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public record DeliveryOrderCreateRequest(
        @NotBlank
        @Size(max = ValidationConstants.DO_NUMBER_MAX_LENGTH)
        String doNumber,

        @NotBlank
        @Size(max = ValidationConstants.CUSTOMER_NAME_MAX_LENGTH)
        String customerName,

        @Size(max = ValidationConstants.ADDRESS_MAX_LENGTH)
        String shippingAddress,

        Instant dueDate,

        @NotEmpty
        List<@Valid DeliveryOrderItemRequest> items
) {
}