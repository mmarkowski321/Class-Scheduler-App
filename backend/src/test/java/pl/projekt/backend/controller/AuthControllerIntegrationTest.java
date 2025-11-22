package pl.projekt.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import pl.projekt.backend.dto.RegisterRequest;
import pl.projekt.backend.dto.LoginRequest;
import pl.projekt.backend.model.Student;
import pl.projekt.backend.model.Tutor;
import pl.projekt.backend.model.User;
import pl.projekt.backend.repository.UserRepository;
import pl.projekt.backend.service.EmailService;
import pl.projekt.backend.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private EmailService emailService;

    @MockBean
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        doNothing().when(emailService).sendVerificationEmail(anyString(), anyString(), anyString());
        doNothing().when(emailService).sendWelcomeEmail(anyString(), anyString(), anyString());
        doNothing().when(emailService).sendPasswordResetEmail(anyString(), anyString(), anyString());
        doNothing().when(emailService).sendPasswordChangedEmail(anyString(), anyString(), anyString());
    }

    @AfterEach
    void clean() {
        userRepository.deleteAll();
    }

    @Test
    void shouldRegisterStudentSuccessfully() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("student@example.com");
        request.setPassword("password123");
        request.setFirstName("Jan");
        request.setLastName("Kowalski");
        request.setBirthDate(LocalDate.of(2010, 5, 15));
        request.setRole("STUDENT");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.message").exists());

        User saved = userRepository.findByEmail("student@example.com").orElseThrow();
        assertThat(saved).isInstanceOf(Student.class);
        assertThat(saved.getFirstName()).isEqualTo("Jan");
        assertThat(saved.getLastName()).isEqualTo("Kowalski");
        assertThat(saved.getEmailVerified()).isFalse();
        assertThat(saved.getVerificationToken()).isNotNull();

        verify(emailService).sendVerificationEmail(eq("student@example.com"), anyString(), anyString());
    }

    @Test
    void shouldRegisterTutorSuccessfully() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("tutor@example.com");
        request.setPassword("password123");
        request.setFirstName("Anna");
        request.setLastName("Nowak");
        request.setBirthDate(LocalDate.of(1990, 3, 20));
        request.setRole("TUTOR");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("TUTOR"))
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.message").exists());

        User saved = userRepository.findByEmail("tutor@example.com").orElseThrow();
        assertThat(saved).isInstanceOf(Tutor.class);
        assertThat(saved.getFirstName()).isEqualTo("Anna");
        assertThat(saved.getLastName()).isEqualTo("Nowak");
        assertThat(saved.getEmailVerified()).isFalse();
        assertThat(saved.getVerificationToken()).isNotNull();

        verify(emailService).sendVerificationEmail(eq("tutor@example.com"), anyString(), anyString());
    }

    @Test
    void shouldRejectDuplicateEmail() throws Exception {
        Student existing = new Student();
        existing.setEmail("existing@example.com");
        existing.setPassword("hashed");
        existing.setFirstName("Existing");
        existing.setLastName("User");
        existing.setBirthDate(LocalDate.of(2010, 1, 1));
        existing.setEmailVerified(true);
        userRepository.save(existing);

        RegisterRequest request = new RegisterRequest();
        request.setEmail("existing@example.com");
        request.setPassword("password123");
        request.setFirstName("New");
        request.setLastName("User");
        request.setBirthDate(LocalDate.of(2011, 1, 1));
        request.setRole("STUDENT");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email already exists"));
    }

    @Test
    void shouldLoginStudentWithValidCredentials() throws Exception {
        Student student = new Student();
        student.setEmail("student@example.com");
        student.setPassword(passwordEncoder.encode("password123"));
        student.setFirstName("Jan");
        student.setLastName("Kowalski");
        student.setBirthDate(LocalDate.of(2010, 1, 1));
        student.setEmailVerified(true);
        student = (Student) userRepository.save(student);

        when(jwtUtil.generateToken(eq(student.getId()), eq("STUDENT"), eq("student@example.com")))
                .thenReturn("test-token");

        LoginRequest request = new LoginRequest();
        request.setEmail("student@example.com");
        request.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.userId").value(student.getId()));

        verify(jwtUtil).generateToken(eq(student.getId()), eq("STUDENT"), eq("student@example.com"));
    }

    @Test
    void shouldLoginTutorWithValidCredentials() throws Exception {
        Tutor tutor = new Tutor();
        tutor.setEmail("tutor@example.com");
        tutor.setPassword(passwordEncoder.encode("password123"));
        tutor.setFirstName("Anna");
        tutor.setLastName("Nowak");
        tutor.setBirthDate(LocalDate.of(1990, 1, 1));
        tutor.setEmailVerified(true);
        tutor = (Tutor) userRepository.save(tutor);

        when(jwtUtil.generateToken(eq(tutor.getId()), eq("TUTOR"), eq("tutor@example.com")))
                .thenReturn("test-token");

        LoginRequest request = new LoginRequest();
        request.setEmail("tutor@example.com");
        request.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.role").value("TUTOR"))
                .andExpect(jsonPath("$.userId").value(tutor.getId()));

        verify(jwtUtil).generateToken(eq(tutor.getId()), eq("TUTOR"), eq("tutor@example.com"));
    }

    @Test
    void shouldRejectLoginWithInvalidPassword() throws Exception {
        Student student = new Student();
        student.setEmail("student@example.com");
        student.setPassword(passwordEncoder.encode("correctPassword"));
        student.setFirstName("Jan");
        student.setLastName("Kowalski");
        student.setBirthDate(LocalDate.of(2010, 1, 1));
        student.setEmailVerified(true);
        userRepository.save(student);

        LoginRequest request = new LoginRequest();
        request.setEmail("student@example.com");
        request.setPassword("wrongPassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));

        verify(jwtUtil, never()).generateToken(any(Long.class), anyString(), anyString());
    }

    @Test
    void shouldRejectLoginWithNonExistentEmail() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("nonexistent@example.com");
        request.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));

        verify(jwtUtil, never()).generateToken(any(Long.class), anyString(), anyString());
    }

    @Test
    void shouldVerifyEmailWithValidToken() throws Exception {
        Student student = new Student();
        student.setEmail("student@example.com");
        student.setPassword(passwordEncoder.encode("password123"));
        student.setFirstName("Jan");
        student.setLastName("Kowalski");
        student.setBirthDate(LocalDate.of(2010, 1, 1));
        student.setEmailVerified(false);
        String token = "verification-token-123";
        student.setVerificationToken(token);
        student = (Student) userRepository.save(student);

        mockMvc.perform(get("/api/auth/verify")
                        .param("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email verified successfully!"));

        User verified = userRepository.findById(student.getId()).orElseThrow();
        assertThat(verified.getEmailVerified()).isTrue();
        assertThat(verified.getVerificationToken()).isNull();

        verify(emailService).sendWelcomeEmail(eq("student@example.com"), eq("Jan"), anyString());
    }

    @Test
    void shouldRejectVerifyEmailWithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/auth/verify")
                        .param("token", "invalid-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid or expired verification token"));

        verify(emailService, never()).sendWelcomeEmail(anyString(), anyString(), anyString());
    }

    @Test
    void shouldResendVerificationEmail() throws Exception {
        Student student = new Student();
        student.setEmail("student@example.com");
        student.setPassword(passwordEncoder.encode("password123"));
        student.setFirstName("Jan");
        student.setLastName("Kowalski");
        student.setBirthDate(LocalDate.of(2010, 1, 1));
        student.setEmailVerified(false);
        student.setVerificationToken("old-token");
        student = (Student) userRepository.save(student);

        Map<String, String> request = new HashMap<>();
        request.put("email", "student@example.com");

        mockMvc.perform(post("/api/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Verification email sent"));

        User updated = userRepository.findById(student.getId()).orElseThrow();
        assertThat(updated.getVerificationToken()).isNotNull();
        assertThat(updated.getVerificationToken()).isNotEqualTo("old-token");

        verify(emailService).sendVerificationEmail(eq("student@example.com"), anyString(), anyString());
    }

    @Test
    void shouldRejectResendVerificationForAlreadyVerifiedEmail() throws Exception {
        Student student = new Student();
        student.setEmail("student@example.com");
        student.setPassword(passwordEncoder.encode("password123"));
        student.setFirstName("Jan");
        student.setLastName("Kowalski");
        student.setBirthDate(LocalDate.of(2010, 1, 1));
        student.setEmailVerified(true);
        userRepository.save(student);

        Map<String, String> request = new HashMap<>();
        request.put("email", "student@example.com");

        mockMvc.perform(post("/api/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email already verified"));
    }

    @Test
    void shouldRequestPasswordReset() throws Exception {
        Student student = new Student();
        student.setEmail("student@example.com");
        student.setPassword(passwordEncoder.encode("password123"));
        student.setFirstName("Jan");
        student.setLastName("Kowalski");
        student.setBirthDate(LocalDate.of(2010, 1, 1));
        student.setEmailVerified(true);
        userRepository.save(student);

        Map<String, String> request = new HashMap<>();
        request.put("email", "student@example.com");

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("If an account exists with this email, a password reset link has been sent."));

        User updated = userRepository.findById(student.getId()).orElseThrow();
        assertThat(updated.getResetPasswordToken()).isNotNull();
        assertThat(updated.getResetPasswordTokenExpiry()).isNotNull();

        verify(emailService).sendPasswordResetEmail(eq("student@example.com"), anyString(), anyString());
    }

    @Test
    void shouldRequestPasswordResetEvenForNonExistentEmail() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("email", "nonexistent@example.com");

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("If an account exists with this email, a password reset link has been sent."));
    }

    @Test
    void shouldValidateResetTokenWithValidToken() throws Exception {
        Student student = new Student();
        student.setEmail("student@example.com");
        student.setPassword(passwordEncoder.encode("password123"));
        student.setFirstName("Jan");
        student.setLastName("Kowalski");
        student.setBirthDate(LocalDate.of(2010, 1, 1));
        student.setEmailVerified(true);
        String resetToken = "reset-token-123";
        student.setResetPasswordToken(resetToken);
        student.setResetPasswordTokenExpiry(java.time.LocalDateTime.now().plusHours(1).toString());
        userRepository.save(student);

        mockMvc.perform(get("/api/auth/validate-reset-token")
                        .param("token", resetToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    void shouldRejectValidateResetTokenWithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/auth/validate-reset-token")
                        .param("token", "invalid-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.error").value("Invalid or expired token"));
    }

    @Test
    void shouldResetPasswordWithValidToken() throws Exception {
        Student student = new Student();
        student.setEmail("student@example.com");
        student.setPassword(passwordEncoder.encode("oldPassword123"));
        student.setFirstName("Jan");
        student.setLastName("Kowalski");
        student.setBirthDate(LocalDate.of(2010, 1, 1));
        student.setEmailVerified(true);
        String resetToken = "reset-token-123";
        student.setResetPasswordToken(resetToken);
        student.setResetPasswordTokenExpiry(java.time.LocalDateTime.now().plusHours(1).toString());
        student = (Student) userRepository.save(student);

        Map<String, String> request = new HashMap<>();
        request.put("token", resetToken);
        request.put("newPassword", "newPassword123");
        request.put("confirmPassword", "newPassword123");

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password has been reset successfully"));

        User updated = userRepository.findById(student.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("newPassword123", updated.getPassword())).isTrue();
        assertThat(updated.getResetPasswordToken()).isNull();
        assertThat(updated.getResetPasswordTokenExpiry()).isNull();

        verify(emailService).sendPasswordChangedEmail(eq("student@example.com"), eq("Jan"), anyString());
    }

    @Test
    void shouldRejectResetPasswordWithMismatchedPasswords() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("token", "valid-token");
        request.put("newPassword", "newPassword123");
        request.put("confirmPassword", "differentPassword");

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Passwords do not match"));
    }

    @Test
    void shouldRejectResetPasswordWithShortPassword() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("token", "valid-token");
        request.put("newPassword", "short");
        request.put("confirmPassword", "short");

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Password must be at least 8 characters long"));
    }
}

