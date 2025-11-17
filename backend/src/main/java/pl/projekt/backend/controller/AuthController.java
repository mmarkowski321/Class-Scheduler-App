package pl.projekt.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.projekt.backend.dto.AuthResponse;
import pl.projekt.backend.dto.LoginRequest;
import pl.projekt.backend.dto.RegisterRequest;
import pl.projekt.backend.service.AuthService;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {
    
    @Autowired
    private AuthService authService;
    
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authService.register(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }
    
    @GetMapping("/verify")
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {
        try {
            boolean verified = authService.verifyEmail(token);
            if (verified) {
                return ResponseEntity.ok().body(Map.of("message", "Email verified successfully!"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid or expired verification token"));
            }
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            authService.resendVerificationEmail(email);
            return ResponseEntity.ok().body(Map.of("message", "Verification email sent"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            if (email == null || email.isBlank()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Email is required"));
            }
            authService.requestPasswordReset(email);
            // Always return success message (security best practice - don't reveal if email exists)
            return ResponseEntity.ok().body(Map.of("message", "If an account exists with this email, a password reset link has been sent."));
        } catch (RuntimeException e) {
            // Still return success for security (don't reveal if email exists)
            return ResponseEntity.ok().body(Map.of("message", "If an account exists with this email, a password reset link has been sent."));
        }
    }

    @GetMapping("/validate-reset-token")
    public ResponseEntity<?> validateResetToken(@RequestParam String token) {
        try {
            // Check if token exists and is not expired
            var userOpt = authService.validateResetToken(token);
            if (userOpt.isPresent()) {
                return ResponseEntity.ok().body(Map.of("valid", true));
            } else {
                return ResponseEntity.badRequest().body(Map.of("valid", false, "error", "Invalid or expired token"));
            }
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("valid", false, "error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("valid", false, "error", "Invalid or expired token"));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        try {
            String token = request.get("token");
            String newPassword = request.get("newPassword");
            String confirmPassword = request.get("confirmPassword");

            if (token == null || token.isBlank()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Reset token is required"));
            }
            if (newPassword == null || newPassword.isBlank()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("New password is required"));
            }
            if (!newPassword.equals(confirmPassword)) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Passwords do not match"));
            }
            if (newPassword.length() < 8) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Password must be at least 8 characters long"));
            }

            authService.resetPassword(token, newPassword);
            return ResponseEntity.ok().body(Map.of("message", "Password has been reset successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }
    
    // Simple error response class
    public static class ErrorResponse {
        private String message;
        
        public ErrorResponse(String message) {
            this.message = message;
        }
        
        public String getMessage() {
            return message;
        }
    }
}

