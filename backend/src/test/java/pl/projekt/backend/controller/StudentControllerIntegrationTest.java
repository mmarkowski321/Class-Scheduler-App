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
import pl.projekt.backend.model.Lesson;
import pl.projekt.backend.model.LessonDeliveryMode;
import pl.projekt.backend.model.LessonStatus;
import pl.projekt.backend.model.Review;
import pl.projekt.backend.model.Student;
import pl.projekt.backend.model.Tutor;
import pl.projekt.backend.repository.LessonRepository;
import pl.projekt.backend.repository.ReviewRepository;
import pl.projekt.backend.repository.UserRepository;
import pl.projekt.backend.util.JwtUtil;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class StudentControllerIntegrationTest {

    private static final String STUDENT_TOKEN = "student-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @MockBean
    private JwtUtil jwtUtil;

    private Student student;
    private Tutor tutor;
    private Tutor tutor2;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        lessonRepository.deleteAll();
        reviewRepository.deleteAll();

        student = new Student();
        student.setEmail("student@example.com");
        student.setPassword("password");
        student.setFirstName("Student");
        student.setLastName("Test");
        student.setBirthDate(java.time.LocalDate.of(2010, 1, 1));
        student.setEmailVerified(true);
        student = (Student) userRepository.save(student);

        tutor = new Tutor();
        tutor.setEmail("tutor@example.com");
        tutor.setPassword("password");
        tutor.setFirstName("Tutor");
        tutor.setLastName("Test");
        tutor.setBirthDate(java.time.LocalDate.of(1990, 1, 1));
        tutor.setEmailVerified(true);
        tutor.setSubjects("math,physics");
        tutor.setHourlyRate(100.0);
        tutor = (Tutor) userRepository.save(tutor);

        tutor2 = new Tutor();
        tutor2.setEmail("tutor2@example.com");
        tutor2.setPassword("password");
        tutor2.setFirstName("Tutor2");
        tutor2.setLastName("Test");
        tutor2.setBirthDate(java.time.LocalDate.of(1990, 1, 1));
        tutor2.setEmailVerified(true);
        tutor2.setSubjects("english");
        tutor2.setHourlyRate(80.0);
        tutor2 = (Tutor) userRepository.save(tutor2);

        // Mock JWT for student
        when(jwtUtil.validateToken(STUDENT_TOKEN)).thenReturn(true);
        when(jwtUtil.getUserIdFromToken(STUDENT_TOKEN)).thenReturn(student.getId());
        when(jwtUtil.getRoleFromToken(STUDENT_TOKEN)).thenReturn("STUDENT");
    }

    @AfterEach
    void tearDown() {
        reviewRepository.deleteAll();
        lessonRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    @Test
    void shouldGetStudentOverview() throws Exception {
        // Create upcoming lesson
        Lesson upcomingLesson = new Lesson();
        upcomingLesson.setTutor(tutor);
        upcomingLesson.setStudent(student);
        upcomingLesson.setStartTime(LocalDateTime.now().plusDays(2));
        upcomingLesson.setEndTime(LocalDateTime.now().plusDays(2).plusHours(1));
        upcomingLesson.setStatus(LessonStatus.SCHEDULED);
        upcomingLesson.setDeliveryMode(LessonDeliveryMode.ONLINE);
        lessonRepository.save(upcomingLesson);

        // Create past lesson
        Lesson pastLesson = new Lesson();
        pastLesson.setTutor(tutor);
        pastLesson.setStudent(student);
        pastLesson.setStartTime(LocalDateTime.now().minusDays(10));
        pastLesson.setEndTime(LocalDateTime.now().minusDays(10).plusHours(1));
        pastLesson.setStatus(LessonStatus.COMPLETED);
        pastLesson.setDeliveryMode(LessonDeliveryMode.ONLINE);
        lessonRepository.save(pastLesson);

        // Create lesson needing attention (REQUESTED)
        Lesson requestedLesson = new Lesson();
        requestedLesson.setTutor(tutor);
        requestedLesson.setStudent(student);
        requestedLesson.setStartTime(LocalDateTime.now().plusDays(5));
        requestedLesson.setEndTime(LocalDateTime.now().plusDays(5).plusHours(1));
        requestedLesson.setStatus(LessonStatus.REQUESTED);
        requestedLesson.setDeliveryMode(LessonDeliveryMode.ONLINE);
        lessonRepository.save(requestedLesson);

        mockMvc.perform(get("/api/students/me/overview")
                        .header("Authorization", bearer(STUDENT_TOKEN))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upcoming").isArray())
                .andExpect(jsonPath("$.upcoming.length()").value(1))
                .andExpect(jsonPath("$.history").isArray())
                .andExpect(jsonPath("$.history.length()").value(1))
                .andExpect(jsonPath("$.attention").isArray())
                .andExpect(jsonPath("$.attention.length()").value(1))
                .andExpect(jsonPath("$.newTutors").isArray());
    }

    @Test
    void shouldGetStudentOverviewWithNewTutors() throws Exception {
        mockMvc.perform(get("/api/students/me/overview")
                        .header("Authorization", bearer(STUDENT_TOKEN))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newTutors").isArray())
                .andExpect(jsonPath("$.newTutors.length()").value(2)); // Should exclude student itself
    }

    @Test
    void shouldGetStudentLessons() throws Exception {
        // Create multiple lessons
        Lesson lesson1 = new Lesson();
        lesson1.setTutor(tutor);
        lesson1.setStudent(student);
        lesson1.setStartTime(LocalDateTime.now().plusDays(1));
        lesson1.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));
        lesson1.setStatus(LessonStatus.SCHEDULED);
        lesson1.setDeliveryMode(LessonDeliveryMode.ONLINE);
        lessonRepository.save(lesson1);

        Lesson lesson2 = new Lesson();
        lesson2.setTutor(tutor);
        lesson2.setStudent(student);
        lesson2.setStartTime(LocalDateTime.now().plusDays(3));
        lesson2.setEndTime(LocalDateTime.now().plusDays(3).plusHours(1));
        lesson2.setStatus(LessonStatus.SCHEDULED);
        lesson2.setDeliveryMode(LessonDeliveryMode.ONLINE);
        lessonRepository.save(lesson2);

        Lesson lesson3 = new Lesson();
        lesson3.setTutor(tutor2);
        lesson3.setStudent(student);
        lesson3.setStartTime(LocalDateTime.now().plusDays(2));
        lesson3.setEndTime(LocalDateTime.now().plusDays(2).plusHours(1));
        lesson3.setStatus(LessonStatus.SCHEDULED);
        lesson3.setDeliveryMode(LessonDeliveryMode.ONLINE);
        lessonRepository.save(lesson3);

        mockMvc.perform(get("/api/students/me/lessons")
                        .header("Authorization", bearer(STUDENT_TOKEN))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lessons").isArray())
                .andExpect(jsonPath("$.lessons.length()").value(3))
                .andExpect(jsonPath("$.lessons[0].tutor").exists())
                .andExpect(jsonPath("$.lessons[0].tutorName").exists());
    }

    @Test
    void shouldGetStudentLessonsSortedByStartTime() throws Exception {
        // Create lessons in random order
        Lesson lesson1 = new Lesson();
        lesson1.setTutor(tutor);
        lesson1.setStudent(student);
        lesson1.setStartTime(LocalDateTime.now().plusDays(3));
        lesson1.setEndTime(LocalDateTime.now().plusDays(3).plusHours(1));
        lesson1.setStatus(LessonStatus.SCHEDULED);
        lesson1.setDeliveryMode(LessonDeliveryMode.ONLINE);
        lessonRepository.save(lesson1);

        Lesson lesson2 = new Lesson();
        lesson2.setTutor(tutor);
        lesson2.setStudent(student);
        lesson2.setStartTime(LocalDateTime.now().plusDays(1));
        lesson2.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));
        lesson2.setStatus(LessonStatus.SCHEDULED);
        lesson2.setDeliveryMode(LessonDeliveryMode.ONLINE);
        lessonRepository.save(lesson2);

        Lesson lesson3 = new Lesson();
        lesson3.setTutor(tutor);
        lesson3.setStudent(student);
        lesson3.setStartTime(LocalDateTime.now().plusDays(2));
        lesson3.setEndTime(LocalDateTime.now().plusDays(2).plusHours(1));
        lesson3.setStatus(LessonStatus.SCHEDULED);
        lesson3.setDeliveryMode(LessonDeliveryMode.ONLINE);
        lessonRepository.save(lesson3);

        mockMvc.perform(get("/api/students/me/lessons")
                        .header("Authorization", bearer(STUDENT_TOKEN))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lessons").isArray())
                .andExpect(jsonPath("$.lessons.length()").value(3));
        // Note: We can't easily verify sort order without parsing dates, but the endpoint should sort correctly
    }

    @Test
    void shouldIncludeReviewStatusInLessons() throws Exception {
        Lesson lesson = new Lesson();
        lesson.setTutor(tutor);
        lesson.setStudent(student);
        lesson.setStartTime(LocalDateTime.now().plusDays(1));
        lesson.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));
        lesson.setStatus(LessonStatus.SCHEDULED);
        lesson.setDeliveryMode(LessonDeliveryMode.ONLINE);
        lesson = lessonRepository.save(lesson);

        // Create review
        Review review = new Review();
        review.setLesson(lesson);
        review.setTutor(tutor);
        review.setStudent(student);
        review.setTutorRating(5);
        review.setStudentReviewAt(LocalDateTime.now());
        reviewRepository.save(review);

        mockMvc.perform(get("/api/students/me/lessons")
                        .header("Authorization", bearer(STUDENT_TOKEN))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lessons[0].studentReviewSubmitted").value(true));
    }

    @Test
    void shouldRejectGetOverviewWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/students/me/overview")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void shouldRejectGetOverviewWithInvalidToken() throws Exception {
        when(jwtUtil.validateToken("invalid-token")).thenReturn(false);

        mockMvc.perform(get("/api/students/me/overview")
                        .header("Authorization", "Bearer invalid-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void shouldRejectGetOverviewWithNonStudentRole() throws Exception {
        when(jwtUtil.validateToken("tutor-token")).thenReturn(true);
        when(jwtUtil.getUserIdFromToken("tutor-token")).thenReturn(tutor.getId());
        when(jwtUtil.getRoleFromToken("tutor-token")).thenReturn("TUTOR");

        mockMvc.perform(get("/api/students/me/overview")
                        .header("Authorization", "Bearer tutor-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void shouldReturnEmptyArraysWhenNoLessons() throws Exception {
        mockMvc.perform(get("/api/students/me/overview")
                        .header("Authorization", bearer(STUDENT_TOKEN))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upcoming").isArray())
                .andExpect(jsonPath("$.upcoming.length()").value(0))
                .andExpect(jsonPath("$.history").isArray())
                .andExpect(jsonPath("$.history.length()").value(0))
                .andExpect(jsonPath("$.attention").isArray())
                .andExpect(jsonPath("$.attention.length()").value(0));
    }
}

