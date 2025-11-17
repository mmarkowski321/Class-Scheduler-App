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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import pl.projekt.backend.model.Student;
import pl.projekt.backend.model.Tutor;
import pl.projekt.backend.model.User;
import pl.projekt.backend.repository.CalendarRepository;
import pl.projekt.backend.repository.LessonRepository;
import pl.projekt.backend.repository.ReviewRepository;
import pl.projekt.backend.repository.UserRepository;
import pl.projekt.backend.service.EmailService;
import pl.projekt.backend.util.JwtUtil;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class SettingsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private CalendarRepository calendarRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private EmailService emailService;

    @MockBean
    private JwtUtil jwtUtil;

    private Student student;
    private Tutor tutor;

    @BeforeEach
    void setUp() {
        reviewRepository.deleteAll();
        lessonRepository.deleteAll();
        calendarRepository.deleteAll();
        userRepository.deleteAll();
        doNothing().when(emailService).sendEmailChangedEmail(anyString(), anyString(), anyString(), anyString());
        doNothing().when(emailService).sendVerificationEmail(anyString(), anyString(), anyString());
        doNothing().when(emailService).sendPasswordChangedEmail(anyString(), anyString(), anyString());

        student = new Student();
        student.setEmail("student@example.com");
        student.setPassword(passwordEncoder.encode("originalPassword"));
        student.setFirstName("Jan");
        student.setLastName("Kowalski");
        student.setBirthDate(LocalDate.of(2010, 1, 1));
        student.setEmailVerified(true);
        student = (Student) userRepository.save(student);

        tutor = new Tutor();
        tutor.setEmail("tutor@example.com");
        tutor.setPassword(passwordEncoder.encode("originalPassword"));
        tutor.setFirstName("Anna");
        tutor.setLastName("Nowak");
        tutor.setBirthDate(LocalDate.of(1990, 1, 1));
        tutor.setEmailVerified(true);
        tutor = (Tutor) userRepository.save(tutor);

        when(jwtUtil.validateToken(anyString())).thenReturn(true);
        when(jwtUtil.getUserIdFromToken(anyString())).thenAnswer(invocation -> {
            String token = invocation.getArgument(0);
            if ("student-token".equals(token)) return student.getId();
            if ("tutor-token".equals(token)) return tutor.getId();
            return null;
        });
        when(jwtUtil.getRoleFromToken(anyString())).thenAnswer(invocation -> {
            String token = invocation.getArgument(0);
            if ("student-token".equals(token)) return "STUDENT";
            if ("tutor-token".equals(token)) return "TUTOR";
            return null;
        });
    }

    @AfterEach
    void clean() {
        reviewRepository.deleteAll();
        lessonRepository.deleteAll();
        calendarRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void studentShouldChangePassword() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("currentPassword", "originalPassword");
        request.put("newPassword", "newPassword123");

        mockMvc.perform(put("/api/settings/password/" + student.getId())
                        .header("Authorization", "Bearer student-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        User updated = userRepository.findById(student.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("newPassword123", updated.getPassword())).isTrue();

        verify(emailService).sendPasswordChangedEmail(eq("student@example.com"), eq("Jan"), anyString());
    }

    @Test
    void tutorShouldChangePassword() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("currentPassword", "originalPassword");
        request.put("newPassword", "newPassword123");

        mockMvc.perform(put("/api/settings/password/" + tutor.getId())
                        .header("Authorization", "Bearer tutor-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        User updated = userRepository.findById(tutor.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("newPassword123", updated.getPassword())).isTrue();

        verify(emailService).sendPasswordChangedEmail(eq("tutor@example.com"), eq("Anna"), anyString());
    }

    @Test
    void shouldRejectPasswordChangeWithWrongCurrentPassword() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("currentPassword", "wrongPassword");
        request.put("newPassword", "newPassword123");

        mockMvc.perform(put("/api/settings/password/" + student.getId())
                        .header("Authorization", "Bearer student-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid current password"));

        User unchanged = userRepository.findById(student.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("originalPassword", unchanged.getPassword())).isTrue();

        verify(emailService, never()).sendPasswordChangedEmail(anyString(), anyString(), anyString());
    }

    @Test
    void shouldRejectPasswordChangeWithShortPassword() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("currentPassword", "originalPassword");
        request.put("newPassword", "short");

        mockMvc.perform(put("/api/settings/password/" + student.getId())
                        .header("Authorization", "Bearer student-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        User unchanged = userRepository.findById(student.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("originalPassword", unchanged.getPassword())).isTrue();
    }

    @Test
    void studentShouldChangeEmail() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("newEmail", "newemail@example.com");
        request.put("currentPassword", "originalPassword");

        mockMvc.perform(put("/api/settings/email/" + student.getId())
                        .header("Authorization", "Bearer student-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        User updated = userRepository.findById(student.getId()).orElseThrow();
        assertThat(updated.getEmail()).isEqualTo("newemail@example.com");
        assertThat(updated.getEmailVerified()).isFalse();
        assertThat(updated.getVerificationToken()).isNotNull();

        verify(emailService).sendEmailChangedEmail(eq("student@example.com"), eq("Jan"), eq("newemail@example.com"), anyString());
        verify(emailService).sendVerificationEmail(eq("newemail@example.com"), anyString(), anyString());
    }

    @Test
    void tutorShouldChangeEmail() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("newEmail", "newemail@example.com");
        request.put("currentPassword", "originalPassword");

        mockMvc.perform(put("/api/settings/email/" + tutor.getId())
                        .header("Authorization", "Bearer tutor-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        User updated = userRepository.findById(tutor.getId()).orElseThrow();
        assertThat(updated.getEmail()).isEqualTo("newemail@example.com");
        assertThat(updated.getEmailVerified()).isFalse();
        assertThat(updated.getVerificationToken()).isNotNull();

        verify(emailService).sendEmailChangedEmail(eq("tutor@example.com"), eq("Anna"), eq("newemail@example.com"), anyString());
        verify(emailService).sendVerificationEmail(eq("newemail@example.com"), anyString(), anyString());
    }

    @Test
    void shouldRejectEmailChangeWithWrongPassword() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("newEmail", "newemail@example.com");
        request.put("currentPassword", "wrongPassword");

        mockMvc.perform(put("/api/settings/email/" + student.getId())
                        .header("Authorization", "Bearer student-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid current password"));

        User unchanged = userRepository.findById(student.getId()).orElseThrow();
        assertThat(unchanged.getEmail()).isEqualTo("student@example.com");
        assertThat(unchanged.getEmailVerified()).isTrue();
    }

    @Test
    void shouldRejectEmailChangeWithDuplicateEmail() throws Exception {
        Student otherStudent = new Student();
        otherStudent.setEmail("existing@example.com");
        otherStudent.setPassword(passwordEncoder.encode("pass"));
        otherStudent.setFirstName("Other");
        otherStudent.setLastName("Student");
        otherStudent.setBirthDate(LocalDate.of(2011, 1, 1));
        otherStudent.setEmailVerified(true);
        userRepository.save(otherStudent);

        Map<String, String> request = new HashMap<>();
        request.put("newEmail", "existing@example.com");
        request.put("currentPassword", "originalPassword");

        mockMvc.perform(put("/api/settings/email/" + student.getId())
                        .header("Authorization", "Bearer student-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Email already exists"));

        User unchanged = userRepository.findById(student.getId()).orElseThrow();
        assertThat(unchanged.getEmail()).isEqualTo("student@example.com");
    }

    @Test
    void studentShouldUpdatePhone() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("phone", "+48123456789");

        mockMvc.perform(put("/api/settings/phone/" + student.getId())
                        .header("Authorization", "Bearer student-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        Student updated = (Student) userRepository.findById(student.getId()).orElseThrow();
        assertThat(updated.getPhone()).isEqualTo("+48123456789");
    }

    @Test
    void shouldRejectPhoneUpdateForTutor() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("phone", "+48123456789");

        mockMvc.perform(put("/api/settings/phone/" + tutor.getId())
                        .header("Authorization", "Bearer tutor-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Phone number can only be updated for students"));
    }
}

