package pl.projekt.backend.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import pl.projekt.backend.model.Lesson;
import pl.projekt.backend.model.LessonDeliveryMode;
import pl.projekt.backend.model.LessonStatus;
import pl.projekt.backend.model.Review;
import pl.projekt.backend.model.Student;
import pl.projekt.backend.model.Tutor;
import pl.projekt.backend.model.User;
import pl.projekt.backend.repository.LessonRepository;
import pl.projekt.backend.repository.ReviewRepository;
import pl.projekt.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Tutor tutor;
    private Student student;
    private Authentication adminAuth;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        lessonRepository.deleteAll();
        reviewRepository.deleteAll();

        // Create tutor
        tutor = new Tutor();
        tutor.setEmail("tutor@example.com");
        tutor.setPassword(passwordEncoder.encode("password123"));
        tutor.setFirstName("Anna");
        tutor.setLastName("Nowak");
        tutor.setBirthDate(LocalDate.of(1990, 1, 1));
        tutor.setEmailVerified(true);
        tutor = (Tutor) userRepository.save(tutor);

        // Create student
        student = new Student();
        student.setEmail("student@example.com");
        student.setPassword(passwordEncoder.encode("password123"));
        student.setFirstName("Jan");
        student.setLastName("Kowalski");
        student.setBirthDate(LocalDate.of(2010, 1, 1));
        student.setEmailVerified(true);
        student = (Student) userRepository.save(student);

        // Mock admin authentication
        adminAuth = mock(Authentication.class);
        Collection<? extends GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"));
        doReturn(authorities).when(adminAuth).getAuthorities();
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(adminAuth);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void clean() {
        SecurityContextHolder.clearContext();
        reviewRepository.deleteAll();
        lessonRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldGetAllUsers() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tutors").isArray())
                .andExpect(jsonPath("$.students").isArray())
                .andExpect(jsonPath("$.tutors[0].email").value("tutor@example.com"))
                .andExpect(jsonPath("$.students[0].email").value("student@example.com"));
    }

    @Test
    void shouldGetAllTutors() throws Exception {
        mockMvc.perform(get("/api/admin/users/tutors")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].email").value("tutor@example.com"));
    }

    @Test
    void shouldGetAllStudents() throws Exception {
        mockMvc.perform(get("/api/admin/users/students")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].email").value("student@example.com"));
    }

    @Test
    void shouldGetUserById() throws Exception {
        mockMvc.perform(get("/api/admin/users/{id}", tutor.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("tutor@example.com"))
                .andExpect(jsonPath("$.id").value(tutor.getId()));
    }

    @Test
    void shouldReturnNotFoundForInvalidUserId() throws Exception {
        mockMvc.perform(get("/api/admin/users/99999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldGetUserLessons() throws Exception {
        // Create lesson
        Lesson lesson = new Lesson();
        lesson.setTutor(tutor);
        lesson.setStudent(student);
        lesson.setStartTime(LocalDateTime.now().plusHours(1));
        lesson.setEndTime(LocalDateTime.now().plusHours(2));
        lesson.setStatus(LessonStatus.SCHEDULED);
        lesson.setDeliveryMode(LessonDeliveryMode.ONLINE);
        lessonRepository.save(lesson);

        mockMvc.perform(get("/api/admin/users/{id}/lessons", tutor.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].tutor.id").value(tutor.getId()))
                .andExpect(jsonPath("$[0].student.id").value(student.getId()));
    }

    @Test
    void shouldGetUserStats() throws Exception {
        // Create lesson
        Lesson lesson = new Lesson();
        lesson.setTutor(tutor);
        lesson.setStudent(student);
        lesson.setStartTime(LocalDateTime.now().plusHours(1));
        lesson.setEndTime(LocalDateTime.now().plusHours(2));
        lesson.setStatus(LessonStatus.SCHEDULED);
        lesson.setDeliveryMode(LessonDeliveryMode.ONLINE);
        lesson = lessonRepository.save(lesson);

        // Create review
        Review review = new Review();
        review.setLesson(lesson);
        review.setTutor(tutor);
        review.setStudent(student);
        review.setTutorRating(5);
        review.setComment("Great tutor!");
        reviewRepository.save(review);

        mockMvc.perform(get("/api/admin/users/{id}/stats", tutor.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lessonsCount").value(1))
                .andExpect(jsonPath("$.reviewsCount").value(1));
    }

    @Test
    void shouldBanUser() throws Exception {
        mockMvc.perform(delete("/api/admin/users/{id}", tutor.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User banned successfully"));

        User bannedUser = userRepository.findById(tutor.getId()).orElseThrow();
        assertThat(bannedUser.getBanned()).isTrue();
    }

    @Test
    void shouldUnbanUser() throws Exception {
        // First ban user
        tutor.ban();
        userRepository.save(tutor);

        mockMvc.perform(post("/api/admin/users/{id}/unban", tutor.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User unbanned successfully"));

        User unbannedUser = userRepository.findById(tutor.getId()).orElseThrow();
        assertThat(unbannedUser.getBanned()).isFalse();
    }

    @Test
    void shouldGetAllReviews() throws Exception {
        // Create lesson first
        Lesson lesson = new Lesson();
        lesson.setTutor(tutor);
        lesson.setStudent(student);
        lesson.setStartTime(LocalDateTime.now().plusHours(1));
        lesson.setEndTime(LocalDateTime.now().plusHours(2));
        lesson.setStatus(LessonStatus.SCHEDULED);
        lesson.setDeliveryMode(LessonDeliveryMode.ONLINE);
        lesson = lessonRepository.save(lesson);

        // Create review
        Review review = new Review();
        review.setLesson(lesson);
        review.setTutor(tutor);
        review.setStudent(student);
        review.setTutorRating(5);
        review.setComment("Great tutor!");
        reviewRepository.save(review);

        mockMvc.perform(get("/api/admin/reviews")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].tutorRating").value(5))
                .andExpect(jsonPath("$[0].comment").value("Great tutor!"));
    }

    @Test
    void shouldGetReviewsByTutorId() throws Exception {
        // Create lesson first
        Lesson lesson = new Lesson();
        lesson.setTutor(tutor);
        lesson.setStudent(student);
        lesson.setStartTime(LocalDateTime.now().plusHours(1));
        lesson.setEndTime(LocalDateTime.now().plusHours(2));
        lesson.setStatus(LessonStatus.SCHEDULED);
        lesson.setDeliveryMode(LessonDeliveryMode.ONLINE);
        lesson = lessonRepository.save(lesson);

        // Create review
        Review review = new Review();
        review.setLesson(lesson);
        review.setTutor(tutor);
        review.setStudent(student);
        review.setTutorRating(5);
        review.setComment("Great tutor!");
        reviewRepository.save(review);

        mockMvc.perform(get("/api/admin/reviews/user/{userId}", tutor.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].tutor.id").value(tutor.getId()));
    }

    @Test
    void shouldGetAllLessons() throws Exception {
        // Create lesson
        Lesson lesson = new Lesson();
        lesson.setTutor(tutor);
        lesson.setStudent(student);
        lesson.setStartTime(LocalDateTime.now().plusHours(1));
        lesson.setEndTime(LocalDateTime.now().plusHours(2));
        lesson.setStatus(LessonStatus.SCHEDULED);
        lesson.setDeliveryMode(LessonDeliveryMode.ONLINE);
        lessonRepository.save(lesson);

        mockMvc.perform(get("/api/admin/lessons")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].status").value("SCHEDULED"));
    }

    @Test
    void shouldGetAdminStats() throws Exception {
        mockMvc.perform(get("/api/admin/stats")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").exists())
                .andExpect(jsonPath("$.tutorsCount").exists())
                .andExpect(jsonPath("$.studentsCount").exists())
                .andExpect(jsonPath("$.totalLessons").exists())
                .andExpect(jsonPath("$.totalReviews").exists());
    }

    @Test
    void shouldDenyAccessWhenNotAdmin() throws Exception {
        // Mock non-admin authentication
        Authentication nonAdminAuth = mock(Authentication.class);
        when(nonAdminAuth.getAuthorities()).thenReturn(Collections.emptyList());
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(nonAdminAuth);
        SecurityContextHolder.setContext(securityContext);

        mockMvc.perform(get("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Admin access required"));
    }
}

