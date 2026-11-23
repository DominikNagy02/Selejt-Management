package hu.unideb.inf.core.dto;

import hu.unideb.inf.core.enums.StorageType;
import hu.unideb.inf.shared.schemas.ValidationConstants;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ShelfRequest(
        @NotNull
        Long zoneId,

        @NotBlank
        @Size(max = ValidationConstants.SHELF_CODE_MAX_LENGTH)
        String code,

        @NotNull
        StorageType storageType,

        @Min(ValidationConstants.MIN_TIERS)
        int maxTiers,

        @NotNull
        @DecimalMin(ValidationConstants.MIN_POSITIVE_WEIGHT)
        @DecimalMax(ValidationConstants.MAX_WEIGHT_KG)
        @Digits(integer = ValidationConstants.WEIGHT_PRECISION - ValidationConstants.WEIGHT_SCALE,
                fraction = ValidationConstants.WEIGHT_SCALE)
        BigDecimal maxWeightKg,

        @NotNull
        @DecimalMin(ValidationConstants.MIN_POSITIVE_VOLUME)
        @DecimalMax(ValidationConstants.MAX_VOLUME_M3)
        @Digits(integer = ValidationConstants.VOLUME_PRECISION - ValidationConstants.VOLUME_SCALE,
                fraction = ValidationConstants.VOLUME_SCALE)
        BigDecimal maxVolumeM3,

        Boolean active
) {
}