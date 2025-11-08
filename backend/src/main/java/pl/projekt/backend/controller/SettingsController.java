package pl.projekt.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import pl.projekt.backend.model.User;
import pl.projekt.backend.repository.UserRepository;
import pl.projekt.backend.service.EmailService;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/settings")
@CrossOrigin(origins = "http://localhost:5173")
public class SettingsController {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private EmailService emailService;
    
    @PutMapping("/email/{userId}")
    public ResponseEntity<?> changeEmail(
            @PathVariable Long userId,
            @RequestBody Map<String, String> request) {
        try {
            String newEmail = request.get("newEmail");
            String currentPassword = request.get("currentPassword");
            
            if (newEmail == null || newEmail.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "New email is required"));
            }
            
            if (currentPassword == null || currentPassword.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Current password is required"));
            }
            
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
            }
            
            User user = userOpt.get();
            
            // Verify current password
            if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid current password"));
            }
            
            // Check if new email is already taken
            Optional<User> existingUserOpt = userRepository.findByEmail(newEmail);
            if (existingUserOpt.isPresent() && !existingUserOpt.get().getId().equals(userId)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email already exists"));
            }
            
            // Store old email for notification
            String oldEmail = user.getEmail();
            String firstName = user.getFirstName();
            String emailLang = user.getEmailLanguage() != null ? user.getEmailLanguage() : "pl";
            
            // Generate new verification token for new email
            String verificationToken = java.util.UUID.randomUUID().toString();
            
            // Update email and reset verification (requires re-verification)
            user.setEmail(newEmail);
            user.setEmailVerified(false);
            user.setVerificationToken(verificationToken);
            
            User saved = userRepository.save(user);
            
            // Send notification to old email
            emailService.sendEmailChangedEmail(oldEmail, firstName, newEmail, emailLang);
            
            // Send verification email to new address
            emailService.sendVerificationEmail(newEmail, verificationToken, emailLang);
            
            return ResponseEntity.ok(Map.of("message", "Email changed successfully. Please verify your new email.", "user", saved));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PutMapping("/password/{userId}")
    public ResponseEntity<?> changePassword(
            @PathVariable Long userId,
            @RequestBody Map<String, String> request) {
        try {
            String currentPassword = request.get("currentPassword");
            String newPassword = request.get("newPassword");
            
            if (currentPassword == null || currentPassword.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Current password is required"));
            }
            
            if (newPassword == null || newPassword.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "New password is required"));
            }
            
            if (newPassword.length() < 8) {
                return ResponseEntity.badRequest().body(Map.of("error", "New password must be at least 8 characters"));
            }
            
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
            }
            
            User user = userOpt.get();
            
            // Verify current password
            if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid current password"));
            }
            
            // Hash and update password
            String hashedPassword = passwordEncoder.encode(newPassword);
            user.setPassword(hashedPassword);
            
            User saved = userRepository.save(user);
            
            // Send notification email about password change (use user's email language preference)
            String emailLang = user.getEmailLanguage() != null ? user.getEmailLanguage() : "pl";
            emailService.sendPasswordChangedEmail(user.getEmail(), user.getFirstName(), emailLang);
            
            return ResponseEntity.ok(Map.of("message", "Password changed successfully", "user", saved));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PutMapping("/phone/{userId}")
    public ResponseEntity<?> updatePhone(
            @PathVariable Long userId,
            @RequestBody Map<String, String> request) {
        try {
            String phone = request.get("phone");
            
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
            }
            
            User user = userOpt.get();
            
            // Check if user is Student (has phone field)
            if (user instanceof pl.projekt.backend.model.Student) {
                pl.projekt.backend.model.Student student = (pl.projekt.backend.model.Student) user;
                student.setPhone(phone);
                User saved = userRepository.save(student);
                return ResponseEntity.ok(Map.of("message", "Phone number updated successfully", "user", saved));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Phone number can only be updated for students"));
            }
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PutMapping("/email-language/{userId}")
    public ResponseEntity<?> updateEmailLanguage(
            @PathVariable Long userId,
            @RequestBody Map<String, String> request) {
        try {
            String emailLanguage = request.get("emailLanguage");
            
            if (emailLanguage == null || (!emailLanguage.equals("pl") && !emailLanguage.equals("en"))) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email language must be 'pl' or 'en'"));
            }
            
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
            }
            
            User user = userOpt.get();
            user.setEmailLanguage(emailLanguage);
            
            User saved = userRepository.save(user);
            return ResponseEntity.ok(Map.of("message", "Email language preference updated successfully", "user", saved));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

