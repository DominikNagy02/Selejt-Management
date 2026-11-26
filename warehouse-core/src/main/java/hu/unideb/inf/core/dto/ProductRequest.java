package hu.unideb.inf.core.dto;

import hu.unideb.inf.core.enums.ZoneType;
import hu.unideb.inf.shared.schemas.ValidationConstants;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank
        @Size(max = ValidationConstants.SKU_MAX_LENGTH)
        String sku,

        @NotBlank
        @Size(max = ValidationConstants.BARCODE_MAX_LENGTH)
        String barcode,

        @NotBlank
        @Size(max = ValidationConstants.PRODUCT_NAME_MAX_LENGTH)
        String name,

        @Size(max = ValidationConstants.DESCRIPTION_MAX_LENGTH)
        String description,

        @NotNull
        @DecimalMin(ValidationConstants.MIN_POSITIVE_WEIGHT)
        @DecimalMax(ValidationConstants.MAX_WEIGHT_KG)
        @Digits(integer = ValidationConstants.WEIGHT_PRECISION - ValidationConstants.WEIGHT_SCALE,
                fraction = ValidationConstants.WEIGHT_SCALE)
        BigDecimal unitWeightKg,

        @NotNull
        @DecimalMin(ValidationConstants.MIN_POSITIVE_VOLUME)
        @DecimalMax(ValidationConstants.MAX_VOLUME_M3)
        @Digits(integer = ValidationConstants.VOLUME_PRECISION - ValidationConstants.VOLUME_SCALE,
                fraction = ValidationConstants.VOLUME_SCALE)
        BigDecimal unitVolumeM3,

        boolean requiresExpiry,

        ZoneType requiredZoneType,

        @Min(0)
        int minStockLevel,

        Boolean active
) {
}
