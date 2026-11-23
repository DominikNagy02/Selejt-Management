package hu.unideb.inf.core.dto;

import hu.unideb.inf.shared.schemas.ValidationConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WarehouseRequest(
        @NotBlank
        @Size(max = ValidationConstants.WAREHOUSE_CODE_MAX_LENGTH)
        String code,

        @NotBlank
        @Size(max = ValidationConstants.NAME_MAX_LENGTH)
        String name,

        @Size(max = ValidationConstants.ADDRESS_MAX_LENGTH)
        String address,

        Boolean active
) {
}