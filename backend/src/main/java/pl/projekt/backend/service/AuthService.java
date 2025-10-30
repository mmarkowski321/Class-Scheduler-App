package pl.projekt.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.projekt.backend.dto.AuthResponse;
import pl.projekt.backend.dto.LoginRequest;
import pl.projekt.backend.dto.RegisterRequest;
import pl.projekt.backend.model.Student;
import pl.projekt.backend.model.Tutor;
import pl.projekt.backend.model.User;
import pl.projekt.backend.repository.UserRepository;
import pl.projekt.backend.util.JwtUtil;

import java.time.LocalDateTime;

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
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        
        // Create new user based on role
        User user;
        
        // Hash password before saving
        String hashedPassword = passwordEncoder.encode(request.getPassword());
        
        // Generate verification token
        String verificationToken = java.util.UUID.randomUUID().toString();
        
        if ("STUDENT".equals(request.getRole())) {
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
        
        // Send verification email
        emailService.sendVerificationEmail(savedUser.getEmail(), verificationToken);
        
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
        
        // Block login if email not verified yet
        if (Boolean.FALSE.equals(user.getEmailVerified())) {
            throw new RuntimeException("Email not verified. Please check your inbox for the verification link.");
        }
        
        // Determine role based on class type
        String role = user instanceof Student ? "STUDENT" : "TUTOR";
        
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
            
            // Send welcome email
            emailService.sendWelcomeEmail(user.getEmail(), user.getFirstName());
            
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
        
        // Send verification email
        emailService.sendVerificationEmail(user.getEmail(), verificationToken);
    }
}

