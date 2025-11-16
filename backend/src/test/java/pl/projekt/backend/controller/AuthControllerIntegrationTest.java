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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
}

