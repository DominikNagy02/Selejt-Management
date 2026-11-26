package hu.unideb.inf.core.dto;

import hu.unideb.inf.core.enums.ZoneType;
import hu.unideb.inf.shared.schemas.ValidationConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ZoneRequest(
        @NotNull
        Long warehouseId,

        @NotBlank
        @Size(max = ValidationConstants.ZONE_CODE_MAX_LENGTH)
        String code,

        @NotBlank
        @Size(max = ValidationConstants.NAME_MAX_LENGTH)
        String name,

        @NotNull
        ZoneType zoneType,

        Boolean active
) {
}