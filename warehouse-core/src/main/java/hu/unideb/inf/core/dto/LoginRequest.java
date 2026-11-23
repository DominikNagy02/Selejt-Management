package hu.unideb.inf.core.dto;

import hu.unideb.inf.shared.schemas.ValidationConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank
        @Size(max = ValidationConstants.USERNAME_MAX_LENGTH)
        String username,

        @NotBlank
        @Size(max = ValidationConstants.PASSWORD_MAX_LENGTH)
        String password
) {
}