package com.ecommerce.project.controller;

import com.ecommerce.project.payload.APIResponse;
import com.ecommerce.project.service.EmailService;
import com.ecommerce.project.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Password Controller - Forgot/Reset Password
 *
 * ============================================================
 * 🎓 PASSWORD RESET API:
 * ============================================================
 *
 * POST /api/password/forgot
 *   - User submits email
 *   - Server sends reset link
 *
 * GET /api/password/reset/validate?token=xxx
 *   - Validate token before showing reset form
 *
 * POST /api/password/reset
 *   - Submit new password with token
 *
 * ============================================================
 * 📋 INTERVIEW TIP:
 * "The password reset uses a secure token-based flow.
 * We never reveal if an email exists (security) and
 * tokens are single-use with short expiry."
 * ============================================================
 */
@RestController
@RequestMapping("/api/password")
@Tag(name = "Password Management", description = "Forgot and reset password")
public class PasswordController {

    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired(required = false)
    private EmailService emailService;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    /**
     * Request password reset email
     */
    @PostMapping("/forgot")
    @Operation(summary = "Forgot password", description = "Request password reset email")
    public ResponseEntity<APIResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            String token = passwordResetService.createPasswordResetToken(request.getEmail());

            // Build reset URL
            String resetUrl = frontendUrl + "/reset-password?token=" + token;

            // Send email (if email service is configured)
            if (emailService != null) {
                emailService.sendPasswordResetEmail(request.getEmail(), resetUrl);
            }

            // Always return success to prevent email enumeration
            return ResponseEntity.ok(new APIResponse(
                    "If the email exists, a password reset link has been sent.",
                    true
            ));
        } catch (Exception e) {
            // Don't reveal if email exists or not (security)
            return ResponseEntity.ok(new APIResponse(
                    "If the email exists, a password reset link has been sent.",
                    true
            ));
        }
    }

    /**
     * Validate reset token
     */
    @GetMapping("/reset/validate")
    @Operation(summary = "Validate reset token", description = "Check if reset token is valid")
    public ResponseEntity<APIResponse> validateToken(@RequestParam String token) {
        boolean isValid = passwordResetService.validateToken(token);
        return ResponseEntity.ok(new APIResponse(
                isValid ? "Token is valid" : "Token is invalid",
                isValid
        ));
    }

    /**
     * Reset password with token
     */
    @PostMapping("/reset")
    @Operation(summary = "Reset password", description = "Set new password using reset token")
    public ResponseEntity<APIResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(new APIResponse("Password has been reset successfully.", true));
    }

    // ==================== REQUEST DTOs ====================

    @Data
    public static class ForgotPasswordRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;
    }

    @Data
    public static class ResetPasswordRequest {
        @NotBlank(message = "Token is required")
        private String token;

        @NotBlank(message = "New password is required")
        @Size(min = 6, max = 40, message = "Password must be between 6 and 40 characters")
        private String newPassword;
    }
}
