package pl.projekt.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setFirstName("Jan");
        registerRequest.setLastName("Kowalski");
        registerRequest.setBirthDate(LocalDate.now().minusYears(14)); // default valid for student
        registerRequest.setRole("STUDENT");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");
    }

    // Accepts a tutor who is exactly 18 years old
    @Test
    void shouldAcceptTutorWhenExactly18() {
        registerRequest.setRole("TUTOR");
        registerRequest.setBirthDate(LocalDate.now().minusYears(18)); // exactly 18

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(userRepository.save(any(Tutor.class))).thenAnswer(invocation -> {
            Tutor tutor = invocation.getArgument(0);
            tutor.setId(100L);
            return tutor;
        });

        AuthResponse response = authService.register(registerRequest);

        assertThat(response.getRole()).isEqualTo("TUTOR");
        assertThat(response.getUserId()).isNotNull();
        verify(emailService).sendVerificationEmail(anyString(), anyString(), anyString());
    }

    // Rejects a tutor who is 17 (under the minimum 18 years)
    @Test
    void shouldRejectTutorWhenUnder18() {
        registerRequest.setRole("TUTOR");
        registerRequest.setBirthDate(LocalDate.now().minusYears(17)); // 17 years old

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("at least 18");

        verify(userRepository, never()).save(any());
    }

    // Accepts a student who is exactly 13 years old
    @Test
    void shouldAcceptStudentWhenExactly13() {
        registerRequest.setRole("STUDENT");
        registerRequest.setBirthDate(LocalDate.now().minusYears(13)); // exactly 13

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(userRepository.save(any(Student.class))).thenAnswer(invocation -> {
            Student s = invocation.getArgument(0);
            s.setId(200L);
            return s;
        });

        AuthResponse response = authService.register(registerRequest);

        assertThat(response.getRole()).isEqualTo("STUDENT");
        assertThat(response.getUserId()).isNotNull();
        verify(emailService).sendVerificationEmail(anyString(), anyString(), anyString());
    }

    // Rejects a student who is 12 (under the minimum 13 years)
    @Test
    void shouldRejectStudentWhenUnder13() {
        registerRequest.setRole("STUDENT");
        registerRequest.setBirthDate(LocalDate.now().minusYears(12)); // 12 years old

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("at least 13");

        verify(userRepository, never()).save(any());
    }
    // Registers a student successfully and sends verification email without issuing a token
    @Test
    void shouldRegisterStudentSuccessfully() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(userRepository.save(any(Student.class))).thenAnswer(invocation -> {
            Student student = invocation.getArgument(0);
            student.setId(1L);
            return student;
        });

        AuthResponse response = authService.register(registerRequest);

        assertThat(response.getUserId()).isNotNull();
        assertThat(response.getRole()).isEqualTo("STUDENT");
        assertThat(response.getToken()).isNull();
        assertThat(response.getMessage()).contains("email");

        verify(userRepository).findByEmail(registerRequest.getEmail());
        verify(passwordEncoder).encode(registerRequest.getPassword());
        verify(userRepository).save(any(Student.class));
        verify(emailService).sendVerificationEmail(anyString(), anyString(), anyString());
    }

    // Registers a tutor successfully and sends verification email
    @Test
    void shouldRegisterTutorSuccessfully() {
        registerRequest.setRole("TUTOR");
        registerRequest.setBirthDate(LocalDate.now().minusYears(25));
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(userRepository.save(any(Tutor.class))).thenAnswer(invocation -> {
            Tutor tutor = invocation.getArgument(0);
            tutor.setId(1L);
            return tutor;
        });

        AuthResponse response = authService.register(registerRequest);

        assertThat(response.getUserId()).isNotNull();
        assertThat(response.getRole()).isEqualTo("TUTOR");
        assertThat(response.getToken()).isNull();

        verify(userRepository).save(any(Tutor.class));
    }

    // Rejects registration when email is already used
    @Test
    void shouldRejectRegistrationWithExistingEmail() {
        Student existingStudent = new Student();
        existingStudent.setEmail("test@example.com");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(existingStudent));

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Email already exists");

        verify(userRepository, never()).save(any());
        verify(emailService, never()).sendVerificationEmail(anyString(), anyString(), anyString());
    }

    // Rejects registration for banned email
    @Test
    void shouldRejectRegistrationWithBannedEmail() {
        Student bannedStudent = new Student();
        bannedStudent.setEmail("test@example.com");
        bannedStudent.setBanned(true);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(bannedStudent));

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("banned");

        verify(userRepository, never()).save(any());
    }

    // Rejects registration for invalid role
    @Test
    void shouldRejectRegistrationWithInvalidRole() {
        registerRequest.setRole("INVALID_ROLE");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid role");
    }

    // Logs in student with valid credentials and returns a JWT
    @Test
    void shouldLoginStudentWithValidCredentials() {
        Student student = new Student();
        student.setId(1L);
        student.setEmail("test@example.com");
        student.setPassword("hashedPassword");
        student.setEmailVerified(true);
        student.setBanned(false);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(student));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtUtil.generateToken(anyLong(), anyString(), anyString())).thenReturn("test-token");

        AuthResponse response = authService.login(loginRequest);

        assertThat(response.getToken()).isEqualTo("test-token");
        assertThat(response.getRole()).isEqualTo("STUDENT");
        assertThat(response.getUserId()).isEqualTo(1L);

        verify(jwtUtil).generateToken(eq(1L), eq("STUDENT"), eq("test@example.com"));
    }

    // Logs in tutor with valid credentials and returns a JWT
    @Test
    void shouldLoginTutorWithValidCredentials() {
        Tutor tutor = new Tutor();
        tutor.setId(2L);
        tutor.setEmail("tutor@example.com");
        tutor.setPassword("hashedPassword");
        tutor.setEmailVerified(true);
        tutor.setBanned(false);

        loginRequest.setEmail("tutor@example.com");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(tutor));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtUtil.generateToken(anyLong(), anyString(), anyString())).thenReturn("tutor-token");

        AuthResponse response = authService.login(loginRequest);

        assertThat(response.getToken()).isEqualTo("tutor-token");
        assertThat(response.getRole()).isEqualTo("TUTOR");
        assertThat(response.getUserId()).isEqualTo(2L);

        verify(jwtUtil).generateToken(eq(2L), eq("TUTOR"), eq("tutor@example.com"));
    }

    // Allows admin login regardless of email verification status
    @Test
    void shouldLoginAdminWithoutEmailVerification() {
        Admin admin = new Admin();
        admin.setId(3L);
        admin.setEmail("admin@example.com");
        admin.setPassword("hashedPassword");
        admin.setBanned(false);

        loginRequest.setEmail("admin@example.com");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtUtil.generateToken(anyLong(), anyString(), anyString())).thenReturn("admin-token");

        AuthResponse response = authService.login(loginRequest);

        assertThat(response.getToken()).isEqualTo("admin-token");
        assertThat(response.getRole()).isEqualTo("ADMIN");
    }

    // Rejects login for non-existing user
    @Test
    void shouldRejectLoginWithInvalidEmail() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid email or password");

        verify(jwtUtil, never()).generateToken(anyLong(), anyString(), anyString());
    }

    // Rejects login with wrong password
    @Test
    void shouldRejectLoginWithInvalidPassword() {
        Student student = new Student();
        student.setEmail("test@example.com");
        student.setPassword("hashedPassword");
        student.setEmailVerified(true);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(student));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid email or password");

        verify(jwtUtil, never()).generateToken(anyLong(), anyString(), anyString());
    }

    // Rejects login for banned accounts
    @Test
    void shouldRejectLoginWhenBanned() {
        Student student = new Student();
        student.setEmail("test@example.com");
        student.setPassword("hashedPassword");
        student.setEmailVerified(true);
        student.setBanned(true);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(student));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("banned");

        verify(jwtUtil, never()).generateToken(anyLong(), anyString(), anyString());
    }

    // Rejects login when email is not verified (non-admins)
    @Test
    void shouldRejectLoginWhenEmailNotVerified() {
        Student student = new Student();
        student.setEmail("test@example.com");
        student.setPassword("hashedPassword");
        student.setEmailVerified(false);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(student));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Email not verified");

        verify(jwtUtil, never()).generateToken(anyLong(), anyString(), anyString());
    }

    // Verifies email using a valid token and sends welcome email
    @Test
    void shouldVerifyEmailWithValidToken() {
        Student student = new Student();
        student.setId(1L);
        student.setEmail("test@example.com");
        student.setFirstName("Jan");
        student.setEmailVerified(false);
        student.setVerificationToken("valid-token");

        when(userRepository.findByVerificationToken(anyString())).thenReturn(Optional.of(student));
        when(userRepository.save(any(User.class))).thenReturn(student);

        boolean verified = authService.verifyEmail("valid-token");

        assertThat(verified).isTrue();
        assertThat(student.getEmailVerified()).isTrue();
        assertThat(student.getVerificationToken()).isNull();

        verify(userRepository).save(student);
        verify(emailService).sendWelcomeEmail(anyString(), anyString(), anyString());
    }

    // Returns false for invalid verification token and does not send emails
    @Test
    void shouldReturnFalseForInvalidToken() {
        when(userRepository.findByVerificationToken(anyString())).thenReturn(Optional.empty());
        when(userRepository.findAll()).thenReturn(java.util.Collections.emptyList());

        boolean verified = authService.verifyEmail("invalid-token");

        assertThat(verified).isFalse();
        verify(emailService, never()).sendWelcomeEmail(anyString(), anyString(), anyString());
    }

    // Resends verification email for unverified account
    @Test
    void shouldResendVerificationEmail() {
        Student student = new Student();
        student.setEmail("test@example.com");
        student.setEmailVerified(false);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(student));
        when(userRepository.save(any(User.class))).thenReturn(student);

        authService.resendVerificationEmail("test@example.com");

        assertThat(student.getVerificationToken()).isNotNull();
        verify(userRepository).save(student);
        verify(emailService).sendVerificationEmail(anyString(), anyString(), anyString());
    }

    // Rejects resend for unknown email
    @Test
    void shouldRejectResendWhenEmailNotFound() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.resendVerificationEmail("notfound@example.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Email not found");

        verify(emailService, never()).sendVerificationEmail(anyString(), anyString(), anyString());
    }

    // Rejects resend for already verified user
    @Test
    void shouldRejectResendWhenEmailAlreadyVerified() {
        Student student = new Student();
        student.setEmail("test@example.com");
        student.setEmailVerified(true);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(student));

        assertThatThrownBy(() -> authService.resendVerificationEmail("test@example.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Email already verified");

        verify(emailService, never()).sendVerificationEmail(anyString(), anyString(), anyString());
    }

    // Accepts a valid (non-expired) reset token
    @Test
    void shouldValidateResetToken() {
        Student student = new Student();
        student.setResetPasswordToken("valid-token");
        student.setResetPasswordTokenExpiry(LocalDateTime.now().plusHours(1).toString());

        when(userRepository.findByResetPasswordToken(anyString())).thenReturn(Optional.of(student));

        Optional<User> result = authService.validateResetToken("valid-token");

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(student);
    }

    // Rejects expired reset tokens
    @Test
    void shouldRejectExpiredResetToken() {
        Student student = new Student();
        student.setResetPasswordToken("expired-token");
        student.setResetPasswordTokenExpiry(LocalDateTime.now().minusHours(1).toString());

        when(userRepository.findByResetPasswordToken(anyString())).thenReturn(Optional.of(student));

        Optional<User> result = authService.validateResetToken("expired-token");

        assertThat(result).isEmpty();
    }

    // Generates and emails a password reset token
    @Test
    void shouldRequestPasswordReset() {
        Student student = new Student();
        student.setEmail("test@example.com");
        student.setFirstName("Jan");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(student));
        when(userRepository.save(any(User.class))).thenReturn(student);

        authService.requestPasswordReset("test@example.com");

        assertThat(student.getResetPasswordToken()).isNotNull();
        assertThat(student.getResetPasswordTokenExpiry()).isNotNull();
        verify(userRepository).save(student);
        verify(emailService).sendPasswordResetEmail(anyString(), anyString(), anyString());
    }

    // Resets password with a valid token, clears token, and emails confirmation
    @Test
    void shouldResetPasswordWithValidToken() {
        Student student = new Student();
        student.setId(1L);
        student.setEmail("test@example.com");
        student.setFirstName("Jan");
        student.setResetPasswordToken("valid-token");
        student.setResetPasswordTokenExpiry(LocalDateTime.now().plusHours(1).toString());

        when(userRepository.findByResetPasswordToken(anyString())).thenReturn(Optional.of(student));
        when(passwordEncoder.encode(anyString())).thenReturn("newHashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(student);

        authService.resetPassword("valid-token", "newPassword123");

        assertThat(student.getPassword()).isEqualTo("newHashedPassword");
        assertThat(student.getResetPasswordToken()).isNull();
        assertThat(student.getResetPasswordTokenExpiry()).isNull();
        verify(userRepository).save(student);
        verify(emailService).sendPasswordChangedEmail(anyString(), anyString(), anyString());
    }

    // Rejects password reset for expired token
    @Test
    void shouldRejectPasswordResetWithExpiredToken() {
        Student student = new Student();
        student.setResetPasswordToken("expired-token");
        student.setResetPasswordTokenExpiry(LocalDateTime.now().minusHours(1).toString());

        when(userRepository.findByResetPasswordToken(anyString())).thenReturn(Optional.of(student));
        when(userRepository.save(any(User.class))).thenReturn(student);

        assertThatThrownBy(() -> authService.resetPassword("expired-token", "newPassword"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("expired");

        verify(passwordEncoder, never()).encode(anyString());
    }

    // Rejects password reset for invalid token
    @Test
    void shouldRejectPasswordResetWithInvalidToken() {
        when(userRepository.findByResetPasswordToken(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.resetPassword("invalid-token", "newPassword"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid or expired reset token");
    }
}

