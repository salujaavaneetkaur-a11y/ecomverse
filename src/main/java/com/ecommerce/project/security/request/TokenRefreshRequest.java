package com.ecommerce.project.security.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request body for token refresh endpoint
 */
@Data
public class TokenRefreshRequest {

    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}
