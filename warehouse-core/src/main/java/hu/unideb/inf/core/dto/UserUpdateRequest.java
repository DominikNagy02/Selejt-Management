package hu.unideb.inf.core.dto;

import hu.unideb.inf.core.enums.Role;
import hu.unideb.inf.shared.schemas.ValidationConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @NotBlank
        @Size(max = ValidationConstants.FULL_NAME_MAX_LENGTH)
        String fullName,

        @Size(min = ValidationConstants.PASSWORD_MIN_LENGTH, max = ValidationConstants.PASSWORD_MAX_LENGTH)
        String password,

        @NotNull
        Role role,

        @NotNull
        Boolean active
) {
}