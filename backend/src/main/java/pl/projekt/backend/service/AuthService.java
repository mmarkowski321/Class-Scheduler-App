package pl.projekt.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.projekt.backend.dto.AuthResponse;
import pl.projekt.backend.dto.LoginRequest;
import pl.projekt.backend.dto.RegisterRequest;
import pl.projekt.backend.model.Admin;
import pl.projekt.backend.model.Student;
import pl.projekt.backend.model.Tutor;
import pl.projekt.backend.model.User;
import pl.projekt.backend.repository.UserRepository;
import pl.projekt.backend.util.JwtUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

@Service
public class AuthService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private EmailService emailService;
    
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check if email already exists
        var existingUserOpt = userRepository.findByEmail(request.getEmail());
        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();
            // Check if user is banned
            if (Boolean.TRUE.equals(existingUser.getBanned())) {
                throw new RuntimeException("This email is banned and cannot be used to create an account");
            }
            throw new RuntimeException("Email already exists");
        }
        
        // Create new user based on role
        User user;
        
        // Hash password before saving
        String hashedPassword = passwordEncoder.encode(request.getPassword());
        
        // Generate verification token
        String verificationToken = java.util.UUID.randomUUID().toString();
        
        if ("STUDENT".equals(request.getRole())) {
            // Age check: students must be at least 13
            LocalDate birth = request.getBirthDate();
            if (birth == null) {
                throw new RuntimeException("Birth date is required");
            }
            int age = Period.between(birth, LocalDate.now()).getYears();
            if (age < 13) {
                throw new RuntimeException("You must be at least 13 to register as a student.");
            }
            Student student = new Student();
            student.setEmail(request.getEmail());
            student.setPassword(hashedPassword); // Store hashed password
            student.setFirstName(request.getFirstName());
            student.setLastName(request.getLastName());
            student.setBirthDate(request.getBirthDate());
            student.setCreatedAt(LocalDateTime.now().toString());
            student.setEmailVerified(false);
            student.setVerificationToken(verificationToken);
            user = student;
        } else if ("TUTOR".equals(request.getRole())) {
            // Age check: tutors must be at least 18
            LocalDate birth = request.getBirthDate();
            if (birth == null) {
                throw new RuntimeException("Birth date is required");
            }
            int age = Period.between(birth, LocalDate.now()).getYears();
            if (age < 18) {
                throw new RuntimeException("You must be at least 18 to register as a tutor.");
            }
            Tutor tutor = new Tutor();
            tutor.setEmail(request.getEmail());
            tutor.setPassword(hashedPassword); // Store hashed password
            tutor.setFirstName(request.getFirstName());
            tutor.setLastName(request.getLastName());
            tutor.setBirthDate(request.getBirthDate());
            tutor.setCreatedAt(LocalDateTime.now().toString());
            tutor.setEmailVerified(false);
            tutor.setVerificationToken(verificationToken);
            user = tutor;
        } else {
            throw new RuntimeException("Invalid role");
        }
        
        User savedUser = userRepository.save(user);
        
        // Send verification email (use user's email language preference, default to "pl")
        String emailLang = savedUser.getEmailLanguage() != null ? savedUser.getEmailLanguage() : "pl";
        emailService.sendVerificationEmail(savedUser.getEmail(), verificationToken, emailLang);
        
        // Do NOT log in on registration – require email verification first
        return new AuthResponse(null, request.getRole(), savedUser.getId(), "Registration successful. Please check your email to verify your account.");
    }
    
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new RuntimeException("Invalid email or password"));
        
        // Verify password using BCrypt
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }
        
        // Block login if user is banned
        if (Boolean.TRUE.equals(user.getBanned())) {
            throw new RuntimeException("This account has been banned");
        }
        
        // Block login if email not verified yet (admins are always verified)
        if (!(user instanceof Admin) && Boolean.FALSE.equals(user.getEmailVerified())) {
            throw new RuntimeException("Email not verified. Please check your inbox for the verification link.");
        }
        
        // Determine role based on class type
        String role;
        if (user instanceof Admin) {
            role = "ADMIN";
        } else if (user instanceof Student) {
            role = "STUDENT";
        } else {
            role = "TUTOR";
        }
        
        // Generate JWT token
        String token = jwtUtil.generateToken(user.getId(), role, user.getEmail());
        
        return new AuthResponse(token, role, user.getId(), "Login successful");
    }
    
    @Transactional
    public boolean verifyEmail(String verificationToken) {
        User user = userRepository.findByVerificationToken(verificationToken)
            .orElse(null);
        
        if (user != null && user.getVerificationToken() != null) {
            // Token found and not used yet - verify the email
            user.verifyEmail(); // Set emailVerified = true, verificationToken = null
            userRepository.save(user);
            
            // Send welcome email (use user's email language preference)
            String emailLang = user.getEmailLanguage() != null ? user.getEmailLanguage() : "pl";
            emailService.sendWelcomeEmail(user.getEmail(), user.getFirstName(), emailLang);
            
            return true;
        }
        
        // Check if email is already verified (user might be clicking the link twice)
        User alreadyVerified = userRepository.findAll().stream()
            .filter(u -> {
                if (u.getVerificationToken() == null && u.getEmailVerified()) {
                    return true;
                }
                return false;
            })
            .findFirst()
            .orElse(null);
        
        if (alreadyVerified != null && alreadyVerified.getEmailVerified()) {
            // Already verified - return true to avoid showing error
            return true;
        }
        
        return false;
    }
    
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Email not found"));
        
        if (user.getEmailVerified()) {
            throw new RuntimeException("Email already verified");
        }
        
        // Generate new verification token
        String verificationToken = java.util.UUID.randomUUID().toString();
        user.setVerificationToken(verificationToken);
        userRepository.save(user);
        
        // Send verification email (use user's email language preference)
        String emailLang = user.getEmailLanguage() != null ? user.getEmailLanguage() : "pl";
        emailService.sendVerificationEmail(user.getEmail(), verificationToken, emailLang);
    }

    public java.util.Optional<User> validateResetToken(String resetToken) {
        java.util.Optional<User> userOpt = userRepository.findByResetPasswordToken(resetToken);
        
        if (userOpt.isEmpty()) {
            return java.util.Optional.empty();
        }

        User user = userOpt.get();
        
        // Check expiry
        if (user.getResetPasswordTokenExpiry() != null) {
            LocalDateTime expiry = LocalDateTime.parse(user.getResetPasswordTokenExpiry());
            if (LocalDateTime.now().isAfter(expiry)) {
                return java.util.Optional.empty();
            }
        }

        return userOpt;
    }

    @Transactional
    public void requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Email not found"));

        // Generate reset token (UUID)
        String resetToken = java.util.UUID.randomUUID().toString();
        user.setResetPasswordToken(resetToken);
        
        // Set expiry (1 hour from now)
        LocalDateTime expiry = LocalDateTime.now().plusHours(1);
        user.setResetPasswordTokenExpiry(expiry.toString());
        
        userRepository.save(user);
        
        // Send reset email (use user's email language preference)
        String emailLang = user.getEmailLanguage() != null ? user.getEmailLanguage() : "pl";
        emailService.sendPasswordResetEmail(user.getEmail(), resetToken, emailLang);
    }

    @Transactional
    public void resetPassword(String resetToken, String newPassword) {
        User user = userRepository.findByResetPasswordToken(resetToken)
            .orElse(null);

        if (user == null) {
            throw new RuntimeException("Invalid or expired reset token");
        }

        // Check expiry
        if (user.getResetPasswordTokenExpiry() != null) {
            LocalDateTime expiry = LocalDateTime.parse(user.getResetPasswordTokenExpiry());
            if (LocalDateTime.now().isAfter(expiry)) {
                user.clearResetToken();
                userRepository.save(user);
                throw new RuntimeException("Reset token has expired");
            }
        }

        // Update password
        String hashedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(hashedPassword);
        user.clearResetToken();
        userRepository.save(user);

        // Send confirmation email (use user's email language preference)
        String emailLang = user.getEmailLanguage() != null ? user.getEmailLanguage() : "pl";
        emailService.sendPasswordChangedEmail(user.getEmail(), user.getFirstName(), emailLang);
    }
}

